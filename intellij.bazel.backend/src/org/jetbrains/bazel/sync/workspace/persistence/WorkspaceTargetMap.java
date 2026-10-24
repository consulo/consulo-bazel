package org.jetbrains.bazel.sync.workspace.persistence;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

public interface WorkspaceTargetMap {
    @Nullable BuildTarget findTargetByKey(WorkspaceTargetKey key, TargetLoadOptions options);

    default @Nullable BuildTarget findTargetByKey(WorkspaceTargetKey key) {
        return findTargetByKey(key, TargetLoadOptions.ALL);
    }

    Stream<BuildTarget> allTargets(TargetLoadOptions options);

    default Stream<BuildTarget> allTargets() {
        return allTargets(TargetLoadOptions.ALL);
    }

    WorkspaceTargetMap EMPTY = new WorkspaceTargetMap() {
        @Override
        public @Nullable BuildTarget findTargetByKey(
            WorkspaceTargetKey key,
            TargetLoadOptions options
        ) {
            return null;
        }

        @Override
        public Stream<BuildTarget> allTargets(TargetLoadOptions options) {
            return Stream.of();
        }
    };

    class InMemoryWorkspaceTargetMap implements WorkspaceTargetMap {
        private final Map<WorkspaceTargetKey, BuildTarget> myTargets;

        public InMemoryWorkspaceTargetMap(Map<WorkspaceTargetKey, BuildTarget> targets) {
            myTargets = targets;
        }

        public Map<WorkspaceTargetKey, BuildTarget> getTargets() {
            return myTargets;
        }

        @Override
        public @Nullable BuildTarget findTargetByKey(
            WorkspaceTargetKey key,
            TargetLoadOptions options
        ) {
            return myTargets.get(key);
        }

        @Override
        public Stream<BuildTarget> allTargets(TargetLoadOptions options) {
            return myTargets.values().stream();
        }

    }
}
