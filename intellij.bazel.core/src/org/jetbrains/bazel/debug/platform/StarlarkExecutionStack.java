package org.jetbrains.bazel.debug.platform;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Frame;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.PausedThread;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class StarlarkExecutionStack extends XExecutionStack {
    private final PausedThread myThread;
    private final StarlarkValueComputer myValueComputer;
    private final StarlarkDebuggerEvaluator.Provider myEvaluatorProvider;

    private @Nullable XStackFrame myTopFrame = null;

    public StarlarkExecutionStack(
        PausedThread thread,
        StarlarkValueComputer valueComputer,
        StarlarkDebuggerEvaluator.Provider evaluatorProvider
    ) {
        super(thread.getId() + ": " + thread.getName());
        myThread = thread;
        myValueComputer = valueComputer;
        myEvaluatorProvider = evaluatorProvider;
    }

    @Override
    public @Nullable XStackFrame getTopFrame() {
        return myTopFrame;
    }

    // consulo: XExecutionStack.computeStackFrames has no startIndex parameter (index of the frame to start from,
    //   0 = the topmost frame); Consulo always computes from the topmost frame
    @Override
    public void computeStackFrames(XStackFrameContainer container) {
        // we don't want to react to any following computeStackFrames calls, because we compute all frames at once
        myValueComputer.computeFramesForExecutionStack(myThread.getId()).thenAccept(frames -> {
            List<StarlarkStackFrame> xStackFrames = frames.stream().map(this::toStackFrame).toList();
            StarlarkStackFrame first = xStackFrames.isEmpty() ? null : xStackFrames.get(0);
            if (first != null) {
                first.setTopFrame(true);
            }
            myTopFrame = first;
            container.addStackFrames(xStackFrames, true);
        });
    }

    private StarlarkStackFrame toStackFrame(Frame receiver) {
        return new StarlarkStackFrame(receiver, myThread.getId(), myValueComputer, myEvaluatorProvider);
    }
}
