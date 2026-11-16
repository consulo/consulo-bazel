package org.jetbrains.bazel.python.run;

import java.util.Objects;
import com.jetbrains.python.impl.debugger.PyDebugProcess;
import com.jetbrains.python.impl.debugger.PyDebugRunner;
import com.jetbrains.python.impl.run.PythonCommandLineState;
import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.ExecutionManager;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.debug.XDebugSession;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.RunContentDescriptor;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.python.debug.BazelPyDebugPositionConverter;
import org.jetbrains.bazel.python.debug.PythonDebugCommandLineState;
import org.jetbrains.bazel.python.debug.PythonDebugUtils;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.server.tasks.BuildTargetTask;
import org.jetbrains.bsp.protocol.CompileParams;
import org.jetbrains.bsp.protocol.CompileResult;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ExtensionImpl
public class PythonBspDebugRunner extends PyDebugRunner {
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        return executorId.equals(DefaultDebugExecutor.EXECUTOR_ID) &&
            profile instanceof BazelRunConfiguration configuration &&
            configuration.getHandler() instanceof PythonBazelHandler<?> &&
            configuration.getTargets().size() == 1 &&
            configuration.getProject().getBasePath() != null;
    }

    /**
     * consulo: upstream overrides {@code createSessionEx(state, environment): Promise<XSessionStartedResult>}; the Consulo
     * {@link PyDebugRunner} creates its session synchronously in {@code doExecute}, so the session is started through
     * {@link ExecutionManager#startRunProfile} with the asynchronous {@link #createSessionEx} (what {@code AsyncProgramRunner} does)
     */
    @Override
    @RequiredUIAccess
    protected void execute(ExecutionEnvironment environment, RunProfileState state) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(
            (s, env) -> createSessionEx(s, env).thenApply(session -> {
                initSession(session, s, env.getExecutor());
                RunContentDescriptor descriptor = session.getRunContentDescriptor();
                // consulo: BaseProgramRunner.postProcess is package-private
                descriptor.setExecutionId(env.getExecutionId());
                ProgramRunner.Callback callback = env.getCallback();
                if (callback != null) {
                    callback.processStarted(descriptor);
                }
                return descriptor;
            }),
            state,
            environment
        );
    }

    // consulo: upstream Promise<XSessionStartedResult> -> CompletableFuture<XDebugSession>
    public CompletableFuture<XDebugSession> createSessionEx(RunProfileState state, ExecutionEnvironment environment) {
        if (!(state instanceof PythonDebugCommandLineState debugState)) {
            throw new IllegalStateException(BazelPluginLocalize.pythonDebugErrorWrongState().get());
        }
        Label target = debugState.getTarget();
        if (target == null) {
            throw new IllegalStateException(BazelPluginLocalize.pythonDebugErrorNoId().get());
        }
        Project project = environment.getProject();
        CompletableFuture<XDebugSession> promise = new CompletableFuture<>();
        buildTargetInDebugMode(
            project,
            target,
            debugState.getAdditionalBazelParams(),
            () ->
                // consulo: withContext(Dispatchers.EDT) { } -> UIAccess.giveAsync
                project.getUIAccess().giveAsync(() -> debugState.asPythonState().whenComplete((nativeState, error) -> {
                    if (error != null) {
                        promise.completeExceptionally(error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                        return;
                    }
                    try {
                        promise.complete(createSession(nativeState, environment));
                    }
                    catch (Throwable e) {
                        promise.completeExceptionally(e);
                    }
                })),
            it -> setPythonBuildError(promise, target, it)
        );
        return promise;
    }

    private void buildTargetInDebugMode(
        Project project,
        Label targetId,
        @Nullable String additionalBazelParams,
        Supplier<CompletableFuture<?>> onBuildComplete,
        Consumer<BazelStatus> onBuildFailed
    ) {
        BazelCoroutineService.getInstance(project).start(() ->
            BuildTargetTask.runBuildTargetTask(
                List.of(targetId),
                project,
                true,
                new PythonDebugBuildTargetTask(additionalBazelParams)
            ).thenCompose(buildStatus -> {
                CompletableFuture<?> step;
                if (buildStatus == BazelStatus.SUCCESS) {
                    step = onBuildComplete.get();
                }
                else {
                    // consulo: withContext(Dispatchers.EDT) { } -> UIAccess.giveAsync
                    step = project.getUIAccess().giveAsync(() -> onBuildFailed.accept(buildStatus));
                }
                return step.<@Nullable Object>thenApply(it -> null);
            })
        );
    }

    @Override
    protected PyDebugProcess createDebugProcess(
        XDebugSession session,
        ServerSocket serverSocket,
        ExecutionResult result,
        PythonCommandLineState pyState
    ) {
        return withPositionConverter(super.createDebugProcess(session, serverSocket, result, pyState));
    }

    private static PyDebugProcess withPositionConverter(PyDebugProcess receiver) {
        XDebugSession session = receiver.getSession();
        // canRun(...) checks that, should never fail
        Label targetId = ((BazelRunConfiguration) Objects.requireNonNull(session.getRunProfile())).getTargets().get(0);
        receiver.setPositionConverter(new BazelPyDebugPositionConverter(session.getProject(), targetId));
        return receiver;
    }

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getInstance(PythonBspDebugRunner.class);

    // consulo: upstream private top-level declarations of PythonBspDebugRunner.kt

    private static void setPythonBuildError(CompletableFuture<?> receiver, Label target, BazelStatus status) {
        receiver.completeExceptionally(new ExecutionException(BazelPluginLocalize.pythonDebugErrorBuildFailed(target, status)));
    }

    private static final String RUNNER_ID = "BspPythonDebugRunner";
}

// consulo: upstream private class of PythonBspDebugRunner.kt
class PythonDebugBuildTargetTask implements BuildTargetTask {
    private final @Nullable String myAdditionalBazelParams;

    PythonDebugBuildTargetTask(@Nullable String additionalBazelParams) {
        myAdditionalBazelParams = additionalBazelParams;
    }

    @Override
    public CompletableFuture<BazelStatus> build(
        BazelServerFacade server,
        List<Label> targetIds,
        TaskConsole buildConsole,
        TaskId taskId,
        List<String> debugFlags
    ) {
        List<String> flags = new ArrayList<>(debugFlags);
        flags.addAll(ProjectViewExtensions.getPythonDebugFlags(server.getProjectView()));
        CompileParams compileParams =
            new CompileParams(
                taskId,
                targetIds,
                PythonDebugUtils.buildPythonDebugBazelArguments(flags, myAdditionalBazelParams)
            );
        return server.buildTargetCompile(compileParams).thenApply(CompileResult::statusCode);
    }
}
