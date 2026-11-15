package org.jetbrains.bazel.clion.workspace;

import consulo.project.Project;
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CcTargetUtils {
    private CcTargetUtils() {
    }

    public static @Nullable WorkspaceTargetKey findToolchainKey(WorkspaceSnapshot snapshot, BuildTarget target) {
        Set<WorkspaceTargetKey> toolchains = WorkspaceHelpers.getAllTargets(snapshot)
            .filter(it -> WorkspaceHelpers.hasBuildData(it, CcToolchainBuildTarget.class))
            .map(BuildTarget::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<WorkspaceTargetKey> candidates = buildToolchainMap.findTargetToolchain(target, toolchains);
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    /**
     * Returns the toolchain of a CC target, or null if the toolchain is ambiguous or not found.
     */
    public static @Nullable BuildTarget findToolchain(WorkspaceSnapshot snapshot, BuildTarget target) {
        @Nullable WorkspaceTargetKey key = findToolchainKey(snapshot, target);
        return key != null ? snapshot.targets().findTargetByKey(key) : null;
    }

    /**
     * Returns the compiler of a CC target, or null if the toolchain or its compiler is unknown.
     */
    public static @Nullable CcCompilerInfo findCompiler(Project project, WorkspaceSnapshot snapshot, BuildTarget target) {
        @Nullable WorkspaceTargetKey key = findToolchainKey(snapshot, target);
        return key != null ? CcCompilerInfoService.getInstance(project).get(key) : null;
    }
}
