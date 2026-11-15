package org.jetbrains.bazel.workspace.fileEvents;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * consulo: upstream reports through a kotlin SequentialProgressReporter, here the steps move the fraction of a ProgressIndicator
 */
public class BazelFileEventProgressReporter {
    private final ProgressIndicator myOriginalReporter;
    // consulo: the work done by the finished steps (SequentialProgressReporter keeps it itself)
    private int myDone = 0;

    public BazelFileEventProgressReporter(ProgressIndicator originalReporter) {
        myOriginalReporter = originalReporter;
    }

    private enum Step {
        QUERY(40),
        MODIFY(40),
        FINALIZE(20);

        private final int mySize;

        Step(int size) {
            mySize = size;
        }

        int getSize() {
            return mySize;
        }
    }

    public <T> CompletableFuture<T> queryStep(Supplier<CompletableFuture<T>> action) {
        return step(
            /* step = */ Step.QUERY,
            /* text = */ BazelPluginLocalize.fileChangeProcessingStepQuery(),
            /* action = */ action
        );
    }

    public <T> CompletableFuture<T> updateModelStep(Supplier<CompletableFuture<T>> action) {
        return step(
            /* step = */ Step.MODIFY,
            /* text = */ BazelPluginLocalize.fileChangeProcessingStepUpdate(),
            /* action = */ action
        );
    }

    public <T> CompletableFuture<T> finalisingStep(Supplier<CompletableFuture<T>> action) {
        return step(
            /* step = */ Step.FINALIZE,
            /* text = */ BazelPluginLocalize.fileChangeProcessingStepCommit(),
            /* action = */ action
        );
    }

    public void message(String message) {
        // consulo: upstream originalReporter.sizedStep(0, text = message)
        myOriginalReporter.setText(LocalizeValue.of(message));
    }

    private <T> CompletableFuture<T> step(Step step, LocalizeValue text, Supplier<CompletableFuture<T>> action) {
        // consulo: upstream originalReporter.sizedStep(workSize = 0, text = text, action = action)
        myOriginalReporter.setText(text);
        CompletableFuture<T> result;
        try {
            result = action.get();
        }
        catch (Throwable e) {
            result = CompletableFuture.failedFuture(e);
        }
        return result.thenApply(it -> {
            // move progress
            myDone += step.getSize();
            myOriginalReporter.setFraction((double) myDone / totalSize());
            return it;
        });
    }

    private static int totalSize() {
        int result = 0;
        for (Step it : Step.values()) {
            result += it.getSize();
        }
        return result;
    }

    public static CompletableFuture<Void> runWithProgressBar(
        Project project,
        Function<BazelFileEventProgressReporter, CompletableFuture<?>> actions
    ) {
        // consulo: withBackgroundProgress + reportSequentialProgress -> Task.Backgroundable.queue with a progress indicator
        CompletableFuture<Void> result = new CompletableFuture<>();
        Task.Backgroundable.queue(project, BazelPluginLocalize.fileChangeProcessingTitle(), true, originalReporter -> {
            // consulo: cancelling the returned future (the coroutine job upstream) cancels the background progress
            result.whenComplete((ignored, error) -> {
                if (result.isCancelled()) {
                    originalReporter.cancel();
                }
            });
            try {
                originalReporter.setIndeterminate(false);
                actions.apply(new BazelFileEventProgressReporter(originalReporter)).join();
                result.complete(null);
            }
            catch (Throwable e) {
                result.completeExceptionally(unwrap(e));
            }
        });
        return result;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable result = throwable;
        while ((result instanceof CompletionException || result instanceof ExecutionException) && result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
