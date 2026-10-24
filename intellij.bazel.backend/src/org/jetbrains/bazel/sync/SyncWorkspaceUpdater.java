package org.jetbrains.bazel.sync;

import consulo.bazel.backend.localize.BazelBackendLocalize;
import consulo.build.ui.event.MessageEvent;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.RepoMappingDisabled;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.RepoType;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.SyncFileClassifier.SyncFile;
import org.jetbrains.bazel.sync.SyncFileClassifier.SyncFileKind;
import org.jetbrains.bazel.sync.task.SyncWorkspaceProvider.SyncWorkspaceContext;
import org.jetbrains.bazel.sync.task.SyncWorkspaceProvider.SyncWorkspaceStatus;
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace;
import org.jetbrains.bazel.sync.workspace.mapper.BazelWorkspaceResolver;
import org.jetbrains.bazel.sync.workspace.snapshot.IncompleteWorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotBuilder;
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams.WorkspaceBuildTargetSelector;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SyncWorkspaceUpdater {
    private final Project myProject;

    public SyncWorkspaceUpdater(Project project) {
        myProject = project;
    }

    public CompletableFuture<SyncWorkspaceUpdate> update(
        ProjectSyncScope requested,
        WorkspaceSnapshot previous,
        SyncWorkspaceContext context
    ) {
        return effectiveScope(context, previous, requested).thenCompose(scope -> {
            return switch (scope) {
                case ProjectSyncScope.Full ignored -> {
                    RepoMapping knownRepoMapping = context.knownRepoMapping();
                    CompletableFuture<RepoMapping> repoMappingFuture = knownRepoMapping != null
                        ? CompletableFuture.completedFuture(knownRepoMapping)
                        : context.server().workspaceRepoMapping(context.taskId());
                    yield repoMappingFuture
                        .thenCompose(repoMapping -> resolveWorkspace(
                            context,
                            repoMapping,
                            WorkspaceBuildTargetSelector.AllTargets.INSTANCE
                        ))
                        .thenCompose(resolved -> {
                            // resolve which produced nothing cannot overwrite what is already there
                            if (resolved.hasError() && resolved.targets().isEmpty()) {
                                return CompletableFuture.completedFuture(
                                    new SyncWorkspaceUpdate(scope, SyncWorkspaceStatus.FATAL, previous)
                                );
                            }
                            return WorkspaceSnapshotBuilder.build(
                                myProject,
                                context.server().getProjectView(),
                                resolved.repoMapping(),
                                context.server().getBazelInfo(),
                                resolved
                            ).thenApply(snapshot -> new SyncWorkspaceUpdate(
                                scope,
                                resolved.hasError() ? SyncWorkspaceStatus.PARTIAL : SyncWorkspaceStatus.SUCCESS,
                                snapshot
                            ));
                        });
                }

                case ProjectSyncScope.Targets targetsScope -> {
                    if (targetsScope.patterns().isEmpty()) {
                        yield CompletableFuture.completedFuture(new SyncWorkspaceUpdate(scope, SyncWorkspaceStatus.SUCCESS, previous));
                    }
                    // TODO: right now `resolveWorkspace` is dumb, bazel build will be called for small subset of targets
                    //  but BEP still return full transitive closure of all output artifacts. That means re still have to read
                    //  large number of .textproto files despite most of them begin unchanged.
                    //  Solution here is to track output artifact identity (modification time, content length for local artifacts)
                    //  and compare those against previous state. As a result we get "artifact diff" and we invoke textproto read + parse
                    //  only on changed files.
                    util.getSyncConsole(myProject).addDiagnosticMessage(
                        context.taskId(),
                        null,
                        -1,
                        -1,
                        BazelBackendLocalize.progressTextResyncingTargets(targetsScope.patterns().size()),
                        targetsScope.patterns().stream()
                            .map(it -> BazelRepoMappingUtils.toShortString(it, myProject))
                            .collect(Collectors.joining("\n")),
                        MessageEvent.Kind.INFO
                    );
                    yield partialSyncRepoMapping(context, previous)
                        .thenCompose(repoMapping -> resolveWorkspace(
                            context,
                            repoMapping,
                            new WorkspaceBuildTargetSelector.SpecificTargets(targetsScope.patterns())
                        ))
                        .thenCompose(resolved -> {
                            IncompleteWorkspaceSnapshot incomplete = WorkspaceSnapshotBuilder.buildIncomplete(resolved);
                            return util.withSubtask(
                                util.getSyncConsole(myProject),
                                context.taskId().subTask("workspace_snapshot_merge"),
                                BazelBackendLocalize.consoleTaskSyncSnapshotUpdating(),
                                ignored -> WorkspaceSnapshotBuilder.merge(
                                    myProject,
                                    context.server().getProjectView(),
                                    context.server().getBazelInfo(),
                                    List.of(IncompleteWorkspaceSnapshot.toIncompleteSnapshot(previous), incomplete)
                                )
                            ).thenApply(snapshot -> new SyncWorkspaceUpdate(
                                scope,
                                resolved.hasError() ? SyncWorkspaceStatus.PARTIAL : SyncWorkspaceStatus.SUCCESS,
                                snapshot
                            ));
                        });
                }

                case ProjectSyncScope.Files ignored ->
                    throw new IllegalStateException("Files sync scope should already have been promoted by effectiveScope(...)");
            };
        });
    }

    private CompletableFuture<RepoMapping> partialSyncRepoMapping(SyncWorkspaceContext context, WorkspaceSnapshot previous) {
        if (previous.repoMapping() instanceof RepoMappingDisabled && context.server().getBazelInfo().isBzlModEnabled()) {
            return context.server().workspaceRepoMapping(context.taskId());
        }
        else {
            return CompletableFuture.completedFuture(previous.repoMapping());
        }
    }

    private CompletableFuture<BazelResolvedWorkspace> resolveWorkspace(
        SyncWorkspaceContext context,
        RepoMapping repoMapping,
        WorkspaceBuildTargetSelector selector
    ) {
        return switch (context.phase()) {
            case FIRST -> BazelWorkspaceResolver.fetchPhasedWorkspace(
                myProject,
                repoMapping,
                context.taskId()
            );

            case SECOND -> BazelWorkspaceResolver.fetchAspectWorkspace(
                myProject,
                repoMapping,
                context.allKnownTargets(),
                context.buildProject(),
                context.taskId(),
                selector
            );
        };
    }

    private boolean isFullResyncRequired(SyncFile receiver) {
        return receiver.kind() instanceof SyncFileKind.Module ||
            Set.of(SyncFileKind.Workspace.INSTANCE, SyncFileKind.ProjectView.INSTANCE, SyncFileKind.BazelDotFile.INSTANCE)
                .contains(receiver.kind()) ||
            (receiver.kind() instanceof SyncFileKind.Starlark starlark && starlark.label() == null);
    }

    private CompletableFuture<ProjectSyncScope> effectiveScope(
        SyncWorkspaceContext context,
        WorkspaceSnapshot previous,
        ProjectSyncScope requested
    ) {
        if (requested instanceof ProjectSyncScope.Files filesScope) {
            // here we're using stale repo mapping from previous sync, however it's ok
            // modifying repos cause full resync anyway
            // the issue appears before first sync, where repo mapping is always empty
            // correct approach here would be to promote to full sync always when snapshot is empty
            SyncFileClassifier classifier = new SyncFileClassifier(previous.repoMapping(), context.server().getBazelInfo());
            List<SyncFile> syncFiles = filesScope.files().stream().map(classifier::classify).toList();
            if (syncFiles.stream().anyMatch(this::isFullResyncRequired)) {
                return CompletableFuture.completedFuture(new ProjectSyncScope.Full(filesScope.build(), false));
            }

            return findAffectedPatterns(context, syncFiles, previous.repoMapping()).thenApply(patternsToResync -> {
                if (patternsToResync.isEmpty()) {
                    util.getSyncConsole(myProject).addDiagnosticMessage(
                        context.taskId(),
                        null,
                        -1,
                        -1,
                        BazelBackendLocalize.progressTextNoTargetsFoundForRequestedFilesToSync(),
                        null,
                        MessageEvent.Kind.WARNING
                    );
                }

                // promote to normal partial sync
                return new ProjectSyncScope.Targets(List.copyOf(patternsToResync), filesScope.build());
            });
        }
        else {
            return CompletableFuture.completedFuture(requested);
        }
    }

    private CompletableFuture<Set<Label>> findAffectedPatterns(
        SyncWorkspaceContext context,
        List<SyncFile> syncFiles,
        RepoMapping repoMapping
    ) {
        Set<Label> patternsToResync = new LinkedHashSet<>();
        for (SyncFile syncFile : syncFiles) {
            SyncFileKind kind = syncFile.kind();
            if (kind instanceof SyncFileKind.Build build) {
                patternsToResync.add(build.pattern());
            }
            else if (kind instanceof SyncFileKind.Folder folder) {
                patternsToResync.add(folder.pattern());
            }
        }

        return findAffectedPatternsBySourceFiles(syncFiles, context)
            .thenCompose(bySourceFiles -> {
                patternsToResync.addAll(bySourceFiles);
                return findAffectedPatternsByStarlarkFiles(context, syncFiles, repoMapping);
            })
            .thenApply(byStarlarkFiles -> {
                patternsToResync.addAll(byStarlarkFiles);
                return patternsToResync;
            });
    }

    private CompletableFuture<List<Label>> findAffectedPatternsByStarlarkFiles(
        SyncWorkspaceContext context,
        List<SyncFile> syncFiles,
        RepoMapping repoMapping
    ) {
        Path workspaceRoot = context.server().getBazelInfo().workspaceRoot();
        List<SyncFile> starlarkFiles =
            syncFiles.stream().filter(it -> it.kind() instanceof SyncFileKind.Starlark && it.path().startsWith(workspaceRoot)).toList();

        // for changed starlark files, try to find affected BUILD files
        if (!starlarkFiles.isEmpty()) {
            return util.withSubtask(
                util.getSyncConsole(myProject),
                context.taskId().subTask("query_buildfiles"),
                BazelBackendLocalize.progressTitleQueryingBuildFiles(),
                taskId -> {
                    Set<Path> workspaceRelativePaths = new LinkedHashSet<>();
                    Set<RepoType> universeRepos = new LinkedHashSet<>();
                    for (SyncFile starlarkFile : starlarkFiles) {
                        Path relativePath = relativeToOrNull(starlarkFile.path(), workspaceRoot);
                        if (relativePath != null) {
                            workspaceRelativePaths.add(relativePath);
                        }
                    }
                    for (SyncFile starlarkFile : starlarkFiles) {
                        if (((SyncFileKind.Starlark) starlarkFile.kind()).label() instanceof ResolvedLabel resolvedLabel) {
                            universeRepos.add(resolvedLabel.repo());
                        }
                    }
                    return BuildfilesQuery.findDependantBuildFiles(
                        context.server(),
                        workspaceRelativePaths,
                        universeRepos,
                        repoMapping,
                        taskId
                    );
                }
            );
        }
        else {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private CompletableFuture<List<Label>> findAffectedPatternsBySourceFiles(List<SyncFile> syncFiles, SyncWorkspaceContext context) {
        Set<Label> fileLabels = new LinkedHashSet<>();
        for (SyncFile syncFile : syncFiles) {
            if (syncFile.kind() instanceof SyncFileKind.Source source) {
                fileLabels.add(source.label());
            }
        }

        // for changed source files, invoke file to target query
        if (!fileLabels.isEmpty()) {
            return util.withSubtask(
                util.getSyncConsole(myProject),
                context.taskId().subTask("query_source_to_target"),
                BazelBackendLocalize.progressTitleQueryingTargetsForSources(),
                taskId -> FileToTargetQuery.findDependantTargetsFromFiles(context.server(), fileLabels, taskId)
                    .thenApply(sources -> {
                        for (Map.Entry<Label, List<Label>> entry : sources.entrySet()) {
                            if (entry.getValue().isEmpty()) {
                                util.getSyncConsole(myProject).addDiagnosticMessage(
                                    taskId,
                                    null,
                                    -1,
                                    -1,
                                    BazelBackendLocalize.progressTextFileQueryFoundNoTargetsFor(entry.getKey()),
                                    null,
                                    MessageEvent.Kind.WARNING
                                );
                            }
                        }
                        return sources;
                    })
            ).thenApply(sources -> {
                List<Label> flatten = new ArrayList<>();
                for (List<Label> targets : sources.values()) {
                    flatten.addAll(targets);
                }
                return flatten;
            });
        }
        else {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    // consulo: Kotlin Path.relativeToOrNull
    private static @Nullable Path relativeToOrNull(Path receiver, Path base) {
        try {
            return base.normalize().relativize(receiver.normalize());
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record SyncWorkspaceUpdate(
        ProjectSyncScope scope,
        SyncWorkspaceStatus status,
        WorkspaceSnapshot snapshot
    ) {
    }
}
