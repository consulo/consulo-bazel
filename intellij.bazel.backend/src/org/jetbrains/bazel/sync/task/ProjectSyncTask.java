package org.jetbrains.bazel.sync.task;

import consulo.application.Application;
import consulo.application.SaveAndSyncHandler;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.bazel.backend.localize.BazelBackendLocalize;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.build.ui.event.BuildEventFactory;
import consulo.build.ui.event.MessageEvent;
import consulo.document.FileDocumentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.action.SaveAllFiles;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.run.task.BazelBuildTaskListener;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.ProjectPostSyncHook;
import org.jetbrains.bazel.sync.ProjectPreSyncHook;
import org.jetbrains.bazel.sync.ProjectSyncHook;
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment;
import org.jetbrains.bazel.sync.ProjectSyncOutcome;
import org.jetbrains.bazel.sync.ProjectSyncOutcome.ProjectSyncCompletionResult;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.SyncWorkspaceUpdater;
import org.jetbrains.bazel.sync.SyncWorkspaceUpdater.SyncWorkspaceUpdate;
import org.jetbrains.bazel.sync.projectStructure.ProjectModelApplicationTask;
import org.jetbrains.bazel.sync.status.SyncStatusService.SyncAlreadyInProgressException;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jetbrains.bazel.sync.task.ProjectSyncResult.ProjectSyncPhase;
import org.jetbrains.bazel.sync.task.ProjectSyncResult.ProjectSyncPhaseDuration;
import org.jetbrains.bazel.sync.task.SyncWorkspaceProvider.SyncPhase;
import org.jetbrains.bazel.sync.task.SyncWorkspaceProvider.SyncWorkspaceContext;
import org.jetbrains.bazel.sync.task.SyncWorkspaceProvider.SyncWorkspaceStatus;
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterHelper;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TaskId.TaskGroupId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

// consulo: suspend functions return CompletableFuture; the workspace model (MutableEntityStorage) is the classic
// BazelModelBuilder; telemetry and usage statistics are not ported
public class ProjectSyncTask {
    private static final Logger LOG = Logger.getInstance(ProjectSyncTask.class);

    private final Project myProject;
    private final ProjectSyncScope myScope;
    private final Consumer<TaskId> myOnSyncTaskStarted;

    // remember from first phase to second phase for proper sharding
    private @Nullable List<Label> myAllKnownTargets;

    // remember from first phase to second phase to fetch the repo mapping once per sync
    private @Nullable RepoMapping myKnownRepoMapping;

    public ProjectSyncTask(Project project, ProjectSyncScope scope) {
        this(project, scope, taskId -> {
        });
    }

    public ProjectSyncTask(Project project, ProjectSyncScope scope, Consumer<TaskId> onSyncTaskStarted) {
        myProject = project;
        myScope = scope;
        myOnSyncTaskStarted = onSyncTaskStarted;
    }

    public CompletableFuture<ProjectSyncOutcome> sync() {
        CompletableFuture<ProjectSyncResult> result;
        if (myScope instanceof ProjectSyncScope.Full full) {
            if (full.phased()) {
                result = phasedSync(myScope.build());
            }
            else {
                result = syncPhase(SyncPhase.SECOND, myScope.build());
            }
        }
        else {
            result = syncPhase(SyncPhase.SECOND, myScope.build());
        }
        return result.thenApply(ProjectSyncResult::toOutcome);
    }

    private CompletableFuture<ProjectSyncResult> phasedSync(boolean buildProject) {
        // consulo: Consulo has no IncompleteDependenciesService, the second phase runs without the incomplete state
        return syncPhase(SyncPhase.FIRST, false).thenCompose(firstPhaseResult -> {
            if (firstPhaseResult.completionResult() == ProjectSyncCompletionResult.CANCELLED ||
                firstPhaseResult.completionResult() == ProjectSyncCompletionResult.SKIPPED) {
                return CompletableFuture.completedFuture(firstPhaseResult);
            }
            return syncPhase(SyncPhase.SECOND, buildProject);
        });
    }

