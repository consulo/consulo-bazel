package org.jetbrains.bazel.progress;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.bazel.commons.localize.BazelExtLocalize;
import consulo.build.ui.event.BuildEventFactory;
import consulo.build.ui.event.EventResult;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bsp.protocol.TaskId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class util {
    private util() {
    }

    public static TaskConsole getSyncConsole(Project receiver) {
        return receiver.getInstance(ConsoleService.class).getSyncConsole();
    }

    public static <T> CompletableFuture<T> withSubtask(
        TaskConsole receiver,
        TaskId subtaskId,
        LocalizeValue message,
        Function<TaskId, CompletableFuture<T>> block
    ) {
        receiver.startSubtask(subtaskId, message);
        CompletableFuture<T> blockResult;
        try {
            blockResult = block.apply(subtaskId);
        }
        catch (Throwable ex) {
            blockResult = CompletableFuture.failedFuture(ex);
        }
        // consulo: the try/catch of upstream runs when the future completes; the failure is propagated as is
        return blockResult.whenComplete((value, error) -> {
            if (error == null) {
                receiver.finishSubtask(subtaskId);
                return;
            }
            Throwable ex = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            BuildEventFactory buildEventFactory = Application.get().getInstance(BuildEventFactory.class);
            EventResult result;
            if (receiver.registerException(subtaskId, ex)) {
                if (ex instanceof java.util.concurrent.CancellationException) {
                    result = buildEventFactory.newFailure()
                        .message(BazelExtLocalize.consoleTaskExceptionCancellationMessage())
                        .createResult();
                }
                else if (ex instanceof TimeoutException) {
                    result = buildEventFactory.newFailure()
                        .message(BazelExtLocalize.consoleTaskExceptionTimeoutMessage())
                        .createResult();
                }
                else {
                    result = buildEventFactory.createFailureResult(ex);
                }
            }
            else {
                result = buildEventFactory.newFailure().createResult();
            }
            receiver.finishSubtask(subtaskId, null, result);
        });
    }

    public static <T> CompletableFuture<T> withSubtask(
        TaskConsole receiver,
        ProgressIndicator reporter,
        TaskId subtaskId,
        LocalizeValue text,
        Function<TaskId, CompletableFuture<T>> block
    ) {
        // consulo: upstream runs the block in SequentialProgressReporter.indeterminateStep(text)
        reporter.setIndeterminate(true);
        reporter.setText(text);
        return withSubtask(
            receiver,
            subtaskId,
            text,
            block
        );
    }
}
