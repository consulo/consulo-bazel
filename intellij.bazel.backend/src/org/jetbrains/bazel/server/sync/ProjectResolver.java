package org.jetbrains.bazel.server.sync;

import consulo.bazel.backend.localize.BazelBackendLocalize;
import consulo.bazel.aspect.BazelAspectModule;
import com.intellij.aspect.lib.Definition;
import com.intellij.aspect.lib.OutputGroups;
import consulo.application.progress.ProgressManager;
import consulo.build.ui.event.MessageEvent;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.bazelrunner.ModuleResolver;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.commons.TargetCollection;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.ignore.BazelIgnoreService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.languages.projectview.ProjectViewTargets;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager.BazelBspAspectsManagerResult;
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager.RulesetInstance;
import org.jetbrains.bazel.server.bsp.managers.BazelExternalRulesetsQuery.BazelExternalRulesetsQueryImpl;
import org.jetbrains.bazel.server.model.BazelSyncProject.AspectSyncProject;
import org.jetbrains.bazel.server.sync.sharding.BazelBuildTargetSharder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler.BazelTaskLogger;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Responsible for querying bazel and constructing Project instance  */
public class ProjectResolver {
    public static class IllegalTargetsSizeException extends Exception {
        public IllegalTargetsSizeException(String message) {
            super(message);
        }
    }

    private static List<TargetCollection> halve(TargetCollection receiver) throws IllegalTargetsSizeException {
        List<Label> values = receiver.values();
        if (values.size() <= 1) {
            throw new IllegalTargetsSizeException("Cannot split target collection with " + values.size() + " targets");
        }
        int mid = values.size() / 2;
        return List.of(
            new TargetCollection(values.subList(0, mid), receiver.excludedValues()),
            new TargetCollection(values.subList(mid, values.size()), receiver.excludedValues())
        );
    }

    private final BazelBspAspectsManager myBazelBspAspectsManager;
    private final ProjectView myProjectView;
    private final BazelInfo myBazelInfo;
    private final BazelRunner myBazelRunner;
    private final BazelPathsResolver myBazelPathsResolver;
    private final BazelTaskEventsHandler myTaskEventsHandler;
    private final Project myProject;

    public ProjectResolver(
        BazelBspAspectsManager bazelBspAspectsManager,
        ProjectView projectView,
        BazelInfo bazelInfo,
        BazelRunner bazelRunner,
        BazelPathsResolver bazelPathsResolver,
        BazelTaskEventsHandler taskEventsHandler,
        Project project
    ) {
        myBazelBspAspectsManager = bazelBspAspectsManager;
        myProjectView = projectView;
        myBazelInfo = bazelInfo;
        myBazelRunner = bazelRunner;
        myBazelPathsResolver = bazelPathsResolver;
        myTaskEventsHandler = taskEventsHandler;
        myProject = project;
    }

    private <T extends @Nullable Object> CompletableFuture<T> measured(String description, Supplier<CompletableFuture<T>> f) {
        // consulo: telemetry is not ported, upstream ran f() in a bspTracer span named by description
        return f.get();
    }

    public CompletableFuture<AspectSyncProject> resolve(
        boolean build,
        @Nullable List<Label> requestedTargetsToSync,
        @Nullable List<Label> allTargets, /* all known targets, if any, from first phase */
        RepoMapping repoMapping,
        TaskId taskId
    ) {
        // consulo: telemetry is not ported, upstream ran the body in the "Resolve project" bspTracer span
        return buildProjectWithAspectAndSetup(build, requestedTargetsToSync, allTargets, repoMapping, taskId).thenCompose(aspectResult ->
            configuration.fetchConfigurationsFromAnalysisCache(myProjectView, myBazelRunner, taskId)
                .thenApply(fetched -> {
                    if (fetched.configurations() != null) {
                        return (Collection<WorkspaceConfiguration>) fetched.configurations();
                    }
                    LOG.warn("`bazel config` invocation failed, falling back to BEP configurations", fetched.failure());
                    return aspectResult.bepOutput().getConfigurations().values(); // fallback to BEP configurations
                })
                .thenCompose(configurations ->
                    extractAspectOutputPaths(aspectResult).thenApply(aspectOutputs -> {
                        Set<WorkspaceTargetKey> rootTargets = aspectResult.bepOutput().rootTargets();

                        Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurationsById = new LinkedHashMap<>();
                        for (WorkspaceConfiguration it : configurations) {
                            configurationsById.put(it.id(), it);
                        }
                        return new AspectSyncProject(
                            myBazelInfo.workspaceRoot(),
                            myBazelInfo.release(),
                            aspectResult.isFailure(),
                            aspectOutputs,
                            rootTargets,
                            configurationsById
                        );
                    })
                )
        );
    }

