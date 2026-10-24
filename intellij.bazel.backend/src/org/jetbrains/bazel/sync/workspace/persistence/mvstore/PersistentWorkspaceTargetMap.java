package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTargetMap;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStorage.SnapshotGeneration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
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
    public @Nullable BuildTarget findTargetByKey(
        WorkspaceTargetKey key,
        TargetLoadOptions options
    ) {
        return myGeneration.findOrLoadTarget(myPartialSnapshot, key, options);
    }

    @Override
    public Stream<BuildTarget> allTargets(TargetLoadOptions options) {
        return myGeneration.scanAllTargets(myPartialSnapshot, options);
    }
}