    private CompletableFuture<ProjectSyncResult> syncPhase(SyncPhase phase, boolean buildProject) {
        // consulo: Consulo has no trusted projects, every project is synced
        Application.get().invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());
        return runSyncTask(phase, buildProject);
    }

    private CompletableFuture<ProjectSyncResult> runSyncTask(SyncPhase phase, boolean buildProject) {
        TaskConsole syncConsole = util.getSyncConsole(myProject);
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        TaskId taskId = new TaskGroupId("sync-" + myProject.getName() + "-" + HexFormat.of().formatHex(randomBytes)).task("project-sync");

        try {
            SyncStatusService.getInstance(myProject).startSync();
        }
        catch (SyncAlreadyInProgressException e) {
            // consulo: usage statistics are not ported
            return CompletableFuture.completedFuture(new ProjectSyncResult(ProjectSyncCompletionResult.SKIPPED));
        }

        myOnSyncTaskStarted.accept(taskId);

        BuildEventFactory buildEventFactory = Application.get().getInstance(BuildEventFactory.class);

        // The status service publishes this result to everyone who waits for the end of the sync.
        CompletableFuture<ProjectSyncResult> result;
        try {
            LOG.debug("Starting sync project task");

            BazelBuildTaskListener taskListener = new BazelBuildTaskListener(syncConsole);
            BazelTaskEventsService.getInstance(myProject).saveListener(taskId.taskGroupId(), taskListener);

            BazelCoroutineService.Deferred<ProjectSyncResult> syncJob = BazelCoroutineService.getInstance(myProject)
                .startAsync(true, () -> doSync(taskId, phase, buildProject));

            syncConsole.startTask(
                taskId,
                BazelBackendLocalize.consoleTaskSyncTitle(),
                BazelBackendLocalize.consoleTaskSyncInProgress(),
                () -> {
                    SyncStatusService.getInstance(myProject).cancel();
                    syncJob.cancel(true);
                },
                () -> syncPhase(phase, buildProject)
            );

            result = syncJob.await().thenApply(syncResult -> {
                switch (syncResult.completionResult()) {
                    case FAILURE -> {
                        Throwable failureCause = syncResult.failureCause();
                        if (failureCause != null) {
                            LOG.error("Error syncing project", failureCause);
                        }
                        // distinguish a thrown error mid-sync from a clean run that resolved no targets
                        LocalizeValue message =
                            failureCause != null ? BazelBackendLocalize.consoleTaskSyncFailed()
                                : BazelBackendLocalize.consoleTaskSyncFatalfailure();
                        syncConsole.finishTask(
                            taskId,
                            message,
                            failureCause != null ? buildEventFactory.createFailureResult(failureCause)
                                : buildEventFactory.newFailure().createResult()
                        );
                    }
                    case PARTIAL_SUCCESS -> {
                        syncConsole.addDiagnosticMessage(
                            taskId,
                            null, -1, -1,
                            BazelBackendLocalize.consoleTaskSyncPartialsuccess(),
                            null,
                            MessageEvent.Kind.WARNING
                        );
                        syncConsole.finishTask(
                            taskId,
                            BazelBackendLocalize.consoleTaskSyncPartialsuccess(),
                            buildEventFactory.createSuccessResult(true)
                        );
                    }
                    case SUCCESS -> syncConsole.finishTask(
                        taskId,
                        BazelBackendLocalize.consoleTaskSyncSuccess()
                    );
                    case CANCELLED -> syncConsole.finishTask(
                        taskId,
                        BazelBackendLocalize.consoleTaskSyncCancelled(),
                        buildEventFactory.createSkippedResult()
                    );
                    case SKIPPED -> {
                    }
                }
                return syncResult;
            }).whenComplete((value, error) -> BazelTaskEventsService.getInstance(myProject).removeListener(taskId.taskGroupId()));
        }
        catch (Throwable e) {
            BazelTaskEventsService.getInstance(myProject).removeListener(taskId.taskGroupId());
            result = CompletableFuture.failedFuture(e);
        }

        return result
            .handle((syncResult, error) -> {
                if (error == null) {
                    return syncResult;
                }
                Throwable e = unwrap(error);
                if (e instanceof CancellationException) {
                    syncConsole.finishTask(
                        taskId,
                        BazelBackendLocalize.consoleTaskSyncCancelled(),
                        buildEventFactory.createSkippedResult()
                    );
                    return new ProjectSyncResult(ProjectSyncCompletionResult.CANCELLED, e);
                }
                LOG.error("Error syncing project", e);
                syncConsole.finishTask(
                    taskId,
                    BazelBackendLocalize.consoleTaskSyncFailed(),
                    buildEventFactory.createFailureResult(e)
                );
                return new ProjectSyncResult(ProjectSyncCompletionResult.FAILURE, e);
            })
            .whenComplete((endResult, error) -> SyncStatusService.getInstance(myProject).finishSync(
                endResult != null ? endResult.toOutcome() : new ProjectSyncResult(ProjectSyncCompletionResult.FAILURE).toOutcome()
            ));
    }

    private CompletableFuture<Void> preSync() {
        LOG.debug("Running pre sync tasks");
        return SaveAllFiles.saveAllFiles().thenCompose(ignored -> clearSyntheticTargets());
    }

    private CompletableFuture<Void> clearSyntheticTargets() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Application.get().invokeLater(() -> {
            try {
                WriteAction.run(() -> {
                    VirtualFile bazelBspDir = org.jetbrains.bazel.config.project.getRootDir(myProject).findChild(Constants.DOT_BAZELBSP_DIR_NAME);
                    VirtualFile syntheticTargets = bazelBspDir == null || !bazelBspDir.isDirectory() ? null : bazelBspDir.findChild("synthetic_targets");
                    VirtualFile[] children = syntheticTargets != null && syntheticTargets.isDirectory() ? syntheticTargets.getChildren() : null;
                    if (children != null) {
                        for (VirtualFile it : children) {
                            it.delete(this);
                        }
                    }
                });
                result.complete(null);
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private CompletableFuture<ProjectSyncResult> doSync(TaskId taskId, SyncPhase phase, boolean buildProject) {
        LocalizeValue syncActivityName = BazelBackendLocalize.consoleTaskSyncActivityName(Constants.BAZEL_DISPLAY_NAME);
        SaveAndSyncHandler saveAndSyncHandler = SaveAndSyncHandler.getInstance();
        List<ProjectSyncPhaseDuration> phaseDurations = new ArrayList<>();
        CompletableFuture<ProjectSyncResult> pipeline = preSync().thenCompose(ignored -> {
            // consulo: withBackgroundProgress + reportSequentialProgress -> background task with a progress indicator,
            // UnindexedFilesScannerExecutor.suspendScanningAndIndexingThenExecute -> DumbService.suspendIndexingAndRun,
            // SaveAndSyncHandler.withDisabledAutoSave -> blocked save on frame deactivation
            CompletableFuture<ProjectSyncResult> syncResult = new CompletableFuture<>();
            Task.Backgroundable.queue(myProject, BazelBackendLocalize.backgroundProgressSyncingProject(), true, progressReporter -> {
                try {
                    DumbService.getInstance(myProject).suspendIndexingAndRun(syncActivityName, () -> {
                        saveAndSyncHandler.blockSaveOnFrameDeactivation();
                        try {
                            syncResult.complete(executeSyncPipeline(progressReporter, taskId, phase, buildProject, phaseDurations).join());
                        }
                        finally {
                            saveAndSyncHandler.unblockSaveOnFrameDeactivation();
                        }
                    });
                }
                catch (Throwable e) {
                    syncResult.completeExceptionally(unwrap(e));
                }
            });
            return syncResult;
        });
        return pipeline.handle((syncResult, error) -> {
            if (error == null) {
                // consulo: SaveAndSyncHandler.scheduleProjectSave has no Consulo counterpart, the project is saved by the platform
                return new ProjectSyncResult(syncResult.completionResult(), syncResult.statistics(), List.copyOf(phaseDurations), syncResult.failureCause());
            }
            Throwable e = unwrap(error);
            if (e instanceof CancellationException) {
                return new ProjectSyncResult(ProjectSyncCompletionResult.CANCELLED, null, List.copyOf(phaseDurations), e);
            }
            return new ProjectSyncResult(ProjectSyncCompletionResult.FAILURE, null, List.copyOf(phaseDurations), e);
        });
    }

    private CompletableFuture<ProjectSyncResult> executeSyncPipeline(
        ProgressIndicator progressReporter,
        TaskId taskId,
        SyncPhase phase,
        boolean buildProject,
        List<ProjectSyncPhaseDuration> phaseDurations
    ) {
        boolean[] shouldUpdateProjectModel = {false};
        ProjectSyncScope[] syncScope = {myScope};
        CompletableFuture<ProjectSyncResult> pipeline = executePreSyncHooks(progressReporter, taskId).thenCompose(ignored -> {
            // A required `import` in the project view (e.g. `import local.bazelproject`) whose file is
            // missing leaves the project view effectively empty. Without stopping here the sync still
            // contacts the Bazel server and tries to resolve configurations/targets from that empty view,
            // which fails slowly with misleading secondary errors instead of the real cause. Stop now;
            // the offending import was already reported as an error by ReparseProjectViewFilePreSyncHook.
            if (!ProjectView.unresolvedRequiredImports(ProjectViewService.getInstance(myProject).getProjectView()).isEmpty()) {
                return CompletableFuture.completedFuture(new ProjectSyncResult(ProjectSyncCompletionResult.FAILURE));
            }

            return BazelServerConnection.getConnection(myProject).runWithServer(taskId, server ->
                withOutFileHardLinksSync(
                    server,
                    () -> shouldUpdateProjectModel[0] && syncScope[0] instanceof ProjectSyncScope.Full,
                    () -> syncWithServer(server, progressReporter, taskId, phase, buildProject, phaseDurations).thenApply(result -> {
                        if (result.second != null) {
                            syncScope[0] = result.second;
                            shouldUpdateProjectModel[0] = true;
                        }
                        return result.first;
                    })
                )
            );
        });
        return pipeline.handle((value, error) -> Pair.create(value, error)).thenCompose(pair ->
            executePostSyncHooks(progressReporter, taskId, shouldUpdateProjectModel[0]).handle((ignored, postError) -> {
                if (pair.second != null) {
                    throw new CompletionException(unwrap(pair.second));
                }
                if (postError != null) {
                    throw new CompletionException(unwrap(postError));
                }
                return pair.first;
            })
        );
    }

    /**
     * consulo: the body of upstream {@code runWithServer { withOutFileHardLinksSync { ... } }}. Returns the result and, when the
     * project model was updated, the effective sync scope (upstream assigns {@code syncScope} and {@code shouldUpdateProjectModel}).
     */
    private CompletableFuture<Pair<ProjectSyncResult, @Nullable ProjectSyncScope>> syncWithServer(
        BazelServerFacade server,
        ProgressIndicator progressReporter,
        TaskId taskId,
        SyncPhase phase,
        boolean buildProject,
        List<ProjectSyncPhaseDuration> phaseDurations
    ) {
        String deprecated = server.getBazelInfo().release().deprecated();
        if (deprecated != null) {
            util.getSyncConsole(myProject).addDiagnosticMessage(
                taskId,
                null, -1, -1,
                BazelBackendLocalize.progressTextSyncMightGiveIncompleteResults(deprecated),
                null,
                MessageEvent.Kind.WARNING
            );
        }

        if (!server.getBazelInfo().isConfigurationSupportEnabled()) {
            util.getSyncConsole(myProject).addDiagnosticMessage(
                taskId,
                null, -1, -1,
                BazelBackendLocalize.consoleTaskSyncConfigurationsUnsupported(),
                null,
                MessageEvent.Kind.WARNING
            );
        }

        BazelModelBuilder storage = Application.get().runReadAction((java.util.function.Supplier<BazelModelBuilder>) () -> new BazelModelBuilder(myProject));

        return ProjectSyncResult.<SyncWorkspaceUpdate>trackSyncPhase(phaseDurations, ProjectSyncPhase.COLLECT_PROJECT_DETAILS, () ->
            util.<SyncWorkspaceUpdate>withSubtask(
                util.getSyncConsole(myProject),
                taskId.subTask("base-project-sync-subtask-id"),
                buildProject ? BazelBackendLocalize.consoleTaskBaseBuildSync() : BazelBackendLocalize.consoleTaskBaseSync(),
                subtaskId -> {
                    SyncWorkspaceContext context = new SyncWorkspaceContext(
                        phase,
                        buildProject,
                        myAllKnownTargets,
                        myKnownRepoMapping,
                        server,
                        subtaskId
                    );

                    // the only snapshot update of the pipeline: the provider infers the effective scope, resolves
                    // and derives the new snapshot, all against the very same base snapshot
                    return myProject.getInstance(WorkspaceSnapshotService.class)
                        .update((WorkspaceSnapshotService.WorkspaceSnapshotUpdater<SyncWorkspaceUpdate>) previous ->
                            new SyncWorkspaceUpdater(myProject).update(myScope, previous, context)
                                .thenApply(syncUpdate -> Pair.create(syncUpdate.snapshot(), syncUpdate))
                        )
                        .thenApply(pair -> pair.second)
                        .thenCompose(workspaceUpdate ->
                            TargetStorage.getTargetStorage(myProject).awaitLoaded() // sync caches
                                .thenApply(ignored -> workspaceUpdate)
                        );
                }
            )
        ).thenCompose(syncWorkspace -> {
            // If this bazel build fails, we still want the sync hooks to be executed.
            // A fatal resolve republished the previous snapshot, so nothing was synced, and it must not be counted
            if (syncWorkspace.status() == SyncWorkspaceStatus.FATAL) {
                storage.dispose();
                return CompletableFuture.completedFuture(Pair.<ProjectSyncResult, @Nullable ProjectSyncScope>create(
                    new ProjectSyncResult(
                        ProjectSyncCompletionResult.FAILURE,
                        ProjectSyncResult.syncStatistics(List.of()),
                        List.of(),
                        null
                    ),
                    null
                ));
            }

            WorkspaceSnapshot workspaceSnapshot = syncWorkspace.snapshot();
            List<BuildTarget> syncedTargets = workspaceSnapshot.targets().allTargets().toList();
            ProjectSyncResult.ProjectSyncStatistics statistics = ProjectSyncResult.syncStatistics(syncedTargets);
            if (phase == SyncPhase.FIRST) {
                List<Label> ids = new ArrayList<>();
                for (BuildTarget it : syncedTargets) {
                    ids.add(BuildTarget.getId(it));
                }
                myAllKnownTargets = ids;
                myKnownRepoMapping = workspaceSnapshot.repoMapping();
            }

            WorkspaceImporterHelper importerHelper = new WorkspaceImporterHelper(
                myProject,
                util.getSyncConsole(myProject),
                progressReporter,
                storage,
                server.getOutputResolver(),
                server.getOutputParser(),
                server.getOutFileHardLinks(),
                server.getBazelInfo()
            );
            return importerHelper.invoke(progressReporter, workspaceSnapshot, taskId)
                .thenCompose(ignored -> util.<Void>withSubtask(
                    util.getSyncConsole(myProject),
                    progressReporter,
                    taskId.subTask("sync-hooks"),
                    BazelBackendLocalize.consoleTaskExecuteSyncHooks(),
                    subtaskId -> {
                        ProjectSyncHookEnvironment environment =
                            new ProjectSyncHookEnvironment(
                                myProject,
                                syncWorkspace.scope(),
                                server,
                                workspaceSnapshot,
                                subtaskId,
                                storage,
                                progressReporter
                            );
                        return forEachSubtask(ProjectSyncHook.getProjectSyncHooks(myProject), subtaskId, it -> it.onSync(environment));
                    }
                ))
                .thenCompose(ignored -> server.getOutFileHardLinks().onBeforeProjectModelUpdate())
                .thenCompose(ignored -> ProjectSyncResult.<Void>trackSyncPhase(phaseDurations, ProjectSyncPhase.APPLY_PROJECT_MODEL, () ->
                    updateProjectModel(progressReporter, storage, taskId)
                        .thenCompose(ignored2 -> importerHelper.invokeLate(progressReporter, workspaceSnapshot, taskId))
                ))
                .thenApply(ignored -> Pair.<ProjectSyncResult, @Nullable ProjectSyncScope>create(
                    new ProjectSyncResult(
                        syncWorkspace.status() == SyncWorkspaceStatus.PARTIAL
                            ? ProjectSyncCompletionResult.PARTIAL_SUCCESS
                            : ProjectSyncCompletionResult.SUCCESS,
                        statistics,
                        List.of(),
                        null
                    ),
                    syncWorkspace.scope()
                ))
                .whenComplete((value, error) -> {
                    if (error != null) {
                        storage.dispose();
                    }
                });
        });
    }

    private <T> CompletableFuture<T> withOutFileHardLinksSync(
        BazelServerFacade server,
        java.util.function.BooleanSupplier projectModelUpdated,
        java.util.function.Supplier<CompletableFuture<T>> action
    ) {
        server.getOutFileHardLinks().onBeforeSync();
        CompletableFuture<T> result;
        try {
            result = action.get();
        }
        catch (Throwable e) {
            result = CompletableFuture.failedFuture(e);
        }
        return result.handle((value, error) -> Pair.create(value, error)).thenCompose(pair ->
            server.getOutFileHardLinks().onAfterSync(projectModelUpdated.getAsBoolean()).thenApply(ignored -> {
                if (pair.second != null) {
                    throw new CompletionException(unwrap(pair.second));
                }
                return pair.first;
            })
        );
    }

    private CompletableFuture<Void> executePreSyncHooks(ProgressIndicator progressReporter, TaskId taskId) {
        return util.withSubtask(
            util.getSyncConsole(myProject),
            progressReporter,
            taskId.subTask("pre-sync-hooks"),
            BazelBackendLocalize.consoleTaskExecutePreSyncHooks(),
            subtaskId -> {
                ProjectPreSyncHook.ProjectPreSyncHookEnvironment environment =
                    new ProjectPreSyncHook.ProjectPreSyncHookEnvironment(
                        myProject,
                        subtaskId,
                        progressReporter
                    );

                return forEachSubtask(ProjectPreSyncHook.getProjectPreSyncHooks(myProject), subtaskId, it -> it.onPreSync(environment));
            }
        );
    }

    private CompletableFuture<Void> updateProjectModel(ProgressIndicator progressReporter, BazelModelBuilder storage, TaskId taskId) {
        return util.withSubtask(
            util.getSyncConsole(myProject),
            progressReporter,
            taskId.subTask("apply-changes"),
            BazelBackendLocalize.consoleTaskApplyChanges(),
            subtaskId -> new ProjectModelApplicationTask(myProject, subtaskId).apply(storage)
        );
    }

    private CompletableFuture<Void> executePostSyncHooks(ProgressIndicator progressReporter, TaskId taskId, boolean projectModelUpdated) {
        return util.withSubtask(
            util.getSyncConsole(myProject),
            progressReporter,
            taskId.subTask("post-sync-hooks"),
            BazelBackendLocalize.consoleTaskExecutePostSyncHooks(),
            subtaskId -> {
                ProjectPostSyncHook.ProjectPostSyncHookEnvironment environment =
                    new ProjectPostSyncHook.ProjectPostSyncHookEnvironment(
                        myProject,
                        subtaskId,
                        progressReporter,
                        projectModelUpdated
                    );

                return forEachSubtask(ProjectPostSyncHook.getProjectPostSyncHooks(myProject), subtaskId, it -> it.onPostSync(environment));
            }
        );
    }

    private <T> CompletableFuture<Void> forEachSubtask(List<T> receiver, TaskId taskId, Function<T, CompletableFuture<Void>> action) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (T item : receiver) {
            chain = chain.thenCompose(ignored -> {
                CompletableFuture<Void> result;
                try {
                    result = action.apply(item);
                }
                catch (Throwable e) {
                    result = CompletableFuture.failedFuture(e);
                }
                return result.exceptionally(error -> {
                    Throwable e = unwrap(error);
                    if (e instanceof CancellationException) {
                        throw new CompletionException(e);
                    }
                    TaskConsole syncConsole = util.getSyncConsole(myProject);
                    if (syncConsole.registerException(taskId, e)) {
                        syncConsole.addDiagnosticMessage(
                            taskId,
                            null, -1, -1,
                            LocalizeValue.of(e.getMessage() != null ? e.getMessage() : "Unknown error"),
                            null,
                            MessageEvent.Kind.ERROR
                        );
                    }
                    LOG.error(e);
                    return null;
                });
            });
        }
        return chain;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
