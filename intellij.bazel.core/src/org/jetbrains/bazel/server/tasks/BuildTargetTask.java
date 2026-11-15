package org.jetbrains.bazel.server.tasks;

import consulo.application.progress.Task;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.action.SaveAllFiles;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.progress.ConsoleService;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.run.task.BazelBuildTaskListener;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bsp.protocol.CompileParams;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public interface BuildTargetTask {
    CompletableFuture<BazelStatus> build(
        BazelServerFacade server,
        List<Label> targetIds,
        TaskConsole buildConsole,
        TaskId taskId,
        List<String> debugFlags
    );

    static CompletableFuture<BazelStatus> runBuildTargetTask(
        List<Label> targetIds,
        Project project,
        boolean isDebug,
        BuildTargetTask buildTargetTask,
        @Nullable Supplier<? extends CompletableFuture<?>> customRedoAction
    ) {
        // consulo: usage statistics are not ported
        // BazelBuildInvocationCollector.logStarted(project, targetIds.size)
        return SaveAllFiles.saveAllFiles().thenCompose(ignored -> {
            // consulo: withBackgroundProgress -> Task.Backgroundable.queue
            CompletableFuture<BazelStatus> result = new CompletableFuture<>();
            Task.Backgroundable.queue(project, BazelPluginLocalize.backgroundProgressBuildingTargets(), true, indicator -> {
                try {
                    result.complete(buildInBackground(targetIds, project, isDebug, buildTargetTask, customRedoAction).join());
                }
                catch (Throwable e) {
                    result.completeExceptionally(BuildTargetTaskInternals.unwrap(e));
                }
            });
            return result;
        });
        // consulo: usage statistics are not ported (upstream `finally { BazelBuildInvocationCollector.logFinished(project) }`)
    }

    // consulo: the body of upstream withBackgroundProgress { } in runBuildTargetTask
    private static CompletableFuture<BazelStatus> buildInBackground(
        List<Label> targetIds,
        Project project,
        boolean isDebug,
        BuildTargetTask buildTargetTask,
        @Nullable Supplier<? extends CompletableFuture<?>> customRedoAction
    ) {
        // some languages require running `bazel build` with additional flags before debugging. e.g., python, c++
        // when this happens, isDebug should be set to true, and flags from "debug_flags" section of the project view file will be added
        CompletableFuture<List<String>> debugFlagsFuture = isDebug
            ? BazelServerConnection.getConnection(project).runWithServer(
                it -> CompletableFuture.completedFuture(ProjectViewExtensions.getDebugFlags(it.getProjectView()))
            )
            : CompletableFuture.completedFuture(List.of());
        return debugFlagsFuture.thenCompose(debugFlags -> {
            TaskId.TaskGroupId taskGroupId = new TaskId.TaskGroupId("build-" + BuildTargetTaskInternals.randomHex(8));
            TaskId taskId = taskGroupId.task("build-" + project.getName() + "-" + BuildTargetTaskInternals.randomHex(8));
            return BazelServerConnection.getConnection(project).runWithServer(taskId, server -> {
                TaskConsole bspBuildConsole = ConsoleService.getInstance(project).getBuildConsole();

                BazelBuildTaskListener taskListener = new BazelBuildTaskListener(bspBuildConsole);
                BazelTaskEventsService.getInstance(project).saveListener(taskGroupId, taskListener);

                CompletableFuture<BazelStatus> status;
                try {
                    // consulo: upstream cancels the coroutineScope, which cancels the build started in it
                    AtomicReference<@Nullable CompletableFuture<BazelStatus>> buildDeferredRef = new AtomicReference<>();
                    LocalizeValue message;
                    if (targetIds.size() == 0) {
                        message = BazelPluginLocalize.consoleTaskBuildNoTargets();
                    }
                    else if (targetIds.size() == 1) {
                        message = BazelPluginLocalize.consoleTaskBuildInProgressOne(
                            BazelRepoMappingUtils.toShortString(targetIds.get(0), project)
                        );
                    }
                    else {
                        message = BazelPluginLocalize.consoleTaskBuildInProgressMany(targetIds.size());
                    }
                    bspBuildConsole.startTask(
                        /* taskId = */ taskId,
                        /* title = */ BazelPluginLocalize.consoleTaskBuildTitle(),
                        /* message = */ message,
                        /* cancelAction = */ () -> {
                            CompletableFuture<BazelStatus> buildDeferred = buildDeferredRef.get();
                            if (buildDeferred != null) {
                                buildDeferred.cancel(true);
                            }
                        },
                        /* redoAction = */ customRedoAction != null
                            ? customRedoAction
                            : () -> runBuildTargetTask(targetIds, project, isDebug, buildTargetTask)
                    );

                    CompletableFuture<BazelStatus> buildDeferred =
                        buildTargetTask.build(server, targetIds, bspBuildConsole, taskId, debugFlags);
                    buildDeferredRef.set(buildDeferred);
                    status = new BspTaskStatusLogger(buildDeferred, bspBuildConsole, taskId).getStatus();
                }
                catch (Throwable e) {
                    status = CompletableFuture.failedFuture(e);
                }
                return status
                    .whenComplete((result, error) -> BazelTaskEventsService.getInstance(project).removeListener(taskGroupId))
                    .thenApply(it -> {
                        // asyncRefresh() refreshes the whole VFS, which includes all projects that have ever been opened in the IDE, not just the current one.
                        // That can lead to long UI freezes, which is why asyncRefresh() is now deprecated in IJ platform.
                        // On the other hand, BazelOutputFileHardLinks handles VFS refresh granularly:
                        // only on project sync, and only for changed Bazel outputs that are used in the current project.
                        if (!server.getOutFileHardLinks().getAllHardLinksCreatedSuccessfully()) {
                            VirtualFileManager.getInstance().asyncRefresh(null);
                        }
                        return it;
                    });
            });
        });
    }

    // consulo: overload for the upstream default argument (customRedoAction = null)
    static CompletableFuture<BazelStatus> runBuildTargetTask(
        List<Label> targetIds,
        Project project,
        boolean isDebug,
        BuildTargetTask buildTargetTask
    ) {
        return runBuildTargetTask(targetIds, project, isDebug, buildTargetTask, null);
    }

    final class DefaultBuildTargetTask implements BuildTargetTask {
        public static final DefaultBuildTargetTask INSTANCE = new DefaultBuildTargetTask();

        private DefaultBuildTargetTask() {
        }

        @Override
        public CompletableFuture<BazelStatus> build(
            BazelServerFacade server,
            List<Label> targetIds,
            TaskConsole buildConsole,
            TaskId taskId,
            List<String> debugFlags
        ) {
            List<String> arguments = new ArrayList<>(debugFlags);
            arguments.add("--keep_going");
            CompileParams compileParams = new CompileParams(taskId, targetIds, arguments);
            return server.buildTargetCompile(compileParams).thenApply(it -> it.statusCode());
        }
    }

    static CompletableFuture<BazelStatus> runBuildTargetTask(
        List<Label> targetIds,
        Project project,
        boolean isDebug,
        @Nullable Supplier<? extends CompletableFuture<?>> customRedoAction
    ) {
        return runBuildTargetTask(targetIds, project, isDebug, DefaultBuildTargetTask.INSTANCE, customRedoAction);
    }

    // consulo: overloads for the upstream default arguments (isDebug = false, customRedoAction = null)
    static CompletableFuture<BazelStatus> runBuildTargetTask(
        List<Label> targetIds,
        Project project,
        boolean isDebug
    ) {
        return runBuildTargetTask(targetIds, project, isDebug, DefaultBuildTargetTask.INSTANCE, null);
    }

    static CompletableFuture<BazelStatus> runBuildTargetTask(
        List<Label> targetIds,
        Project project
    ) {
        return runBuildTargetTask(targetIds, project, false);
    }
}

// consulo: helpers of the Java port (Kotlin Random.nextBytes(8).toHexString(), future exception unwrapping)
final class BuildTargetTaskInternals {
    private BuildTargetTaskInternals() {
    }

    static String randomHex(int size) {
        byte[] randomBytes = new byte[size];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    static Throwable unwrap(Throwable throwable) {
        Throwable result = throwable;
        while ((result instanceof CompletionException || result instanceof ExecutionException) && result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
