package org.jetbrains.bazel.progress;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.bazel.commons.localize.BazelExtLocalize;
import consulo.build.ui.event.BuildEventFactory;
import consulo.build.ui.event.EventResult;
import consulo.build.ui.event.MessageEvent;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.ControlFlowException;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class util {
    private util() {
    }

    private static final Logger log = Logger.getInstance(TaskConsole.class);

    public static TaskConsole getSyncConsole(Project receiver) {
        return receiver.getInstance(ConsoleService.class).getSyncConsole();
    }

    /**
     * Runs {@code action}, which calls {@link #withSubtask}, so that its failure does not fail the parent task with {@code taskId}.
     * The failure stops here, so this function logs it.
     * It also reports the failure as a diagnostic message of the parent task. The console skips it when a subtask already shows it.
     * It rethrows a control-flow exception and a cancellation.
     */
    public static <T> CompletableFuture<@Nullable T> isolateSubtaskFailure(
        TaskConsole receiver,
        TaskId taskId,
        Supplier<CompletableFuture<T>> action
    ) {
        CompletableFuture<T> actionResult;
        try {
            actionResult = action.get();
        }
        catch (Throwable ex) {
            actionResult = CompletableFuture.failedFuture(ex);
        }
        // consulo: the try/catch of upstream runs when the future completes
        return actionResult.<@Nullable T>thenApply(value -> value).exceptionally(error -> {
            Throwable e = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            // consulo: IntelliJ rethrowControlFlowException(e)
            if (e instanceof ControlFlowException || e instanceof java.util.concurrent.CancellationException) {
                throw error instanceof CompletionException completionException ? completionException : new CompletionException(e);
            }
            String message = e.getMessage();
            receiver.addDiagnosticMessage(
                taskId,
                null,
                -1,
                -1,
                message != null ? LocalizeValue.of(message) : IdeLocalize.unknownError(),
                null,
                MessageEvent.Kind.ERROR,
                e
            );
            log.error(e);
            return null;
        });
    }

    /**
     * Runs {@code block} as a subtask with {@code subtaskId}. On a failure, it shows the failure and rethrows the original exception.
     * The console shows each exception one time, so the enclosing subtasks only show that they failed.
     * A failed subtask fails its parent task. To continue after a failure, use {@link #isolateSubtaskFailure}.
     */
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
            if (ex instanceof java.util.concurrent.CancellationException) {
                result = buildEventFactory.newFailure()
                    .message(BazelExtLocalize.consoleTaskExceptionCancellationMessage())
                    .error(ex)
                    .createResult();
            }
            else if (ex instanceof ControlFlowException) {
                result = buildEventFactory.newFailure().createResult();
            }
            else if (ex instanceof TimeoutException) {
                result = buildEventFactory.newFailure()
                    .message(BazelExtLocalize.consoleTaskExceptionTimeoutMessage())
                    .error(ex)
                    .createResult();
            }
            else {
                result = buildEventFactory.createFailureResult(ex);
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
