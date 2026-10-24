package org.jetbrains.bazel.sync.workspace.persistence;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@ServiceAPI(ComponentScope.PROJECT)
public interface WorkspaceSnapshotService {
    @FunctionalInterface
    interface WorkspaceSnapshotUpdater<R extends @Nullable Object> {
        CompletableFuture<Pair<WorkspaceSnapshot, R>> update(WorkspaceSnapshot snapshot);
    }

    // consulo: StateFlow replaced by ObservableValue
    ObservableValue<WorkspaceSnapshot> getSnapshot();

    /**
     * Awaits the initial disk load, then returns the current snapshot.
     * {@link WorkspaceSnapshot#EMPTY} means no snapshot exists yet.
     */
    CompletableFuture<WorkspaceSnapshot> currentSnapshot();

    /**
     * Update current snapshot atomically
     */
    <R extends @Nullable Object> CompletableFuture<Pair<WorkspaceSnapshot, R>> update(WorkspaceSnapshotUpdater<R> updater);
}
