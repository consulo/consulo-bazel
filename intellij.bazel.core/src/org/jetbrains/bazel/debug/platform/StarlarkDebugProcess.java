package org.jetbrains.bazel.debug.platform;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Stepping;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.breakpoint.XBreakpointHandler;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.ui.ExecutionConsole;
import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger;
import org.jetbrains.bazel.debug.connector.StarlarkSocketConnector;
import org.jspecify.annotations.Nullable;

public class StarlarkDebugProcess extends XDebugProcess {
    private final ExecutionConsole myConsole;

    private final StarlarkDebugMessenger myMessenger;
    private final XBreakpointHandler<?>[] myBreakpointHandler;

    public StarlarkDebugProcess(
        StarlarkSocketConnector connector,
        XDebugSession session,
        XBreakpointHandler<?> breakpointHandler,
        ExecutionConsole console
    ) {
        super(session);
        myConsole = console;
        session.setPauseActionSupported(true);
        myMessenger = new StarlarkDebugMessenger(connector, session);
        myBreakpointHandler = new XBreakpointHandler<?>[]{breakpointHandler};
    }

    @Override
    public void sessionInitialized() {
        myMessenger.startDebugging();
    }

    @Override
    public void startPausing() {
        myMessenger.pauseAllThreads();
    }

    @Override
    public XDebuggerEditorsProvider getEditorsProvider() {
        return new StarlarkDebuggerEditorsProvider();
    }

    @Override
    public XBreakpointHandler<?>[] getBreakpointHandlers() {
        return myBreakpointHandler;
    }

    @Override
    public void stop() {
        myMessenger.close();
    }

    @Override
    public void resume(@Nullable XSuspendContext context) {
        myMessenger.resumeAllThreads();
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        startStep(context, Stepping.INTO);
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        startStep(context, Stepping.OVER);
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        startStep(context, Stepping.OUT);
    }

    private void startStep(@Nullable XSuspendContext context, Stepping steppingType) {
        XExecutionStack activeExecutionStack = context != null ? context.getActiveExecutionStack() : null;
        if (activeExecutionStack != null && activeExecutionStack.getTopFrame() instanceof StarlarkStackFrame topActiveFrame) {
            myMessenger.resumeThread(topActiveFrame.getThreadId(), steppingType);
        }
    }

    @Override
    public ExecutionConsole createConsole() {
        return myConsole;
    }

    public boolean isSuspended() {
        XDebugSession it = this.getSession();
        // if the top frame is not available, it is not a normal suspension
        if (it == null) {
            return true;
        }
        XSuspendContext suspendContext = it.getSuspendContext();
        XExecutionStack activeExecutionStack = suspendContext != null ? suspendContext.getActiveExecutionStack() : null;
        return it.isPaused() && !it.isStopped() && activeExecutionStack != null && activeExecutionStack.getTopFrame() != null;
    }
}