    private CompletableFuture<BazelBspAspectsManagerResult> buildProjectWithAspectAndSetup(
        boolean build,
        @Nullable List<Label> requestedTargetsToSync,
        @Nullable List<Label> allTargets, /* all known targets, if any, from first phase */
        RepoMapping repoMapping,
        TaskId taskId
    ) {
        BazelExternalRulesetsQueryImpl bazelExternalRulesetsQuery =
            new BazelExternalRulesetsQueryImpl(
                taskId,
                myBazelRunner,
                myBazelInfo.isBzlModEnabled(),
                myBazelInfo.isWorkspaceEnabled(),
                myTaskEventsHandler,
                myProjectView,
                repoMapping
            );

        return measured(
            "Discovering supported external rules",
            bazelExternalRulesetsQuery::fetchExternalRulesetNames
        ).thenCompose(externalRulesetNames -> {
            List<String> externalRepos = new ArrayList<>();
            if (repoMapping instanceof BzlmodRepoMapping bzlmodRepoMapping) {
                Map<String, String> mapping = bzlmodRepoMapping.getApparentRepoNameToCanonicalName();
                for (String repoName : externalRulesetNames) {
                    String canonicalName = mapping.get(repoName);
                    if (canonicalName != null && !canonicalName.equals("")) {
                        externalRepos.add("@@" + canonicalName);
                    }
                }
            }

            return measured("Looking up definitions of external rules", () ->
                new ModuleResolver(myBazelRunner, myProjectView, taskId).resolveModules(externalRepos, myBazelInfo)
            ).thenCompose(repoDefinitionsWithWarnings -> {
                repoDefinitionsWithWarnings.warnings().forEach(it ->
                    util.getSyncConsole(myProject).addDiagnosticMessage(
                        taskId, null, 0, 0, LocalizeValue.of(it), null,
                        MessageEvent.Kind.WARNING
                    )
                );

                Map<String, ModuleResolver.@Nullable ShowRepoResult> repoDefinitions = repoDefinitionsWithWarnings.result();

                return measured("Mapping rule names to languages", () ->
                    CompletableFuture.completedFuture(
                        myBazelBspAspectsManager.calculateRulesets(
                            externalRulesetNames,
                            repoDefinitions,
                            myBazelInfo.externalAutoloads()
                        )
                    )
                ).thenCompose(ruleSets -> {
                    for (RulesetInstance it : ruleSets) {
                        String deprecated = it.ruleset().deprecated();
                        if (deprecated != null) {
                            util.getSyncConsole(myProject).addDiagnosticMessage(
                                taskId, null, 0, 0, LocalizeValue.of(deprecated), null,
                                MessageEvent.Kind.WARNING
                            );
                        }
                    }

                    return measured("Realizing language aspect files from templates", () -> {
                        myBazelBspAspectsManager.deployIntelliJAspect(
                            ruleSets,
                            myBazelInfo.release(),
                            repoMapping,
                            ProjectViewExtensions.getAspectsAttrs(myProjectView),
                            ProjectViewExtensions.getDropNoIde(myProjectView)
                        );
                        // consulo: tell which aspect module the workspace's bazel_dep provides
                        BazelAspectModule.Resolved externalAspect = myBazelBspAspectsManager.getExternalAspect();
                        if (externalAspect != null) {
                            util.getSyncConsole(myProject).addDiagnosticMessage(
                                taskId, null, 0, 0,
                                externalAspect.module() == BazelAspectModule.INTELLIJ
                                    ? BazelBackendLocalize.syncAspectExternalIntellijModule(externalAspect.module().getModuleName())
                                    : BazelBackendLocalize.syncAspectExternalModule(externalAspect.module().getModuleName()),
                                null,
                                MessageEvent.Kind.INFO
                            );
                        }
                        return CompletableFuture.<@Nullable Void>completedFuture(null);
                    });
                }).thenCompose(ignored -> {
                    CompletableFuture<TargetCollection> targetsToSync =
                        requestedTargetsToSync != null
                            ? CompletableFuture.completedFuture(new TargetCollection(requestedTargetsToSync, List.of()))
                            : projectViewTargetsToSync();

                    return targetsToSync.thenCompose(it ->
                        measured("Building project with aspect", () ->
                            buildProjectWithAspect(
                                myProjectView,
                                build,
                                it,
                                allTargets,
                                taskId
                            )
                        )
                    );
                });
            });
        });
    }

