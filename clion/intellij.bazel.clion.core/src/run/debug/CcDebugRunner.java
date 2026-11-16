package org.jetbrains.bazel.clion.run.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.build.ui.event.BuildEventFactory;
import consulo.execution.RunCanceledByUserException;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.runner.AsyncProgramRunner;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.localize.LocalizeKey;
import consulo.nativeDev.debugger.NativeDebugSessionStarter;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.process.ExecutionException;
import consulo.project.Project;
import org.jetbrains.bazel.clion.run.CcRunHandler;
import org.jetbrains.bazel.clion.run.debug.buildExecutable.CcDebugBuildResult;
import org.jetbrains.bazel.clion.run.debug.checkDebugInfo.CcDebugInfoCheck;
import org.jetbrains.bazel.clion.run.debug.collectTargetInfo.CcDebugTargetInfo;
import org.jetbrains.bazel.clion.run.debug.resolveDebugInfoIssues.CcDebugInfoDecision;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.progress.ConsoleService;
import org.jetbrains.bazel.progress.ShowConsole;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.task.BazelBuildTaskListener;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TaskId.TaskGroupId;
import org.jspecify.annotations.Nullable;

import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

// consulo: upstream starts the CIDR debug session (CidrRunner.startDebugDescriptor -> CcCidrLauncher.createDebugProcess) with the
// CLion debug profile; here the debug target from CcCidrLauncher.createDebugTarget is started by consulo-native-dev's
// NativeDebugSessionStarter, which picks an installed debugger (gdb, lldb, ... over DAP)
@ExtensionImpl
public class CcDebugRunner extends AsyncProgramRunner<RunnerSettings> {
    @Override
    public String getRunnerId() {
        return "CcDebugRunner";
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        if (!executorId.equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            return false;
        }
        if (!(profile instanceof BazelRunConfiguration configuration)) {
            return false;
        }
        if (configuration.getTargets().size() != 1) {
            return false;
        }
        BazelRunHandler handler = configuration.getHandler();
        return (handler instanceof CcRunHandler || handler instanceof CcRunHandler.CcTestHandler) &&
            // the debug session needs the workspace snapshot, which the sync replaces
            !SyncStatusService.isSyncInProgress(configuration.getProject());
    }

    @Override
    protected CompletableFuture<RunContentDescriptor> executeImpl(RunProfileState state, ExecutionEnvironment environment)
        throws ExecutionException {
        if (!(state instanceof CcDebugCommandLineState ccState)) {
            throw new ExecutionException(BazelCLionCoreLocalize.debugErrorInvalidState());
        }

        List<Label> targets = BazelRunConfiguration.get(environment).getTargets();
        if (targets.size() != 1) {
            throw new ExecutionException(BazelCLionCoreLocalize.debugErrorNoSingleTarget());
        }
        Label label = targets.get(0);

        // TODO consulo: CidrDebugProfileManager.getCurrentDebugProfile / showMissingDebugProfileNotification /
        //   DebugProfileErrorWithCustomNotification (com.intellij.cidr.debugger.profiles, com.jetbrains.cidr.cpp.execution) do not
        //   exist; NativeDebugSessionStarter.startSession fails with an ExecutionException when no debugger is installed
        // val debugProfile = CidrDebugProfileManager.getInstance().getCurrentDebugProfile(environment.project)
        // if (debugProfile == null) {
        //   showMissingDebugProfileNotification(environment.project)
        //   throw DebugProfileErrorWithCustomNotification()
        // }
        // ccState.debugProfile = debugProfile

        Project project = environment.getProject();
        TaskConsole console = ConsoleService.getInstance(project).getBuildConsole();
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        TaskGroupId taskGroupId = new TaskGroupId("cc-debug-" + HexFormat.of().formatHex(randomBytes));
        TaskId taskId = taskGroupId.task("debug");

        BazelTaskEventsService taskEvents = BazelTaskEventsService.getInstance(project);
        taskEvents.saveListener(taskGroupId, new BazelBuildTaskListener(console));

        BazelCoroutineService.Deferred<RunContentDescriptor> deferred = BazelCoroutineService.getInstance(project).startAsync(() ->
            prepareDebugSession(environment, ccState, label, console, taskId).thenCompose(ignored ->
                // consulo: withContext(Dispatchers.EDT) { CidrRunner.startDebugDescriptor(ccState, environment, false) }
                project.getUIAccess().giveAsync(() -> {
                    try {
                        NativeDebugTarget target = ccState.getLauncher().createDebugTarget(ccState);
                        return NativeDebugSessionStarter.getInstance().startSession(environment, target, null);
                    }
                    catch (ExecutionException e) {
                        throw new CompletionException(e);
                    }
                })
            )
        );

        console.startTask(
            /* taskId = */ taskId,
            /* title = */ BazelCLionCoreLocalize.debugTaskTitle(),
            /* message = */ BazelCLionCoreLocalize.debugTaskMessage(environment.getRunProfile().getName()),
            /* cancelAction = */ () -> deferred.cancel(true),
            /* redoAction = */ null,
            /* showConsole = */ ShowConsole.ON_FAIL
        );

        // consulo: upstream Deferred.toPromiseWithoutLogError() with onSuccess / onError
        return deferred.whenComplete((descriptor, error) -> {
            if (error == null) {
                console.finishTask(taskId, BazelCLionCoreLocalize.debugTaskStatusDone());
            }
            else {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                // the user can cancel in the console or in the debug info dialog
                if (deferred.isCancelled() || cause instanceof RunCanceledByUserException) {
                    console.finishTask(taskId, BazelCLionCoreLocalize.debugTaskStatusCanceled());
                }
                else {
                    console.finishTask(
                        taskId,
                        BazelCLionCoreLocalize.debugTaskStatusFailed(),
                        Application.get().getInstance(BuildEventFactory.class).createFailureResult(cause)
                    );
                }
            }
            taskEvents.removeListener(taskGroupId);
        });
    }

