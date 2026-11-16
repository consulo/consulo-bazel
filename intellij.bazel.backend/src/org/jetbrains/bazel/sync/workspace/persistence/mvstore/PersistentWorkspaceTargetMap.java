package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTargetMap;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStorage.SnapshotGeneration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

final class PersistentWorkspaceTargetMap implements WorkspaceTargetMap {
    private final PersistentWorkspaceSnapshot myPartialSnapshot;
    private final SnapshotGeneration myGeneration;

    PersistentWorkspaceTargetMap(
        PersistentWorkspaceSnapshot partialSnapshot,
        SnapshotGeneration generation
    ) {
        myPartialSnapshot = partialSnapshot;
        myGeneration = generation;
    }

    PersistentWorkspaceSnapshot getPartialSnapshot() {
        return myPartialSnapshot;
    }

    SnapshotGeneration getGeneration() {
        return myGeneration;
    }

    @Override
    public @Nullable BuildTargetInfo findTargetInfoByKey(WorkspaceTargetKey key) {
        return myGeneration.findTargetInfo(myPartialSnapshot, key);
    }

    @Override
    public Stream<BuildTargetInfo> allTargetInfos() {
        return myGeneration.scanAllTargetInfos(myPartialSnapshot);
    }

    @Override
    public @Nullable BuildTarget findTargetByKey(WorkspaceTargetKey key) {
        return myGeneration.findOrLoadTarget(myPartialSnapshot, key);
    }

    @Override
    public Stream<BuildTarget> allTargets() {
        return myGeneration.scanAllTargets(myPartialSnapshot);
    }

    @Override
    public <T extends BuildTargetData> @Nullable T findTargetData(WorkspaceTargetKey key, Class<T> type) {
        return myGeneration.findTargetData(myPartialSnapshot, key, type);
    }
}
