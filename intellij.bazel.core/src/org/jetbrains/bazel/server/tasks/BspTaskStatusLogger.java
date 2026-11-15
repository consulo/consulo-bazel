package org.jetbrains.bazel.server.tasks;

import consulo.application.Application;
import consulo.bazel.commons.localize.BazelExtLocalize;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.build.ui.event.BuildEventFactory;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class BspTaskStatusLogger {
    private final CompletableFuture<BazelStatus> myTaskDeferred;
    private final TaskConsole myBspBuildConsole;
    private final TaskId myTaskId;

    BspTaskStatusLogger(
        CompletableFuture<BazelStatus> taskDeferred,
        TaskConsole bspBuildConsole,
        TaskId taskId
    ) {
        myTaskDeferred = taskDeferred;
        myBspBuildConsole = bspBuildConsole;
        myTaskId = taskId;
    }

    CompletableFuture<BazelStatus> getStatus() {
        // consulo: upstream invokeOnCompletion { } + await()
        myTaskDeferred.whenComplete((result, throwable) -> catchBuildErrors(unwrap(throwable)));
        return myTaskDeferred.thenApply(it -> {
            finishBuildConsoleTaskWithProperResult(it, myBspBuildConsole);
            return it;
        });
    }

    private void finishBuildConsoleTaskWithProperResult(
        BazelStatus status,
        TaskConsole bspBuildConsole
    ) {
        if (status == BazelStatus.SUCCESS) {
            bspBuildConsole.finishTask(myTaskId, BazelPluginLocalize.consoleTaskStatusOk());
        }
        else if (status == BazelStatus.CANCEL) {
            bspBuildConsole.finishTask(myTaskId, BazelPluginLocalize.consoleTaskStatusCancelled());
        }
        else {
            bspBuildConsole.finishTask(
                myTaskId,
                BazelPluginLocalize.consoleTaskStatusError(),
                buildEventFactory().newFailure().createResult()
            );
        }
    }

    private void catchBuildErrors(@Nullable Throwable exception) {
        if (exception == null) {
            return;
        }
        if (exception instanceof CancellationException) {
            myBspBuildConsole.finishTask(
                myTaskId,
                BazelPluginLocalize.consoleTaskExceptionCancellation(),
                buildEventFactory().newFailure().message(BazelExtLocalize.consoleTaskExceptionCancellationMessage()).createResult()
            );
        }
        else if (exception instanceof TimeoutException) {
            myBspBuildConsole.finishTask(
                myTaskId,
                BazelPluginLocalize.consoleTaskExceptionTimedOut(),
                buildEventFactory().newFailure().message(BazelExtLocalize.consoleTaskExceptionTimeoutMessage()).createResult()
            );
        }
        else {
            myBspBuildConsole.finishTask(
                myTaskId,
                BazelPluginLocalize.consoleTaskExceptionOther(),
                buildEventFactory().newFailure().error(exception).createResult()
            );
        }
    }

    // consulo: upstream FailureResultImpl(...)
    private static BuildEventFactory buildEventFactory() {
        return Application.get().getInstance(BuildEventFactory.class);
    }

    // consulo: futures wrap the failure of a dependent stage
    private static @Nullable Throwable unwrap(@Nullable Throwable throwable) {
        Throwable result = throwable;
        while ((result instanceof CompletionException || result instanceof ExecutionException) && result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
