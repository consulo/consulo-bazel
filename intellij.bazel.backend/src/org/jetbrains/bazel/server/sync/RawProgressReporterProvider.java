package org.jetbrains.bazel.server.sync;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// consulo: RawProgressReporter is mapped to ProgressIndicator
public interface RawProgressReporterProvider {
    <T> CompletableFuture<T> invoke(Function<ProgressIndicator, CompletableFuture<T>> block);

    static RawProgressReporterProvider current() {
        return new RawProgressReporterProvider() {
            @Override
            public <T> CompletableFuture<T> invoke(Function<ProgressIndicator, CompletableFuture<T>> block) {
                // consulo: upstream reportRawProgress { } reports to the progress of the current coroutine context;
                // the progress indicator of the calling thread is used, or an empty one when there is none
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                return block.apply(indicator != null ? indicator : new EmptyProgressIndicator());
            }
        };
    }
}
