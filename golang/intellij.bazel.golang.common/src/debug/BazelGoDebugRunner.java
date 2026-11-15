package org.jetbrains.bazel.golang.debug;

import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.process.ProcessHandler;
import consulo.util.lang.function.Condition;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

// TODO consulo: not registered (upstream programRunner). IntelliJ GoBuildingRunner and the Delve debugger (com.goide.dlv:
//  DlvDebugProcess, DlvRemoteVmConnection, DlvDisconnectOption) are missing in consulo-google-go, and
//  GoBuildingRunningState.debugServerAddress / XDebugSessionImpl.addExtraActions are not available; the runner keeps
//  its checks, attaching the debugger is kept commented out
public class BazelGoDebugRunner /* : GoBuildingRunner() */ {
    public String getRunnerId() {
        return "BazelGoDebugRunner";
    }

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

    // TODO consulo: GoBuildingRunner.execute(environment, state): Promise<RunContentDescriptor?> and the Delve attach
    // override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> =
    //   resolvedPromise(attachVM(state as GoBuildingRunningState<*>, environment))
    //
    // private fun attachVM(state: GoBuildingRunningState<*>, executionEnvironment: ExecutionEnvironment): RunContentDescriptor {
    //   val project = executionEnvironment.project
    //   (state as GoDebugCommandLineState).patchNativeState()
    //   ProgressManager.getInstance().runProcessWithProgressSynchronously(
    //     { ReadAction.run<RuntimeException>(state::prepareStateInBGT) },
    //     BazelPluginBundle.message("go.debug.progress.manager.preparing.process.title"),
    //     false,
    //     project,
    //   )
    //   val starter = BazelDebugProcessStarter(state.execute(executionEnvironment.executor, this), state)
    //   return XDebuggerManager.getInstance(project).newSessionBuilder(starter)
    //     .environment(executionEnvironment)
    //     .startSession().runContentDescriptor!!
    // }

    /**
     * Builds the stop condition for the Delve debugger attach. With a Delve {@code processHandler} the attach is
     * process-bound — {@code RemoteVmConnection.open} retries until Delve is reachable or its process exits
     * ({@code doOpen} uses {@code maxAttemptCount = -1}) — instead of the default bounded ~19s connect window. This path
     * opens the connection right after launching Delve (unlike the standard {@code GoDebugStarter}, which waits for
     * {@code getDebugClientAddress()}), so on a slow Delve startup the bounded window races the port opening. Returns
     * {@code null} (bounded window) when no process handler is available.
     */
    // consulo: upstream top-level function of BazelGoDebugRunner.kt
    public static @Nullable Condition<Void> delveAttachStopCondition(@Nullable ProcessHandler processHandler) {
        if (processHandler == null) {
            return null;
        }
        return it -> processHandler.isProcessTerminating() || processHandler.isProcessTerminated();
    }
}

// TODO consulo: XDebugProcessStarter for the Delve debugger, see BazelGoDebugRunner
// private class BazelDebugProcessStarter(
//   private val executionResult: ExecutionResult,
//   val state: GoBuildingRunningState<*>
// ) : XDebugProcessStarter() {
//   override fun start(session: XDebugSession): XDebugProcess {
//     val sessionImpl = session as XDebugSessionImpl
//     sessionImpl.addExtraActions(*executionResult.actions)
//     (executionResult as? DefaultExecutionResult)?.let {
//       sessionImpl.addRestartActions(*it.restartActions)
//     }
//     val connection = DlvRemoteVmConnection(DlvDisconnectOption.KILL)
//     val process = DlvDebugProcess(session, connection, executionResult, true)
//     connection.open(state.debugServerAddress, delveAttachStopCondition(executionResult.processHandler))
//     return process
//   }
// }
