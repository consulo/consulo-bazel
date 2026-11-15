package org.jetbrains.bazel.debug.connector;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Frame;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Value;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StarlarkValueComputer {
    private final StarlarkDebugMessenger myMessenger;

    public StarlarkValueComputer(StarlarkDebugMessenger messenger) {
        myMessenger = messenger;
    }

    /**
     * Computes the frames for the execution stack of a thread.
     *
     * @param threadId The ID of the thread.
     */
    public CompletableFuture<List<Frame>> computeFramesForExecutionStack(long threadId) {
        return myMessenger.listFrames(threadId).thenApply(it -> it != null ? it.getFrameList() : List.of());
    }

    /**
     * Computes the children of a given compound value (i.e. list, dictionary, etc.).
     *
     * @param threadId The ID of the thread.
     * @param valueId  The ID of the value whose children values need to be computed.
     */
    public CompletableFuture<List<Value>> computeValueChildren(long threadId, long valueId) {
        return myMessenger.getChildren(threadId, valueId).thenApply(it -> it != null ? it.getChildrenList() : List.of());
    }
}
