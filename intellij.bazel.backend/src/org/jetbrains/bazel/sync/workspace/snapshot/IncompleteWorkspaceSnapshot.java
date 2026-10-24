package org.jetbrains.bazel.sync.workspace.snapshot;

import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// contains minimal set of information needed for WorkspaceSnapshot construction/merge,
// it allows to avoid unnecessary building of target graph and indexes
public record IncompleteWorkspaceSnapshot(
    Map<WorkspaceTargetKey, BuildTarget> targets,
    Set<WorkspaceTargetKey> rootTargets,
    Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations,
    RepoMapping repoMapping,
    @Nullable String workspaceName
) {
    public static IncompleteWorkspaceSnapshot toIncompleteSnapshot(WorkspaceSnapshot receiver) {
        Map<WorkspaceTargetKey, BuildTarget> targets = new LinkedHashMap<>();
        receiver.targets().allTargets().forEach(it -> targets.put(it.getKey(), it));
        return new IncompleteWorkspaceSnapshot(
            targets,
            receiver.targetGraph().getRootTargets().collect(Collectors.toCollection(HashSet::new)),
            receiver.configurations(),
            receiver.repoMapping(),
            receiver.workspaceName()
        );
    }
}
