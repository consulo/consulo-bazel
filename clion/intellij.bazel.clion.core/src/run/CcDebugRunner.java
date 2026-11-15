package org.jetbrains.bazel.clion.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.build.ui.event.BuildEventFactory;
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
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.action.SaveAllFiles;
import org.jetbrains.bazel.clion.workspace.CcTargetUtils;
import org.jetbrains.bazel.clion.workspace.CcCompilerInfo;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.progress.ConsoleService;
import org.jetbrains.bazel.progress.ShowConsole;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.GenericRunConfigurationState;
import org.jetbrains.bazel.run.task.BazelBuildTaskListener;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bazel.utils.ExecutableInfo;
import org.jetbrains.bazel.utils.ExecutableInfo.RunfileManifestOnlyException;
import org.jetbrains.bazel.utils.ExecutableInfo.UnexpectedScriptContentException;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.RunParams;
import org.jetbrains.bsp.protocol.RunResult;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TaskId.TaskGroupId;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        return handler instanceof CcRunHandler || handler instanceof CcRunHandler.CcTestHandler;
    }

    @Override
    protected CompletableFuture<RunContentDescriptor> executeImpl(RunProfileState state, ExecutionEnvironment environment)
        throws ExecutionException {
        if (!(state instanceof CcDebugCommandLineState ccState)) {
            throw new ExecutionException(BazelCLionCoreLocalize.debugErrorInvalidState());
        }

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
            prepareDebugSession(environment, ccState, console, taskId).thenCompose(ignored ->
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
                console.finishTask(
                    taskId,
                    BazelCLionCoreLocalize.debugTaskStatusFailed(),
                    Application.get().getInstance(BuildEventFactory.class).createFailureResult(cause)
                );
            }
            taskEvents.removeListener(taskGroupId);
        });
    }

    private record CcDebugContext(
        TaskId taskId,
        TaskConsole console,
        ExecutionEnvironment environment,
        GenericRunConfigurationState state
    ) {
    }

    // consulo: @Throws(ExecutionException::class) suspend fun -> the future fails with the ExecutionException
    private static CompletableFuture<@Nullable Void> prepareDebugSession(
        ExecutionEnvironment environment,
        CcDebugCommandLineState state,
        TaskConsole console,
        TaskId taskId
    ) {

        // consulo: the upstream local function; the key of the subtask is also the key of its message
        class SubTasks {
            <T> CompletableFuture<T> subTask(
                String key,
                Function<CcDebugContext, CompletableFuture<T>> body
            ) {
                return util.withSubtask(
                    console,
                    taskId.subTask(key),
                    LocalizeKey.of(BazelCLionCoreLocalize.ID, key).getValue(),
                    subTaskId -> {
                        CcDebugContext taskCtx = new CcDebugContext(subTaskId, console, environment, state.getBazelState());
                        return body.apply(taskCtx);
                    }
                );
            }
        }
        SubTasks subTasks = new SubTasks();

        return subTasks.subTask("debug.task.discover.target", CcDebugRunner::collectDebugTargetInfo).thenCompose(targetInfo -> {

            // TODO: check if target is debuggable

            return subTasks.subTask("debug.task.discover.execution.environment", ctx -> collectExecutableInfo(ctx, targetInfo))
                .thenAccept(executionInfo -> {
                    state.setTargetInfo(targetInfo);
                    state.setExecutionInfo(executionInfo);
                });
        });
    }

    // consulo: upstream public top-level data class of CcDebugRunner.kt
    public record CcDebugTargetInfo(
        BuildTarget target,
        CcCompilerInfo compiler,
        RepoMapping repoMapping
    ) {
    }

    // consulo: @Throws(ExecutionException::class) context(ctx: CcDebugContext) suspend fun -> ctx parameter, the future fails with the
    //   ExecutionException
    private static CompletableFuture<CcDebugTargetInfo> collectDebugTargetInfo(CcDebugContext ctx) {
        List<Label> targets = BazelRunConfiguration.get(ctx.environment()).getTargets();
        if (targets.size() != 1) {
            return CompletableFuture.failedFuture(new ExecutionException(BazelCLionCoreLocalize.debugErrorNoSingleTarget()));
        }
        Label label = targets.get(0);

        // TODO: discover target configuration i.e. aquery

        Project project = ctx.environment().getProject();
        return project.getInstance(WorkspaceSnapshotService.class).currentSnapshot().thenApply(snapshot -> {
            BuildTarget target = WorkspaceHelpers.getAllTargets(snapshot)
                .filter(it -> it.getKey().label().equals(label))
                .findFirst()
                .orElse(null);
            if (target == null) {
                throw new CompletionException(new ExecutionException(BazelCLionCoreLocalize.debugErrorTargetNotFound(label)));
            }

            CcCompilerInfo compilerInfo = CcTargetUtils.findCompiler(project, snapshot, target);
            if (compilerInfo == null) {
                throw new CompletionException(new ExecutionException(BazelCLionCoreLocalize.debugErrorNoCompiler(label)));
            }

            return new CcDebugTargetInfo(target, compilerInfo, snapshot.repoMapping());
        });
    }

    // consulo: @Throws(ExecutionException::class) context(ctx: CcDebugContext) suspend fun -> ctx parameter, the future fails with the
    //   ExecutionException
    private static CompletableFuture<ExecutableInfo> collectExecutableInfo(CcDebugContext ctx, CcDebugTargetInfo targetInfo) {
        Path scriptPath;
        try {
            // consulo: upstream withContext(Dispatchers.IO) { }
            scriptPath = Files.createTempFile("bazel-cc-debug-", ".sh");
        }
        catch (IOException e) {
            return CompletableFuture.failedFuture(
                new ExecutionException(BazelCLionCoreLocalize.debugErrorScriptCreateFailed(StringUtil.notNullize(e.getMessage())))
            );
        }

        CompletableFuture<BazelStatus> statusFuture =
            BazelServerConnection.getConnection(ctx.environment().getProject()).runWithServer(ctx.taskId(), server -> {

                List<String> bazelParams = new ArrayList<>();
                bazelParams.addAll(ScriptPathBuildTargetTask.scriptPathParams(scriptPath));
                bazelParams.addAll(ProjectViewExtensions.getDebugFlags(server.getProjectView()));
                bazelParams.addAll(Utils.parseAsProgramArguments(ctx.state().getAdditionalBazelParams()));

                RunParams params = new RunParams(
                    /* taskId = */ ctx.taskId(),
                    /* target = */ targetInfo.target().getKey().label(),
                    /* checkVisibility = */ true,
                    /* arguments = */ Utils.parseAsProgramArguments(ctx.state().getProgramArguments()),
                    /* environmentVariables = */ ctx.state().getEnv().getEnvs(),
                    /* additionalBazelParams = */ bazelParams,
                    /* pidDeferred = */ null,
                    /* captureStdout = */ false
                );

                return SaveAllFiles.saveAllFiles()
                    .thenCompose(ignored -> server.buildTargetRun(params))
                    .thenApply(RunResult::statusCode);
            });

        return statusFuture.thenApply(status -> {
            if (status != BazelStatus.SUCCESS) {
                throw new CompletionException(new ExecutionException(BazelCLionCoreLocalize.debugErrorScriptGenerateFailed(status)));
            }

            String content;
            try {
                // consulo: upstream withContext(Dispatchers.IO) { }
                content = Files.readString(scriptPath);
            }
            catch (IOException e) {
                throw new CompletionException(
                    new ExecutionException(BazelCLionCoreLocalize.debugErrorScriptReadFailed(StringUtil.notNullize(e.getMessage())))
                );
            }

            try {
                return ExecutableInfo.fromBazelRunScript(content);
            }
            catch (UnexpectedScriptContentException | RunfileManifestOnlyException e) {
                throw new CompletionException(
                    new ExecutionException(BazelCLionCoreLocalize.debugErrorScriptParseFailed(StringUtil.notNullize(e.getMessage())))
                );
            }
        }).whenComplete((result, error) -> {
            // consulo: upstream finally { withContext(Dispatchers.IO) { } }
            // best effort cleanup
            try {
                Files.delete(scriptPath);
            }
            catch (IOException | RuntimeException ignored) {
            }
        });
    }
}
