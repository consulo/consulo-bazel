package org.jetbrains.bazel.sync.status;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.AwaitValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.sync.ProjectSyncOutcome;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class SyncStatusService {
    // consulo: upstream internal top-level class of this file, used from another package
    public static class SyncAlreadyInProgressException extends IllegalStateException {
    }

    /**
     * The state of the syncs of one project.
     *
     * @param startedSyncCount The number of syncs that started. A sync that started after a given moment has a higher count.
     * @param lastOutcome      How the last sync that finished ended, or null when no sync finished yet. A project that reopens from its
     *                         cache has no sync, so it has no outcome.
     */
    public record SyncStatus(
        long startedSyncCount,
        boolean isInProgress,
        @Nullable ProjectSyncOutcome lastOutcome
    ) {
    }

    private final Project myProject;

    private volatile boolean myIsCanceled = false;

    private final Object mySyncStatusLock = new Object();

    // consulo: kotlinx MutableStateFlow -> MutableObservableValue of the Consulo coroutine library
    private final MutableObservableValue<SyncStatus> myStatus = MutableObservableValue.of(new SyncStatus(0, false, null));

    @Inject
    public SyncStatusService(Project project) {
        myProject = project;
    }

    /**
     * The state of the syncs. A reader that waits for a change listens to this value. It does not need the lock.
     */
    public ObservableValue<SyncStatus> getStatus() {
        return myStatus;
    }

    public boolean isSyncInProgress() {
        return myStatus.get().isInProgress();
    }

    public <T> T withSyncStatus(Function<Boolean, T> action) {
        synchronized (mySyncStatusLock) {
            return action.apply(isSyncInProgress());
        }
    }

    public void startSync() {
        synchronized (mySyncStatusLock) {
            SyncStatus current = myStatus.get();
            if (current.isInProgress()) {
                throw new SyncAlreadyInProgressException();
            }
            myStatus.set(new SyncStatus(current.startedSyncCount() + 1, true, current.lastOutcome()));
            myIsCanceled = false;
        }
        myProject.getMessageBus().syncPublisher(SyncStatusListener.TOPIC).syncStarted();
    }

    public void finishSync(ProjectSyncOutcome outcome) {
        // Read the flag in the same lock as the state change. A waiter can start the next sync as soon as the state changes.
        boolean canceled;
        synchronized (mySyncStatusLock) {
            SyncStatus current = myStatus.get();
            myStatus.set(new SyncStatus(current.startedSyncCount(), false, outcome));
            canceled = myIsCanceled;
        }
        myProject.getMessageBus().syncPublisher(SyncStatusListener.TOPIC).syncFinished(canceled);
    }

    public void cancel() {
        myIsCanceled = true;
    }

    public static SyncStatusService getInstance(Project project) {
        return project.getInstance(SyncStatusService.class);
    }

    public static boolean isSyncInProgress(Project receiver) {
        return SyncStatusService.getInstance(receiver).isSyncInProgress();
    }

    /**
     * Waits until no sync runs, and returns the status at that moment. Returns at once when no sync runs now.
     */
    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    public static CompletableFuture<SyncStatus> awaitSyncFinished(Project receiver) {
        CoroutineScope scope = CoroutineScope.of(receiver.coroutineContext());
        ObservableValue<SyncStatus> status = SyncStatusService.getInstance(receiver).getStatus();
        return Coroutine.first(AwaitValue.<Object, SyncStatus>until(status, it -> !it.isInProgress()))
            .runAsync(scope, null)
            .toFuture();
    }
}
