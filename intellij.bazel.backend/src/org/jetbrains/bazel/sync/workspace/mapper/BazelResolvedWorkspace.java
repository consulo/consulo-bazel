package org.jetbrains.bazel.sync.workspace;

import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record BazelResolvedWorkspace(
    @Nullable String workspaceName,
    RepoMapping repoMapping,
    Set<WorkspaceTargetKey> rootTargets,
    List<BuildTarget> targets,
    boolean hasError,
    Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations
) {
    // consulo: overload for the upstream default argument (hasError = false)
    public BazelResolvedWorkspace(
        @Nullable String workspaceName,
        RepoMapping repoMapping,
        Set<WorkspaceTargetKey> rootTargets,
        List<BuildTarget> targets,
        Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations
    ) {
        this(workspaceName, repoMapping, rootTargets, targets, false, configurations);
    }
}