    // consulo: the upstream local functions subtask / checkBuild of prepareDebugSession; the key of the subtask is also the key of its
    //   message
    private static final class SubTasks {
        private final ExecutionEnvironment myEnvironment;
        private final CcDebugCommandLineState myState;
        private final Label myLabel;
        private final TaskConsole myConsole;
        private final TaskId myTaskId;

        SubTasks(ExecutionEnvironment environment, CcDebugCommandLineState state, Label label, TaskConsole console, TaskId taskId) {
            myEnvironment = environment;
            myState = state;
            myLabel = label;
            myConsole = console;
            myTaskId = taskId;
        }

        <T extends @Nullable Object> CompletableFuture<T> subtask(String key, Function<CcDebugContext, CompletableFuture<T>> body) {
            // the retry runs the same subtasks again, thus each run needs its own id
            return util.withSubtask(
                myConsole,
                myTaskId.uniqueSubTask(key),
                LocalizeKey.of(BazelCLionCoreLocalize.ID, key).getValue(),
                subtaskId -> {
                    CcDebugContext ctx =
                        CcDebugContext.create(subtaskId, myConsole, myEnvironment.getProject(), myLabel, myState.getBazelState());
                    return body.apply(ctx);
                }
            );
        }

        CompletableFuture<CcDebugInfoCheck.@Nullable Result> checkBuild(CcDebugBuildResult build) {
            if (build.targets().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return subtask("debug.task.check.debug.info", ctx -> CompletableFuture.completedFuture(
                checkDebugInfo.checkCcDebugInfo(ctx, build.targets())
            ));
        }
    }

    // consulo: @Throws(ExecutionException::class) suspend fun -> the future fails with the ExecutionException
    private static CompletableFuture<@Nullable Void> prepareDebugSession(
        ExecutionEnvironment environment,
        CcDebugCommandLineState state,
        Label label,
        TaskConsole console,
        TaskId taskId
    ) {
        SubTasks subTasks = new SubTasks(environment, state, label, console, taskId);
        Project project = environment.getProject();

        // the build comes first, because the debug aspect reports the configuration of the target
        return subTasks.subtask("debug.task.discover.execution.environment", buildExecutable::buildExecutable).thenCompose(build ->
            subTasks.subtask(
                "debug.task.discover.target",
                ctx -> collectTargetInfo.collectTargetInfo(ctx, build.targets(), /* reportUnknownConfiguration = */ true)
            ).thenCompose(targetInfo ->
                subTasks.checkBuild(build)
                    .thenCompose(result -> resolveDebugInfoIssues.resolveDebugInfoIssues(project, label, result, /* allowRetry = */ true))
                    .thenCompose(decision -> {
                        if (decision != CcDebugInfoDecision.RETRY_WITH_DBG) {
                            return CompletableFuture.completedFuture(new Prepared(build, targetInfo));
                        }

                        return subTasks.subtask(
                            "debug.task.rebuild",
                            ctx -> projectView.setDbgCompilationMode(ctx).thenCompose(ignored -> buildExecutable.buildExecutable(ctx))
                        ).thenCompose(rebuild ->
                            // the last sync did not use dbg, thus the snapshot usually does not contain the configuration of the new build
                            subTasks.subtask(
                                "debug.task.discover.target",
                                ctx -> collectTargetInfo.collectTargetInfo(ctx, rebuild.targets(), /* reportUnknownConfiguration = */ false)
                            ).thenCompose(newTargetInfo ->
                                subTasks.checkBuild(rebuild)
                                    .thenCompose(result ->
                                        resolveDebugInfoIssues.resolveDebugInfoIssues(project, label, result, /* allowRetry = */ false)
                                    )
                                    .thenApply(ignored -> new Prepared(rebuild, newTargetInfo))
                            )
                        );
                    })
            )
        ).<@Nullable Void>thenApply(prepared -> {
            state.setTargetInfo(prepared.targetInfo());
            state.setExecutionInfo(prepared.build().executable());
            return null;
        });
    }

    // consulo: the upstream `var build` / `var targetInfo` of prepareDebugSession
    private record Prepared(CcDebugBuildResult build, CcDebugTargetInfo targetInfo) {
    }
}
