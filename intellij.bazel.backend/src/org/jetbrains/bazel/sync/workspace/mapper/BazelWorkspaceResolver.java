package org.jetbrains.bazel.sync.workspace.mapper;

import consulo.bazel.backend.localize.BazelBackendLocalize;
import consulo.build.ui.event.MessageEvent;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.ignore.BazelIgnoreService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.server.BazelServerConnection.BazelServerService;
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams;
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams.WorkspaceBuildTargetSelector;
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class BazelWorkspaceResolver {
    private BazelWorkspaceResolver() {
    }

    public static CompletableFuture<BazelResolvedWorkspace> fetchPhasedWorkspace(
        Project project,
        RepoMapping repoMapping,
        TaskId taskId
    ) {
        return BazelServerService.getInstance(project).getConnection().runWithServer(taskId, server ->
            server.workspaceBuildPhasedTargets(new WorkspaceBuildTargetPhasedParams(taskId)).thenApply(phasedSyncProject -> {
                PhasedBazelProjectMapper phasedMapper = new PhasedBazelProjectMapper(
                    server.getBazelPathsResolver(),
                    server.getProjectView()
                );
                List<BuildTarget> targets = phasedMapper.mapTargets(phasedSyncProject.modules());
                Set<WorkspaceTargetKey> rootTargets = new LinkedHashSet<>();
                for (BuildTarget target : targets) {
                    rootTargets.add(target.getKey());
                }
                return new BazelResolvedWorkspace(
                    null,
                    repoMapping,
                    rootTargets,
                    targets,
                    phasedSyncProject.hasError(),
                    Map.of()
                );
            })
        );
    }

    public static CompletableFuture<BazelResolvedWorkspace> fetchAspectWorkspace(
        Project project,
        RepoMapping repoMapping,
        @Nullable List<Label> allKnownTargets,
        boolean build,
        TaskId taskId,
        WorkspaceBuildTargetSelector selector
    ) {
        return BazelServerService.getInstance(project).getConnection().runWithServer(taskId, server -> {
            reportIgnoredBazelBsp(project, taskId, server.getBazelInfo().workspaceRoot());

            return server.workspaceBuildTargets(new WorkspaceBuildTargetParams(selector, build, allKnownTargets, repoMapping, taskId))
                .thenCompose(syncProject -> util.withSubtask(
                        util.getSyncConsole(project),
                        taskId.subTask("process_target_info"),
                        BazelBackendLocalize.progressTitleProcessWorkspaceTargets(),
                        ignored -> new AspectBazelProjectMapper(project, server)
                            .mapTargets(syncProject.targetProtoPaths(), build, taskId)
                    ).thenCompose(targets -> {
                        reportImportedNoIdeTargets(project, taskId, targets, ProjectViewExtensions.getDropNoIde(server.getProjectView()));

                        // consulo: upstream top-level extendRepoMapping of server/bzlmod/RepoMapping.kt
                        return org.jetbrains.bazel.server.bzlmod.RepoMapping.extendRepoMapping(
                            server,
                            repoMapping,
                            targets.stream().map(BuildTarget::getId).toList(),
                            taskId
                        ).thenApply(extendedRepoMapping -> new BazelResolvedWorkspace(
                            targets.isEmpty() ? "_main" : targets.get(0).getWorkspaceName(),
                            extendedRepoMapping,
                            syncProject.rootTargets(),
                            targets,
                            syncProject.hasError(),
                            syncProject.configurations()
                        ));
                    })
                );
        });
    }

    private static void reportImportedNoIdeTargets(
        Project project,
        TaskId taskId,
        Collection<BuildTarget> targets,
        boolean dropNoIde
    ) {
        List<BuildTarget> noIdeTargets = targets.stream().filter(target -> target.getTags().contains(Constants.NO_IDE)).toList();
        if (!noIdeTargets.isEmpty()) {
            // consulo: Kotlin joinToString(",", limit = 5)
            String description = noIdeTargets.stream()
                .limit(5)
                .map(it -> BuildTarget.getId(it).toString())
                .collect(Collectors.joining(",")) + (noIdeTargets.size() > 5 ? ",..." : "");
            if (dropNoIde) {
                util.getSyncConsole(project).addDiagnosticMessage(
                    taskId, null, -1, -1,
                    BazelBackendLocalize.bazelImportNoideDroppedtargets(noIdeTargets.size(), Constants.NO_IDE),
                    description,
                    MessageEvent.Kind.INFO
                );
            }
            else {
                util.getSyncConsole(project).addDiagnosticMessage(
                    taskId, null, -1, -1,
                    BazelBackendLocalize.bazelImportNoideTargets(noIdeTargets.size(), Constants.NO_IDE),
                    description,
                    MessageEvent.Kind.WARNING
                );
            }
        }
    }

    private static void reportIgnoredBazelBsp(Project project, TaskId taskId, Path workspaceRoot) {
        Path dotBazelBsp = workspaceRoot.resolve(Constants.DOT_BAZELBSP_DIR_NAME);
        if (BazelIgnoreService.getInstance(project).isIgnored(dotBazelBsp)) {
            util.getSyncConsole(project).addDiagnosticMessage(
                taskId, null, -1, -1,
                BazelBackendLocalize.bazelImportIgnoredBazelbsp(dotBazelBsp, Constants.DOT_BAZELBSP_DIR_NAME),
                null,
                MessageEvent.Kind.ERROR
            );
        }
    }
}
