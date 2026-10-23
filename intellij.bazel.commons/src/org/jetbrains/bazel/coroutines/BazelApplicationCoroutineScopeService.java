package org.jetbrains.bazel.coroutines;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Application-level coroutine scope service for operations that must survive project closure.
 * Use this when you need to close a project and then open another one.
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class BazelApplicationCoroutineScopeService {
    private static final Logger LOG = Logger.getInstance(BazelApplicationCoroutineScopeService.class);

    @Inject
    public BazelApplicationCoroutineScopeService() {
    }

    public CompletableFuture<?> launch(Supplier<? extends CompletableFuture<?>> block) {
        // consulo: upstream uses CoroutineStart.UNDISPATCHED, so the block starts in the calling thread (up to its first
        // asynchronous step). The returned future is not bound to any project, like the application coroutine scope.
        CompletableFuture<?> job;
        try {
            job = block.get();
        }
        catch (Throwable e) {
            job = CompletableFuture.failedFuture(e);
        }
        // consulo: the application coroutine scope reports failures of launched coroutines
        job.whenComplete((result, error) -> {
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (cause != null && !(cause instanceof CancellationException)) {
                LOG.error(cause);
            }
        });
        return job;
    }

    public static BazelApplicationCoroutineScopeService getInstance() {
        return Application.get().getInstance(BazelApplicationCoroutineScopeService.class);
    }
}
