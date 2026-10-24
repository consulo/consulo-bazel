package org.jetbrains.bazel.sync.status;

import consulo.project.Project;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.AwaitValue;
import org.jetbrains.bazel.sync.ProjectSyncOutcome;
import org.jetbrains.bazel.sync.ProjectSyncOutcome.ProjectSyncCompletionResult;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.sync.status.SyncStatusService.SyncStatus;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class SyncOutcomeChecks {
    private SyncOutcomeChecks() {
    }

    /**
     * Waits for the sync that runs now, or for a sync that starts in {@code startWindow}. Then waits until no sync runs, and fails
     * when the last sync did not end in a success. The last sync can also be one that ended before this call.
     * <p>
     * A project that reopens from its cache runs no sync, so it has no outcome. The call then passes after {@code startWindow}.
     */
    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    public static CompletableFuture<Void> awaitSyncAndCheckOutcome(Project receiver, Duration startWindow) {
        ObservableValue<SyncStatus> status = receiver.getInstance(SyncStatusService.class).getStatus();
        long startedSyncCount = status.get().startedSyncCount();

        // The sync can start before this call. Then it is already in progress, and the wait ends at once.
        // Otherwise the sync can start later, so the wait has a window. A sync that started and finished in the window also counts.
        CoroutineScope scope = CoroutineScope.of(receiver.coroutineContext());
        Continuation<SyncStatus> startWait = Coroutine
            .first(AwaitValue.<Object, SyncStatus>until(status, it -> it.isInProgress() || it.startedSyncCount() > startedSyncCount))
            .runAsync(scope, null);
        // consulo: withTimeoutOrNull(startWindow) -> the wait ends with no failure after the window, the waiting coroutine is cancelled
        return startWait.toFuture()
            .thenApply(it -> true)
            .completeOnTimeout(false, startWindow.toMillis(), TimeUnit.MILLISECONDS)
            .whenComplete((ignored, error) -> startWait.cancel())
            .thenCompose(ignored -> SyncStatusService.awaitSyncFinished(receiver))
            .thenAccept(finished -> {
                // A sync that failed before this call leaves its outcome in the status, so this check sees it too.
                ProjectSyncOutcome lastOutcome = finished.lastOutcome();
                if (lastOutcome != null) {
                    lastOutcome.checkSuccessful();
                }
            });
    }

    /**
     * Waits until no sync runs, runs a sync with {@code scope}, and fails when this sync did not end in a success.
     * <p>
     * Another sync can start between the wait and the call. Then the call is skipped, so this function waits again and retries.
     * A skip with no new sync has another cause, for example an untrusted project. Then this function fails.
     */
    public static CompletableFuture<Void> syncAndCheckOutcome(Project receiver, ProjectSyncScope scope) {
        SyncStatusService statusService = receiver.getInstance(SyncStatusService.class);
        ProjectSyncService syncService = receiver.getInstance(ProjectSyncService.class);
        return syncAndCheckOutcomeAttempt(receiver, scope, statusService, syncService);
    }

    // consulo: one iteration of the upstream while (true) loop, the next iteration is chained when the sync was skipped
    private static CompletableFuture<Void> syncAndCheckOutcomeAttempt(
        Project receiver,
        ProjectSyncScope scope,
        SyncStatusService statusService,
        ProjectSyncService syncService
    ) {
        return SyncStatusService.awaitSyncFinished(receiver).thenCompose(finished -> {
            long startedSyncCount = finished.startedSyncCount();
            return syncService.sync(scope).thenCompose(outcome -> {
                boolean anotherSyncStarted = statusService.getStatus().get().startedSyncCount() > startedSyncCount;
                if (outcome.completionResult() != ProjectSyncCompletionResult.SKIPPED || !anotherSyncStarted) {
                    outcome.checkSuccessful();
                    return CompletableFuture.completedFuture(null);
                }
                return syncAndCheckOutcomeAttempt(receiver, scope, statusService, syncService);
            });
        });
    }
}
