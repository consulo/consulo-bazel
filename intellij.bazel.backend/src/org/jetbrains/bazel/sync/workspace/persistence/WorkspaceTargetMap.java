package org.jetbrains.bazel.sync.workspace.persistence;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

public interface WorkspaceTargetMap {
    @Nullable BuildTargetInfo findTargetInfoByKey(WorkspaceTargetKey key);

    Stream<BuildTargetInfo> allTargetInfos();

    @Nullable BuildTarget findTargetByKey(WorkspaceTargetKey key);

    /**
     * @deprecated Use {@link #findTargetByKey(WorkspaceTargetKey)} or {@link #findTargetInfoByKey(WorkspaceTargetKey)}
     */
    @Deprecated
    default @Nullable BuildTarget findTargetByKey(WorkspaceTargetKey key, TargetLoadOptions options) {
        return findTargetByKey(key);
    }

    Stream<BuildTarget> allTargets();

    /**
     * @deprecated Use {@link #allTargets()} or {@link #allTargetInfos()}
     */
    @Deprecated
    default Stream<BuildTarget> allTargets(TargetLoadOptions options) {
        return allTargets();
    }

    /**
     * Loads one language data of one target, without the other parts of the {@link BuildTarget}.
     */
    <T extends BuildTargetData> @Nullable T findTargetData(WorkspaceTargetKey key, Class<T> type);

    WorkspaceTargetMap EMPTY = new WorkspaceTargetMap() {
        @Override
        public @Nullable BuildTargetInfo findTargetInfoByKey(WorkspaceTargetKey key) {
            return null;
        }

        @Override
        public Stream<BuildTargetInfo> allTargetInfos() {
            return Stream.of();
        }

        @Override
        public @Nullable BuildTarget findTargetByKey(WorkspaceTargetKey key) {
            return null;
        }

        @Override
        public Stream<BuildTarget> allTargets() {
            return Stream.of();
        }

        @Override
        public <T extends BuildTargetData> @Nullable T findTargetData(WorkspaceTargetKey key, Class<T> type) {
            return null;
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
        public @Nullable BuildTargetInfo findTargetInfoByKey(WorkspaceTargetKey key) {
            BuildTarget target = myTargets.get(key);
            return target != null ? target.getInfo() : null;
        }

        @Override
        public Stream<BuildTargetInfo> allTargetInfos() {
            return myTargets.values().stream().map(BuildTarget::getInfo);
        }

        @Override
        public @Nullable BuildTarget findTargetByKey(WorkspaceTargetKey key) {
            return myTargets.get(key);
        }

        @Override
        public Stream<BuildTarget> allTargets() {
            return myTargets.values().stream();
        }

        @Override
        public <T extends BuildTargetData> @Nullable T findTargetData(WorkspaceTargetKey key, Class<T> type) {
            BuildTarget target = myTargets.get(key);
            return target != null ? BuildTarget.data(target, type) : null;
        }
    }
}
