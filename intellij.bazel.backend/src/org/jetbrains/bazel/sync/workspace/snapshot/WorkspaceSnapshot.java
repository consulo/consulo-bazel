package org.jetbrains.bazel.sync.workspace.snapshot;

import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTargetMap;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Immutable workspace snapshot, describing fixed input scope for {@link BazelWorkspaceImporter}
 *
 * @param targets           Lazy loadable collection of {@link BuildTarget}
 * @param workspaceName     Bazel workspace name, {@code null} when it is not known yet
 * @param configurations    All used bazel configurations
 * @param targetGraph       Immutable representation of bazel target graph
 * @param syncConfigs       Set of sync configs used by {@link BazelWorkspaceImporter} in later stage
 * @param repoMapping       Bazel repo mapping
 * @param metadata          Snapshot metadata
 */
public record WorkspaceSnapshot(
    WorkspaceTargetMap targets,
    @Nullable String workspaceName,
    Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations,
    WorkspaceTargetGraph targetGraph,
    FileToTargetMap fileToTarget,
    ExecutableTargetsIndex executableTargets,
    List<WorkspaceSyncConfig> syncConfigs,
    RepoMapping repoMapping,
    BazelInfo bazelInfo,
    WorkspaceSnapshotMetadata metadata
) {
    public static final WorkspaceSnapshot EMPTY = new WorkspaceSnapshot(
        WorkspaceTargetMap.EMPTY,
        null,
        Map.of(),
        WorkspaceTargetGraph.EMPTY,
        FileToTargetMap.EMPTY,
        ExecutableTargetsIndex.EMPTY,
        List.of(),
        RepoMapping.RepoMappingDisabled.INSTANCE,
        BazelInfo.DEFAULT,
        new WorkspaceSnapshotMetadata(
            1
        )
    );

    public record WorkspaceSnapshotMetadata(
        int version
    ) {
    }

    /**
     * Marker interface for workspace importer specific configuration,
     * Any kind of projectview flag, registry key, or user setting have to be passed
     * using class implementing {@link WorkspaceSyncConfig} to ensure correct workspace state.
     */
    // RC: keep it open, so other modules can extend it
    public interface WorkspaceSyncConfig {
    }

    /**
     * Common {@link WorkspaceSyncConfig} shared among all importers
     *
     * @param projectRootDir Project root directory shall be used instead of {@code Project.rootDir} to ensure importer pure semantics
     * @param projectName    Project name shall be used instead of {@code Project.bazelProjectName}
     */
    public record CommonWorkspaceSyncConfig(
        Path projectRootDir,
        @Nullable Path dotIdeaPath,
        String projectName,
        int importDepth
    ) implements WorkspaceSyncConfig {
    }
}
