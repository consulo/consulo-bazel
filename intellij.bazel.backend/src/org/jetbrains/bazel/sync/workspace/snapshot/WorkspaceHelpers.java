package org.jetbrains.bazel.sync.workspace.snapshot;

import consulo.project.Project;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.CommonWorkspaceSyncConfig;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jspecify.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

// consulo: upstream Kotlin top-level (extension) functions and properties
public final class WorkspaceHelpers {
    private WorkspaceHelpers() {
    }

    // consulo: upstream is `inline fun <reified T>`, the type is passed explicitly
    public static <T extends BuildTargetData> @Nullable T findBuildData(BuildTarget receiver, Class<T> type) {
        return receiver.getData().stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
    }

    public static <T extends BuildTargetData> boolean hasBuildData(BuildTarget receiver, Class<T> type) {
        return findBuildData(receiver, type) != null;
    }

    public static <T extends BuildTargetData> Stream<Pair<BuildTarget, T>> filterBuildTarget(Stream<BuildTarget> receiver, Class<T> type) {
        return receiver.map(target -> {
            T data = findBuildData(target, type);
            return data != null ? Pair.create(target, data) : null;
        }).filter(Objects::nonNull);
    }

    public static Stream<BuildTarget> getAllTargets(WorkspaceSnapshot receiver) {
        return receiver.targetGraph().getAllTargets()
            .map(it -> receiver.targets().findTargetByKey(it, TargetLoadOptions.ALL))
            .filter(Objects::nonNull);
    }

    public static CommonWorkspaceSyncConfig getCommonSyncConfig(WorkspaceSnapshot receiver) {
        return receiver.syncConfigs().stream()
            .filter(CommonWorkspaceSyncConfig.class::isInstance)
            .map(CommonWorkspaceSyncConfig.class::cast)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Collection contains no element matching the predicate."));
    }

    public static @Nullable WorkspaceSnapshot getWorkspaceSnapshotOrNull(Project receiver) {
        WorkspaceSnapshot snapshot = receiver.getInstance(WorkspaceSnapshotService.class).getSnapshot().get();
        return snapshot != WorkspaceSnapshot.EMPTY ? snapshot : null;
    }

    /**
     * Await initial workspace snapshot load, return {@code null} when snapshot is {@link WorkspaceSnapshot#EMPTY}`
     */
    public static CompletableFuture<@Nullable WorkspaceSnapshot> currentWorkspaceSnapshotOrNull(Project receiver) {
        return receiver.getInstance(WorkspaceSnapshotService.class).currentSnapshot()
            .thenApply(snapshot -> snapshot != WorkspaceSnapshot.EMPTY ? snapshot : null);
    }
}
