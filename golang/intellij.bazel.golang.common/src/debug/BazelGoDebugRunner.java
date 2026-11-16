package org.jetbrains.bazel.golang.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.runner.DefaultProgramRunner;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.go.debug.GoDlvNativeDebuggerProvider;
import consulo.nativeDev.debugger.NativeDebugSessionStarter;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.util.lang.function.Condition;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

// consulo: upstream extends GoBuildingRunner and attaches the IntelliJ Go debugger (com.goide.dlv: DlvDebugProcess over a
// DlvRemoteVmConnection) to the Delve that GoBuildingRunningState starts. Here consulo-native-dev starts the session: the debug target
// of the state (the binary that ScriptPathDebugBeforeRunTaskProvider built with `bazel run --script_path`) is launched by the Delve
// debugger of consulo-google-go (GoDlvNativeDebuggerProvider, `dlv dap`), picked by its id
@ExtensionImpl
public class BazelGoDebugRunner extends DefaultProgramRunner {
    @Override
    public String getRunnerId() {
        return "BazelGoDebugRunner";
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        // if target cannot be debugged, do not offer debugging it
        if (!executorId.equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            return false;
        }
        if (!(profile instanceof BazelRunConfiguration configuration)) {
            return false;
        }
        BazelRunHandler handler = configuration.getHandler();
        return handler instanceof BazelGoRunHandler || handler instanceof BazelGoTestHandler;
    }

    // consulo: upstream execute(environment, state) = resolvedPromise(attachVM(state, environment)); the session is started on the UI
    //   thread, like upstream XDebuggerManager.newSessionBuilder(starter).startSession()
    @Override
    protected RunContentDescriptor doExecute(RunProfileState state, ExecutionEnvironment environment) throws ExecutionException {
        GoDebugCommandLineState goState = (GoDebugCommandLineState) state;
        goState.patchNativeState();
        // consulo: upstream runs GoBuildingRunningState.prepareStateInBGT under a modal progress
        //   (go.debug.progress.manager.preparing.process.title); GoRunningState of consulo-google-go has no such preparation, the
        //   command line of the state is created right away
        NativeDebugTarget target = goState.createDebugTarget();
        // TODO consulo: upstream BazelDebugProcessStarter adds the actions of the ExecutionResult of the state
        //   (XDebugSessionImpl.addExtraActions / addRestartActions) and, for a test, its SMTRunner console (GoTestRunningState.execute);
        //   NativeDebugSessionStarter creates the debug process and its console, there is no API to give it either. A test target is
        //   debugged like a binary
        return NativeDebugSessionStarter.getInstance().startSession(environment, target, GoDlvNativeDebuggerProvider.ID);
    }

    /**
     * Builds the stop condition for the Delve debugger attach. With a Delve {@code processHandler} the attach is
     * process-bound — {@code RemoteVmConnection.open} retries until Delve is reachable or its process exits
     * ({@code doOpen} uses {@code maxAttemptCount = -1}) — instead of the default bounded ~19s connect window. This path
     * opens the connection right after launching Delve (unlike the standard {@code GoDebugStarter}, which waits for
     * {@code getDebugClientAddress()}), so on a slow Delve startup the bounded window races the port opening. Returns
     * {@code null} (bounded window) when no process handler is available.
     */
    // consulo: upstream top-level function of BazelGoDebugRunner.kt; unused, since GoDlvDebugProcess owns the connection to Delve
    //   (its socket DAP retries until Delve listens), kept for the upstream BazelGoDebugRunnerTest
    public static @Nullable Condition<Void> delveAttachStopCondition(@Nullable ProcessHandler processHandler) {
        if (processHandler == null) {
            return null;
        }
        return it -> processHandler.isProcessTerminating() || processHandler.isProcessTerminated();
    }
}
