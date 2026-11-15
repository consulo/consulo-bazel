package org.jetbrains.bazel.debug.platform;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.PausedThread;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XSuspendContext;
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class StarlarkSuspendContext extends XSuspendContext {
    private final StarlarkValueComputer myValueComputer;
    private final StarlarkDebuggerEvaluator.Provider myEvaluatorProvider;

    private final List<StarlarkExecutionStack> myExecutionStacks;
    private final @Nullable StarlarkExecutionStack myActiveExecutionStack;

    // consulo: overload for the upstream default argument (primaryThreadId = null)
    public StarlarkSuspendContext(
        List<PausedThread> threads,
        StarlarkValueComputer valueComputer,
        StarlarkDebuggerEvaluator.Provider evaluatorProvider
    ) {
        this(threads, valueComputer, evaluatorProvider, null);
    }

    public StarlarkSuspendContext(
        List<PausedThread> threads,
        StarlarkValueComputer valueComputer,
        StarlarkDebuggerEvaluator.Provider evaluatorProvider,
        @Nullable Long primaryThreadId
    ) {
        myValueComputer = valueComputer;
        myEvaluatorProvider = evaluatorProvider;
        myExecutionStacks = threads.stream().map(it -> new StarlarkExecutionStack(it, myValueComputer, myEvaluatorProvider)).toList();
        StarlarkExecutionStack activeExecutionStack = null;
        if (primaryThreadId != null) {
            int index = -1;
            for (int i = 0; i < threads.size(); i++) {
                if (threads.get(i).getId() == primaryThreadId) {
                    index = i;
                    break;
                }
            }
            activeExecutionStack = myExecutionStacks.get(index);
        }
        if (activeExecutionStack == null) {
            activeExecutionStack = myExecutionStacks.isEmpty() ? null : myExecutionStacks.get(0);
        }
        myActiveExecutionStack = activeExecutionStack;
    }

    @Override
    public @Nullable XExecutionStack getActiveExecutionStack() {
        return myActiveExecutionStack;
    }

    @Override
    public XExecutionStack[] getExecutionStacks() {
        return myExecutionStacks.toArray(new XExecutionStack[0]);
    }

    @Override
    public void computeExecutionStacks(XExecutionStackContainer container) {
        container.addExecutionStack(myExecutionStacks, true);
    }
}
