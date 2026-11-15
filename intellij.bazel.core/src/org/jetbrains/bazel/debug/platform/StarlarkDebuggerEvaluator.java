package org.jetbrains.bazel.debug.platform;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.EvaluateResponse;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger;
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer;
import org.jspecify.annotations.Nullable;

public class StarlarkDebuggerEvaluator extends XDebuggerEvaluator {
    private final long myThreadId;
    private final StarlarkValueComputer myValueComputer;
    private final StarlarkDebugMessenger myMessenger;

    public StarlarkDebuggerEvaluator(
        long threadId,
        StarlarkValueComputer valueComputer,
        StarlarkDebugMessenger messenger
    ) {
        myThreadId = threadId;
        myValueComputer = valueComputer;
        myMessenger = messenger;
    }

    @Override
    public void evaluate(
        String expression,
        XEvaluationCallback callback,
        @Nullable XSourcePosition sourcePosition
    ) {
        myMessenger
            .evaluate(myThreadId, expression)
            .thenAccept(it -> callBack(it, callback));
    }

    private void callBack(@Nullable EvaluateResponse receiver, XEvaluationCallback callback) {
        if (receiver != null) {
            callback.evaluated(StarlarkValue.fromProto(receiver.getResult(), myThreadId, myValueComputer));
        }
        else {
            callback.errorOccurred(BazelPluginLocalize.starlarkDebugValueObtainFailed());
        }
    }

    /**
     * Always resolves to an error with given message
     */
    public static class ErrorEvaluator extends XDebuggerEvaluator {
        private final LocalizeValue myMessage;

        public ErrorEvaluator(LocalizeValue message) {
            myMessage = message;
        }

        @Override
        public void evaluate(
            String expression,
            XEvaluationCallback callback,
            @Nullable XSourcePosition sourcePosition
        ) {
            callback.errorOccurred(myMessage);
        }
    }

    public static class Provider {
        private final StarlarkValueComputer myValueComputer;
        private final StarlarkDebugMessenger myMessenger;

        public Provider(StarlarkValueComputer valueComputer, StarlarkDebugMessenger messenger) {
            myValueComputer = valueComputer;
            myMessenger = messenger;
        }

        public XDebuggerEvaluator obtain(long threadId) {
            return new StarlarkDebuggerEvaluator(threadId, myValueComputer, myMessenger);
        }
    }
}
