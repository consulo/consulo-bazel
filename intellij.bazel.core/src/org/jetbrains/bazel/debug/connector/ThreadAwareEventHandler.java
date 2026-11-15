package org.jetbrains.bazel.debug.connector;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Error;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.PausedThread;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.ThreadContinuedEvent;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.ThreadPausedEvent;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointProperties;
import org.jetbrains.bazel.debug.platform.StarlarkDebuggerEvaluator;
import org.jetbrains.bazel.debug.platform.StarlarkSuspendContext;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ThreadAwareEventHandler {
    private final XDebugSession mySession;
    private final StarlarkDebugMessenger myMessenger;
    private final BiFunction<String, Integer, @Nullable XLineBreakpoint<StarlarkBreakpointProperties>> myBreakpointResolver;

    // maps thread IDs to PausedThreads
    private final Map<Long, PausedThread> myPausedThreads = new LinkedHashMap<>();
    private final StarlarkValueComputer myValueComputer;
    private final StarlarkDebuggerEvaluator.Provider myEvaluatorProvider;

    public ThreadAwareEventHandler(
        XDebugSession session,
        StarlarkDebugMessenger messenger,
        BiFunction<String, Integer, @Nullable XLineBreakpoint<StarlarkBreakpointProperties>> breakpointResolver
    ) {
        mySession = session;
        myMessenger = messenger;
        myBreakpointResolver = breakpointResolver;
        myValueComputer = new StarlarkValueComputer(messenger);
        myEvaluatorProvider = new StarlarkDebuggerEvaluator.Provider(myValueComputer, messenger);
    }

    public void reactTo(DebugEvent event) {
        switch (event.getPayloadCase()) {
            case ERROR -> handleError(event.getError());
            case THREAD_PAUSED -> handleThreadPaused(event.getThreadPaused());
            case THREAD_CONTINUED -> handleThreadContinued(event.getThreadContinued());
            default -> {
            }
        }
    }

    /**
     * Creates an immutable snapshot of paused thread set
     */
    private List<PausedThread> getPausedThreadList() {
        return List.copyOf(myPausedThreads.values());
    }

    private void handleError(Error event) {
        mySession.reportError(event.getMessage());
    }

    private void handleThreadPaused(ThreadPausedEvent event) {
        long primaryThreadId = event.getThread().getId();
        myPausedThreads.put(primaryThreadId, event.getThread());

        StarlarkSuspendContext suspendContext =
            new StarlarkSuspendContext(
                getPausedThreadList(),
                myValueComputer,
                myEvaluatorProvider,
                primaryThreadId
            );

        switch (event.getThread().getPauseReason()) {
            case HIT_BREAKPOINT -> handleBreakpointHit(event, suspendContext, primaryThreadId);
            case INITIALIZING -> {
                /* ignore */
            }
            default -> mySession.positionReached(suspendContext);
        }
    }

    private void handleBreakpointHit(
        ThreadPausedEvent event,
        StarlarkSuspendContext suspendContext,
        long primaryThreadId
    ) {
        XLineBreakpoint<StarlarkBreakpointProperties> breakpoint =
            myBreakpointResolver.apply(event.getThread().getLocation().getPath(), event.getThread().getLocation().getLineNumber());
        Boolean shouldStop =
            breakpoint != null ? mySession.breakpointReached(breakpoint, null, suspendContext) : null;
        if (!Boolean.TRUE.equals(shouldStop)) {
            myMessenger.resumeThread(primaryThreadId);
        }
    }

    private void handleThreadContinued(ThreadContinuedEvent event) {
        long threadId = event.getThreadId();
        myPausedThreads.remove(threadId);
    }
}
