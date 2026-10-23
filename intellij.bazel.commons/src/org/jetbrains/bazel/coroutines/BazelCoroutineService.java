package org.jetbrains.bazel.coroutines;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelCoroutineService {
    private final Project myProject;

    @Inject
    public BazelCoroutineService(Project project) {
        myProject = project;
    }

    public CompletableFuture<?> start(Supplier<? extends CompletableFuture<?>> block) {
        Deferred<@Nullable Object> job =
            new Deferred<>(() -> launchInProjectScope(() -> block.get().thenApply(result -> (@Nullable Object) result)));
        job.start();
        return job;
    }

    public <T extends @Nullable Object> Deferred<T> startAsync(Supplier<? extends CompletableFuture<T>> callable) {
        return startAsync(false, callable);
    }

    public <T extends @Nullable Object> Deferred<T> startAsync(boolean lazy, Supplier<? extends CompletableFuture<T>> callable) {
        Deferred<T> deferred = new Deferred<>(() -> launchInProjectScope(callable));
        if (!lazy) {
            deferred.start();
        }
        return deferred;
    }

    // consulo: replaces the service coroutine scope - the block runs as a coroutine in the project coroutine context
    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    private <T extends @Nullable Object> Continuation<T> launchInProjectScope(Supplier<? extends CompletableFuture<T>> callable) {
        CoroutineScope scope = CoroutineScope.of(myProject.coroutineContext());
        return Coroutine.first(CompletableFutureStep.<Object, T>await(ignored -> callable.get())).runAsync(scope, null);
    }

    public static BazelCoroutineService getInstance(Project project) {
        return project.getInstance(BazelCoroutineService.class);
    }

    /**
     * consulo: Java counterpart of {@code kotlinx.coroutines.Deferred} (and {@code Job}) returned by {@link #startAsync} and
     * {@link #start}.
     * <p>
     * A lazy deferred ({@code startAsync(true, ...)}) does not run until {@link #start()}, {@link #await()}, {@link #join()}
     * or {@link #get()} is called, like {@code CoroutineStart.LAZY}. Registering dependent stages ({@code thenApply},
     * {@code whenComplete}, ...) does not start it, like {@code invokeOnCompletion}.
     * {@link #cancel(boolean)} also cancels the running coroutine.
     */
    public static final class Deferred<T extends @Nullable Object> extends CompletableFuture<T> {
        private final Supplier<Continuation<T>> myStarter;
        private final AtomicBoolean myStarted = new AtomicBoolean();
        private volatile @Nullable Continuation<T> myContinuation;

        private Deferred(Supplier<Continuation<T>> starter) {
            myStarter = starter;
        }

        /**
         * Starts the computation if it was not started yet, like {@code Job.start()}.
         *
         * @return true if this call started the computation
         */
        public boolean start() {
            if (isDone() || !myStarted.compareAndSet(false, true)) {
                return false;
            }
            Continuation<T> continuation = myStarter.get();
            myContinuation = continuation;
            continuation.toFuture().whenComplete((result, error) -> {
                if (error != null) {
                    completeExceptionally(error);
                }
                else {
                    complete(result);
                }
            });
            if (isCancelled()) {
                continuation.cancel();
            }
            return true;
        }

        /**
         * Starts the computation (if lazy) and returns this future, like {@code Deferred.await()}.
         */
        public CompletableFuture<T> await() {
            start();
            return this;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            start();
            return super.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            start();
            return super.get(timeout, unit);
        }

        @Override
        public T join() {
            start();
            return super.join();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            Continuation<T> continuation = myContinuation;
            if (continuation != null) {
                continuation.cancel();
            }
            return cancelled;
        }
    }
}
