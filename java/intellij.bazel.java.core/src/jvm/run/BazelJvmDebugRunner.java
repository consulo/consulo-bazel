package org.jetbrains.bazel.jvm.run;

import com.intellij.java.debugger.impl.DebuggerManagerEx;
import com.intellij.java.debugger.impl.DebuggerSession;
import com.intellij.java.debugger.impl.DefaultDebugEnvironment;
import com.intellij.java.debugger.impl.engine.DebugProcessImpl;
import com.intellij.java.debugger.impl.engine.JavaDebugProcess;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.ProcessCanceledException;
import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugProcessStarter;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.GenericProgramRunner;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.project.Project;
import org.jdom.Element;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

@ExtensionImpl
public class BazelJvmDebugRunner extends GenericProgramRunner<BazelJvmDebugRunner.BazelDebugRunnerSetting> {
    @Override
    public String getRunnerId() {
        return "BazelJvmDebugRunner";
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
        if (configuration.getTargets().size() != 1) {
            return false;
        }
        BazelRunHandler handler = configuration.getHandler();
        return handler instanceof JvmRunHandler || handler instanceof JvmTestHandler;
    }

    @Override
    protected @Nullable RunContentDescriptor doExecute(RunProfileState state, ExecutionEnvironment environment) throws ExecutionException {
        // cast should always succeed, because canRun(...) checks for a compatible profile
        JvmDebuggableCommandLineState debuggableState = (JvmDebuggableCommandLineState) state;

        AtomicReference<@Nullable ExecutionException> ex = new AtomicReference<>();
        AtomicReference<@Nullable RunContentDescriptor> result = new AtomicReference<>();
        Project project = environment.getProject();
        Application.get().invokeAndWait(() -> {
            DefaultDebugEnvironment debugEnvironment = debuggableState.createDebugEnvironment(environment);
            try {
                DebuggerSession debuggerSession =
                    DebuggerManagerEx
                        .getInstanceEx(project)
                        .attachVirtualMachine(debugEnvironment);
                if (debuggerSession == null) {
                    throw new IllegalStateException("VM attachment failed");
                }
                // consulo: upstream newSessionBuilder(starter).environment(environment).startSession()
                RunContentDescriptor descriptor = XDebuggerManager.getInstance(project)
                    .startSession(environment, new BspDebugProcessStarter(debuggerSession))
                    .getRunContentDescriptor();
                result.set(descriptor);
            }
            catch (ProcessCanceledException ignored) {
                // ignore
            }
            catch (ExecutionException e) {
                ex.set(e);
            }
        });
        ExecutionException exception = ex.get();
        if (exception != null) {
            throw exception;
        }
        return result.get();
    }

    // consulo: upstream public top-level class of BazelJvmDebugRunner.kt
    public static class BazelDebugRunnerSetting implements RunnerSettings {
        @Override
        public void readExternal(Element element) {
            // empty settings, don't do anything
        }

        @Override
        public void writeExternal(Element element) {
            // empty settings, don't do anything
        }
    }

    private static class BspDebugProcessStarter implements XDebugProcessStarter {
        private final DebuggerSession myDebuggerSession;

        BspDebugProcessStarter(DebuggerSession debuggerSession) {
            myDebuggerSession = debuggerSession;
        }

        @Override
        public XDebugProcess start(XDebugSession session) {
            DebugProcessImpl debugProcess = myDebuggerSession.getProcess();
            // consulo: XDebugSession has addExtraActions/addRestartActions, no cast to XDebugSessionImpl is needed
            ExecutionResult executionResult = debugProcess.getExecutionResult();
            session.addExtraActions(executionResult.getActions());
            if (executionResult instanceof DefaultExecutionResult defaultExecutionResult) {
                session.addRestartActions(defaultExecutionResult.getRestartActions());
            }
            return JavaDebugProcess.create(session, myDebuggerSession);
        }
    }
}
