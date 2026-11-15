package org.jetbrains.bazel.debug.connector;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Breakpoint;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.ContinueExecutionRequest;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugRequest;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.EvaluateRequest;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.EvaluateResponse;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.GetChildrenRequest;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.GetChildrenResponse;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.ListFramesRequest;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.ListFramesResponse;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Location;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.PauseThreadRequest;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.SetBreakpointsRequest;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.StartDebuggingRequest;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Stepping;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.logging.Logger;
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointHandler;
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointProperties;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

// it serves as the main debug message dispatcher, it needs these methods
public class StarlarkDebugMessenger implements AutoCloseable {
    private final StarlarkSocketConnector myConnector;
    private final XDebugSession mySession;
    private final Map<Long, CompletableFuture<DebugEvent>> mySubscriptions = new HashMap<>();
    private final AtomicLong mySeq = new AtomicLong(1L);

    public StarlarkDebugMessenger(StarlarkSocketConnector connector, XDebugSession session) {
        myConnector = connector;
        mySession = session;
    }

    /**
     * This function needs to be safe to call multiple times
     */
    @Override
    public void close() {
        myConnector.close();
        mySession.stop();
    }

    public void readEventAndHandle(ThreadAwareEventHandler handler) throws IOException {
        DebugEvent event = myConnector.read();
        CompletableFuture<DebugEvent> waitingForResponse = mySubscriptions.get(event.getSequenceNumber());
        if (waitingForResponse != null) {
            waitingForResponse.complete(event);
        }
        else {
            handler.reactTo(event);
        }
    }

    private DebugRequest.Builder debugRequestTemplate() {
        return DebugRequest.newBuilder().setSequenceNumber(mySeq.getAndIncrement());
    }

    public void setBreakpoints(List<XLineBreakpoint<StarlarkBreakpointProperties>> breakpoints) {
        SetBreakpointsRequest.Builder request =
            SetBreakpointsRequest.newBuilder().addAllBreakpoint(
                breakpoints
                    .stream()
                    .map(this::createSDPBreakpoint)
                    .toList()
            );
        send(debugRequestTemplate().setSetBreakpoints(request));
    }

    public CompletableFuture<@Nullable ListFramesResponse> listFrames(long threadId) {
        ListFramesRequest.Builder request =
            ListFramesRequest
                .newBuilder()
                .setThreadId(threadId);
        return sendAndSubscribe(debugRequestTemplate().setListFrames(request), DebugEvent::getListFrames);
    }

    public CompletableFuture<@Nullable GetChildrenResponse> getChildren(long threadId, long valueId) {
        GetChildrenRequest.Builder request =
            GetChildrenRequest
                .newBuilder()
                .setThreadId(threadId)
                .setValueId(valueId);
        return sendAndSubscribe(debugRequestTemplate().setGetChildren(request), DebugEvent::getGetChildren);
    }

    public CompletableFuture<@Nullable EvaluateResponse> evaluate(long threadId, String expression) {
        EvaluateRequest.Builder request =
            EvaluateRequest
                .newBuilder()
                .setThreadId(threadId)
                .setStatement(expression);
        return sendAndSubscribe(debugRequestTemplate().setEvaluate(request), DebugEvent::getEvaluate);
    }

    private Breakpoint createSDPBreakpoint(XLineBreakpoint<StarlarkBreakpointProperties> breakpoint) {
        String path = StarlarkBreakpointHandler.absolutePath(breakpoint);
        int line = breakpoint.getLine() + 1; // XLineBreakpoints' lines are 0-indexed; we want 1-indexed
        Location.Builder location =
            Location
                .newBuilder()
                .setPath(path)
                .setLineNumber(line);
        return Breakpoint
            .newBuilder()
            .setLocation(location)
            .build();
    }

    public void pauseAllThreads() {
        PauseThreadRequest.Builder request =
            PauseThreadRequest
                .newBuilder()
                .setThreadId(0);
        send(debugRequestTemplate().setPauseThread(request));
    }

    public void resumeAllThreads() {
        resumeThread(0);
    }

    // consulo: overload for the upstream default argument (stepping = Stepping.NONE)
    public void resumeThread(long threadId) {
        resumeThread(threadId, Stepping.NONE);
    }

    public void resumeThread(long threadId, Stepping stepping) {
        ContinueExecutionRequest.Builder request =
            ContinueExecutionRequest
                .newBuilder()
                .setThreadId(threadId)
                .setStepping(stepping);
        send(debugRequestTemplate().setContinueExecution(request));
    }

    public void startDebugging() {
        StartDebuggingRequest.Builder request = StartDebuggingRequest.newBuilder();
        send(debugRequestTemplate().setStartDebugging(request));
    }

    private void send(DebugRequest.Builder receiver) {
        try {
            myConnector.write(receiver.build());
        }
        catch (Exception e) {
            if (!(e instanceof IOException) && !(e instanceof CancellationException)) {
                LOG.error(e); // other exceptions are not expected
            }
            close();
        }
        // closing this Messenger is not done in finally{}, because we don't want to do it if no exception occurred
    }

    private <ResponseT extends @Nullable Object> CompletableFuture<ResponseT> sendAndSubscribe(
        DebugRequest.Builder receiver,
        Function<DebugEvent, ResponseT> responseExtractor
    ) {
        ResponseFuture<ResponseT> responseFuture = new ResponseFuture<>(responseExtractor);
        mySubscriptions.put(receiver.getSequenceNumber(), responseFuture);
        send(receiver);
        return responseFuture.getTypedResponseFuture();
    }

    private static class ResponseFuture<ResponseT extends @Nullable Object> extends CompletableFuture<DebugEvent> {
        private final CompletableFuture<ResponseT> myTypedResponseFuture;

        ResponseFuture(Function<DebugEvent, ResponseT> responseExtractor) {
            myTypedResponseFuture = this.thenApply(responseExtractor);
        }

        CompletableFuture<ResponseT> getTypedResponseFuture() {
            return myTypedResponseFuture;
        }
    }

    private static final Logger LOG = Logger.getInstance(StarlarkDebugMessenger.class);
}