    private CompletableFuture<TargetCollection> projectViewTargetsToSync() {
        return measured("Dropping excluded patterns without packages", () ->
            TargetCollection.dropExcludedPatternsWithoutPackages(
                TargetCollection.fromExcludableList(ProjectViewTargets.getTargets(myProjectView)),
                myBazelInfo.workspaceRoot(),
                BazelIgnoreService.getInstance(myProject)
            )
        );
    }

    private CompletableFuture<BazelBspAspectsManagerResult> buildProjectWithAspect(
        ProjectView projectView,
        boolean build,
        TargetCollection targetsToSync,
        @Nullable List<Label> allTargets, /* all known targets, if any, from first phase */
        TaskId taskId
    ) {
        // consulo: upstream coroutineScope
        List<String> aspects = List.of(Definition.INTELLIJ_ASPECT.getLocation().toString());
        BazelTaskLogger taskLogger = BazelTaskEventsHandler.asLogger(myTaskEventsHandler, taskId);
        List<String> outputGroups = new ArrayList<>();
        outputGroups.add(OutputGroups.INFO.getGroupName());
        if (BazelFeatureFlags.isSyncRequestSourcesEnabled()) {
            outputGroups.add(OutputGroups.SYNC.getGroupName());
        }
        if (build) {
            outputGroups.add(OutputGroups.BUILD.getGroupName());
        }
        Supplier<CompletableFuture<BazelBspAspectsManagerResult>> nonShardBuild =
            () ->
                myBazelBspAspectsManager
                    .fetchFilesFromOutputGroups(
                        targetsToSync,
                        aspects,
                        outputGroups,
                        projectView,
                        taskId
                    ).thenApply(it -> {
                        if (it.status() == BazelStatus.OOM_ERROR) {
                            taskLogger.warn(
                                "Bazel ran out of memory during sync. To mitigate, consider enabling shard sync in your project view file: `shard_sync: true`"
                            );
                            taskLogger.message("---");
                        }
                        return it;
                    });

        CompletableFuture<BazelBspAspectsManagerResult> res;
        if (ProjectViewExtensions.getShardSync(projectView)) {
            res = BazelBuildTargetSharder.expandAndShardTargets(
                myBazelPathsResolver,
                targetsToSync,
                projectView,
                myBazelRunner,
                taskLogger,
                taskId,
                allTargets
            ).thenCompose(shardedResult -> {
                // consulo: the vars of upstream are mutated by the asynchronous iterations of the loop below
                AtomicReference<List<TargetCollection>> remainingShardedTargetsSpecs =
                    new AtomicReference<>(new ArrayList<>(shardedResult.targets()));
                AtomicInteger shardNumber = new AtomicInteger(1);
                BazelBspAspectsManagerResult.Accumulator shardedBuildResult = new BazelBspAspectsManagerResult.Accumulator();
                AtomicInteger suggestedTargetShardSize = new AtomicInteger(ProjectViewExtensions.getTargetShardSize(projectView));
                return whileAsync(() -> !remainingShardedTargetsSpecs.get().isEmpty(), () -> {
                    // consulo: upstream ensureActive()
                    ProgressManager.checkCanceled();
                    CompletableFuture<?> shutDown;
                    if (BazelFeatureFlags.getShutDownBeforeShardBuild()) {
                        // Prevent memory leak by forcing Bazel to shut down before it builds a shard
                        // This may cause the build to become slower, but it is necessary, at least before this issue is solved
                        // https://github.com/bazelbuild/bazel/issues/19412
                        shutDown = runBazelShutDown(projectView, taskId);
                    }
                    else {
                        shutDown = CompletableFuture.completedFuture(null);
                    }
                    return shutDown.thenCompose(ignored -> {
                        TargetCollection shardedTargetsSpec = remainingShardedTargetsSpecs.get().remove(0);
                        String shardName =
                            "shard " + shardNumber.get() + " of " + (shardNumber.get() + remainingShardedTargetsSpecs.get().size());
                        taskLogger.message("\nBuilding " + shardName + " ...");
                        taskLogger.message("Expected remaining shards: " + remainingShardedTargetsSpecs.get().size());
                        return myBazelBspAspectsManager
                            .fetchFilesFromOutputGroups(
                                shardedTargetsSpec,
                                aspects,
                                outputGroups,
                                projectView,
                                taskId
                            ).<@Nullable Void>thenApply(result -> {
                                if (result.isFailure()) {
                                    taskLogger.warn("Failed to build " + shardName);
                                }
                                else {
                                    taskLogger.message("Finished building " + shardName);
                                }
                                if (result.status() == BazelStatus.OOM_ERROR) {
                                    taskLogger.warn("Bazel ran out of memory during sync, attempting to halve the target shard size to recover");
                                    try {
                                        List<TargetCollection> halvedTargetsSpec = halve(shardedTargetsSpec);
                                        suggestedTargetShardSize.set(halvedTargetsSpec.get(0).values().size());
                                        taskLogger.message(
                                            "Retrying with the target shard size of " + suggestedTargetShardSize.get()
                                                + " (previously " + shardedTargetsSpec.values().size() + ") ..."
                                        );
                                        List<TargetCollection> halvedRemaining = new ArrayList<>();
                                        for (TargetCollection it : remainingShardedTargetsSpecs.get()) {
                                            halvedRemaining.addAll(halve(it));
                                        }
                                        remainingShardedTargetsSpecs.set(halvedRemaining);
                                        remainingShardedTargetsSpecs.get().addAll(0, halvedTargetsSpec);
                                    }
                                    catch (IllegalTargetsSizeException e) {
                                        taskLogger.error("Cannot split targets further: " + e.getMessage());
                                        throw new CompletionException(e);
                                    }
                                }
                                // As bazel starts the naming of the namedSetOfFiles freshly (starting from "0") on each run, we have to distinguish
                                // them somehow; we do this by encoding the shard number in their names.
                                BazelBspAspectsManagerResult runResult = result.renameNamedSets(shardNumber.get());
                                shardedBuildResult.add(runResult);

                                taskLogger.message("---");
                                shardNumber.incrementAndGet();
                                return null;
                            });
                    });
                }).thenApply(ignored -> {
                    if (suggestedTargetShardSize.get() != ProjectViewExtensions.getTargetShardSize(projectView)) {
                        taskLogger.message(
                            "Bazel ran out of memory during sync. To mitigate, consider setting shard size in your project view file: `target_shard_size: "
                                + suggestedTargetShardSize.get() + "`"
                        );
                        taskLogger.message("---");
                    }
                    return shardedBuildResult.build();
                });
            });
        }
        else {
            res = nonShardBuild.get();
        }

        return res;
    }

    // consulo: asynchronous form of the upstream `while (condition) { body }` whose body suspends
    private static CompletableFuture<@Nullable Void> whileAsync(BooleanSupplier condition, Supplier<CompletableFuture<?>> body) {
        if (!condition.getAsBoolean()) {
            return CompletableFuture.completedFuture(null);
        }
        return body.get().thenCompose(ignored -> whileAsync(condition, body));
    }

    private CompletableFuture<?> runBazelShutDown(ProjectView projectView, TaskId taskId) {
        BazelCommand command =
            myBazelRunner.buildBazelCommand(projectView, builder ->
                builder.shutDown()
            );
        return myBazelRunner.runBazelCommand(command, taskId)
            .waitAndGetResult();
    }

    public CompletableFuture<Set<Path>> extractAspectOutputPaths(BazelBspAspectsManagerResult buildAspectResult) {
        return measured(
            "Reading aspect output paths",
            () -> CompletableFuture.completedFuture(buildAspectResult.bepOutput().filesByInfoOutputGroup())
        );
    }

    private static final Logger LOG = Logger.getInstance(ProjectResolver.class);

    // consulo: the failure of a CompletableFuture stage is wrapped into a CompletionException
    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }
}
