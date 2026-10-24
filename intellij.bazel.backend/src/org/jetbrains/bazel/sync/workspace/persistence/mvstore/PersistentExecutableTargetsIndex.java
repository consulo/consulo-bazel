package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStorage.SnapshotGeneration;
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndex;

import java.util.List;

final class PersistentExecutableTargetsIndex implements ExecutableTargetsIndex {
    private final PersistentWorkspaceSnapshot myPartialSnapshot;
    private final SnapshotGeneration myGeneration;

    PersistentExecutableTargetsIndex(
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
    public List<Label> executableTargetsFor(Label label) {
        return myGeneration.findExecutableTargets(myPartialSnapshot, label);
    }
}
