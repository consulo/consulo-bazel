package org.jetbrains.bazel.run.task;

import consulo.process.ProcessOutputType;
import consulo.process.util.AnsiEscapeDecoder;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

public class BazelRunTaskListener implements BazelTaskListener {
    private final BazelProcessHandler myHandler;
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    public BazelRunTaskListener(BazelProcessHandler handler) {
        myHandler = handler;
    }

    protected BazelProcessHandler getHandler() {
        return myHandler;
    }

    protected AnsiEscapeDecoder getAnsiEscapeDecoder() {
        return myAnsiEscapeDecoder;
    }

    @Override
    public void onOutputStream(@Nullable TaskId taskId, String text) {
        myAnsiEscapeDecoder.escapeText(text, ProcessOutputType.STDOUT, (s, key) -> {
            myHandler.notifyTextAvailable(s, key);
        });
    }

    @Override
    public void onErrorStream(@Nullable TaskId taskId, String text) {
        myAnsiEscapeDecoder.escapeText(text, ProcessOutputType.STDERR, (s, key) -> {
            myHandler.notifyTextAvailable(s, key);
        });
    }

    // For compatibility with older BSP servers and JB test runner
    // TODO: Log messages in the correct place
    @Override
    public void onLogMessage(TaskId taskId, String message) {
        String messageWithNewline = message.endsWith("\n") ? message : message + "\n";
        myAnsiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT, (s, key) -> {
            myHandler.notifyTextAvailable(s, key);
        });
    }
}
