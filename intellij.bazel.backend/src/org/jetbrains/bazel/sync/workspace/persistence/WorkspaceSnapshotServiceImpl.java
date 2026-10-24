package org.jetbrains.bazel.sync.workspace.persistence;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.SettingsSavingComponent;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.Mutex;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.lang.Pair;
import consulo.util.lang.lazy.LazyValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.jetbrains.bazel.config.project.isBazelProject;

@ServiceImpl
@Singleton
public class WorkspaceSnapshotServiceImpl implements WorkspaceSnapshotService, SettingsSavingComponent, Disposable {
    private final Project myProject;

    private final LazyValue<WorkspaceSnapshotService> myDelegate;

    private boolean myIsEphemeral = false;

    private boolean myForcePersistent = false;

    // consulo: the upstream service coroutine scope is the project coroutine context
    @Inject
    public WorkspaceSnapshotServiceImpl(Project project) {
        myProject = project;
        myDelegate = LazyValue.atomicNotNull(() -> {
            if ((myIsEphemeral || !isBazelProject(myProject)) && !myForcePersistent) {
                return new InMemoryWorkspaceSnapshotService();
            }
            else {
                return new PersistentWorkspaceSnapshotService(myProject);
            }
        });
    }

    public boolean isEphemeral() {
        return myIsEphemeral;
    }

    public void setEphemeral(boolean ephemeral) {
        myIsEphemeral = ephemeral;
    }

    @TestOnly
    public boolean getForcePersistent() {
        return myForcePersistent;
    }

    @TestOnly
    public void setForcePersistent(boolean forcePersistent) {
        myForcePersistent = forcePersistent;
    }

    @Override
    public ObservableValue<WorkspaceSnapshot> getSnapshot() {
        return myDelegate.get().getSnapshot();
    }

    @Override
    public CompletableFuture<WorkspaceSnapshot> currentSnapshot() {
        return myDelegate.get().currentSnapshot();
    }

    @Override
    public <R extends @Nullable Object> CompletableFuture<Pair<WorkspaceSnapshot, R>> update(WorkspaceSnapshotUpdater<R> updater) {
        return myDelegate.get().update(updater);
    }

    // consulo: SettingsSavingComponent.save() is synchronous in Consulo
    @Override
    public void save() {
        if (myDelegate.get() instanceof SettingsSavingComponent component) {
            component.save();
        }
    }

    @Override
    public void dispose() {
        if (myDelegate.get() instanceof Disposable disposable) {
            Disposer.dispose(disposable);
        }
    }
}

class InMemoryWorkspaceSnapshotService implements WorkspaceSnapshotService {
    private final Mutex myUpdateLock = new Mutex();

    private final MutableObservableValue<WorkspaceSnapshot> mySnapshot = MutableObservableValue.of(WorkspaceSnapshot.EMPTY);

    @Override
    public ObservableValue<WorkspaceSnapshot> getSnapshot() {
        return mySnapshot;
    }

    @Override
    public CompletableFuture<WorkspaceSnapshot> currentSnapshot() {
        return CompletableFuture.completedFuture(mySnapshot.get());
    }

    @Override
    public <R extends @Nullable Object> CompletableFuture<Pair<WorkspaceSnapshot, R>> update(WorkspaceSnapshotUpdater<R> updater) {
        // not using atomic `update { ... }` to ensure single invocation of updater
        return withLock(myUpdateLock, () -> {
            WorkspaceSnapshot current = mySnapshot.get();
            return updater.update(current).thenApply(pair -> {
                WorkspaceSnapshot newSnapshot = pair.getFirst();
                R result = pair.getSecond();
                mySnapshot.set(newSnapshot);
                return Pair.create(newSnapshot, result);
            });
        });
    }

    // consulo: kotlinx Mutex.withLock { } over a Consulo coroutine Mutex
    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    private static <T extends @Nullable Object> CompletableFuture<T> withLock(Mutex mutex, Supplier<CompletableFuture<T>> action) {
        Object owner = new Object();
        CoroutineScope scope = CoroutineScope.of(Application.get().coroutineContext());
        return Coroutine.first(mutex.<@Nullable Object>lock(owner))
            .then(CompletableFutureStep.<@Nullable Object, T>await(ignored -> action.get()))
            .runAsync(scope, null)
            .toFuture()
            .whenComplete((result, error) -> {
                if (mutex.holdsLock(owner)) {
                    mutex.unlock(owner);
                }
            });
    }
}
