package org.jetbrains.bazel.debug.console;

import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.process.ProcessOutputType;
import consulo.process.util.AnsiEscapeDecoder;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class StarlarkDebugTaskListener implements BazelTaskListener {
    private final ConsoleView myConsole;
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    /**
     * Function for checking if debug execution is currently suspended
     */
    private @Nullable BooleanSupplier myDebugPausedChecker = null;

    public StarlarkDebugTaskListener(ConsoleView console) {
        myConsole = console;
    }

    public void setDebugPausedChecker(BooleanSupplier newDebugPausedChecker) {
        myDebugPausedChecker = newDebugPausedChecker;
    }

    @Override
    public void onLogMessage(TaskId taskId, String message) {
        // bazelbsp server erroneously sends a redundant log message in a loop when the debugging session is suspended
        BooleanSupplier debugPausedChecker = myDebugPausedChecker;
        if (debugPausedChecker == null || !debugPausedChecker.getAsBoolean()) {
            printMessage(message);
        }
    }

    private void printMessage(String message) {
        myAnsiEscapeDecoder.escapeText(message, ProcessOutputType.STDOUT, (text, key) ->
            myConsole.print(text, ConsoleViewContentType.getConsoleViewType(key))
        );
        myConsole.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }
}
