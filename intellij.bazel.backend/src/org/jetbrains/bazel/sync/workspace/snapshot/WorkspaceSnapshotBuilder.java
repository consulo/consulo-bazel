package org.jetbrains.bazel.sync.workspace.snapshot;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.RepoMappingDisabled;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTargetMap.InMemoryWorkspaceTargetMap;
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndex.ExecutableTargetsIndexBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.InMemoryFileToTargetMap.File2TargetMapBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.CommonWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSnapshotMetadata;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSyncConfig;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.jetbrains.bazel.config.project.getBazelProjectName;
import static org.jetbrains.bazel.config.project.getRootDir;

public final class WorkspaceSnapshotBuilder {
    private WorkspaceSnapshotBuilder() {
    }

    public static CompletableFuture<WorkspaceSnapshot> build(
        Project project,
        ProjectView projectView,
        RepoMapping repoMapping,
        BazelInfo bazelInfo,
        BazelResolvedWorkspace resolved
    ) {
        Path workspaceRoot = getRootDir(project).toNioPath();
        CommonWorkspaceSyncConfig commonSyncConfig = new CommonWorkspaceSyncConfig(
            workspaceRoot,
            dotIdeaPath(projectView, project),
            getBazelProjectName(project),
            ProjectViewExtensions.getImportDepth(projectView)
        );
        Map<WorkspaceTargetKey, BuildTarget> targets = associateByKey(resolved.targets());
        WorkspaceTargetGraph targetGraph = WorkspaceTargetGraph.WorkspaceTargetGraphBuilder.build(resolved.rootTargets(), targets.values());
        return createSyncConfigs(project, projectView, commonSyncConfig).thenApply(syncConfigs -> new WorkspaceSnapshot(
            new InMemoryWorkspaceTargetMap(targets),
            resolved.workspaceName(),
            resolved.configurations(),
            targetGraph,
            new File2TargetMapBuilder(bazelInfo, repoMapping).build(targets.values()),
            ExecutableTargetsIndexBuilder.build(
                targetGraph,
                commonSyncConfig.importDepth(),
                targets.values()
            ),
            syncConfigs,
            repoMapping,
            bazelInfo,
            new WorkspaceSnapshotMetadata(
                1
            )
        ));
    }

    private static @Nullable Path dotIdeaPath(
        ProjectView projectView,
        Project project
    ) {
        Path location = ProjectViewExtensions.getDotIdeaDirectoryLocation(projectView);
        if (location != null) {
            return location;
        }
        // consulo: IntelliJ VirtualFile.toNioPathOrNull()
        VirtualFile rootDir = getRootDir(project);
        try {
            return rootDir.toNioPath().resolve(Project.DIRECTORY_STORE_FOLDER);
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }

    public static IncompleteWorkspaceSnapshot buildIncomplete(BazelResolvedWorkspace resolved) {
        return new IncompleteWorkspaceSnapshot(
            associateByKey(resolved.targets()),
            resolved.rootTargets(),
            resolved.configurations(),
            resolved.repoMapping(),
            resolved.workspaceName()
        );
    }

    public static CompletableFuture<WorkspaceSnapshot> merge(
        Project project,
        ProjectView projectView,
        BazelInfo bazelInfo,
        List<IncompleteWorkspaceSnapshot> snapshots
    ) {
        // newest snapshot comes last
        Path workspaceRoot = getRootDir(project).toNioPath();
        CommonWorkspaceSyncConfig commonSyncConfig = new CommonWorkspaceSyncConfig(
            workspaceRoot,
            dotIdeaPath(projectView, project),
            getBazelProjectName(project),
            ProjectViewExtensions.getImportDepth(projectView)
        );
        Map<WorkspaceTargetKey, BuildTarget> targets = new LinkedHashMap<>();
        for (IncompleteWorkspaceSnapshot snapshot : snapshots) {
            for (BuildTarget target : snapshot.targets().values()) {
                targets.put(target.getKey(), target);
            }
        }
        Set<WorkspaceTargetKey> rootTargets = new LinkedHashSet<>();
        for (IncompleteWorkspaceSnapshot snapshot : snapshots) {
            rootTargets.addAll(snapshot.rootTargets());
        }
        Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations = new LinkedHashMap<>();
        for (IncompleteWorkspaceSnapshot snapshot : snapshots) {
            for (WorkspaceConfiguration configuration : snapshot.configurations().values()) {
                configurations.put(configuration.id(), configuration);
            }
        }
        WorkspaceTargetGraph targetGraph = WorkspaceTargetGraph.WorkspaceTargetGraphBuilder.build(rootTargets, targets.values());
        RepoMapping repoMapping;
        if (snapshots.stream().allMatch(it -> it.repoMapping() instanceof RepoMappingDisabled)) {
            repoMapping = RepoMappingDisabled.INSTANCE;
        }
        else {
            Set<String> nonLocalCanonicalRepoNames = new LinkedHashSet<>();
            for (IncompleteWorkspaceSnapshot it : snapshots) {
                if (it.repoMapping() instanceof BzlmodRepoMapping bzlmod) {
                    nonLocalCanonicalRepoNames.addAll(bzlmod.getNonLocalCanonicalRepoNames());
                }
            }
            repoMapping = new BzlmodRepoMapping(
                mergeMaps(snapshots
                    .stream()
                    .map(it -> it.repoMapping() instanceof BzlmodRepoMapping bzlmod ? bzlmod.getCanonicalRepoNameToLocalPath() : Map.<String, Path>of())
                    .toList()),
                mergeMaps(snapshots
                    .stream()
                    .map(it -> it.repoMapping() instanceof BzlmodRepoMapping bzlmod ? bzlmod.getApparentRepoNameToCanonicalName() : Map.<String, String>of())
                    .toList()),
                mergeMaps(snapshots
                    .stream()
                    .map(it -> it.repoMapping() instanceof BzlmodRepoMapping bzlmod ? bzlmod.getCanonicalRepoNameToPath() : Map.<String, Path>of())
                    .toList()),
                nonLocalCanonicalRepoNames
            );
        }
        IncompleteWorkspaceSnapshot last = snapshots.get(snapshots.size() - 1);
        RepoMapping finalRepoMapping = repoMapping;
        return createSyncConfigs(project, projectView, commonSyncConfig).thenApply(syncConfigs -> new WorkspaceSnapshot(
            new InMemoryWorkspaceTargetMap(targets),
            last.workspaceName(),
            configurations,
            targetGraph,
            new File2TargetMapBuilder(bazelInfo, finalRepoMapping).build(targets.values()),
            ExecutableTargetsIndexBuilder.build(
                targetGraph,
                commonSyncConfig.importDepth(),
                targets.values()
            ),
            syncConfigs,
            finalRepoMapping,
            bazelInfo,
            new WorkspaceSnapshotMetadata(
                1
            )
        ));
    }

    private static <K, V> Map<K, V> mergeMaps(List<Map<K, V>> receiver) {
        Map<K, V> result = new LinkedHashMap<>();
        receiver.forEach(result::putAll);
        return result;
    }

    // consulo: `listOf(commonSyncConfig) + LanguagePlugin.EP_NAME.extensionList.flatMap { it.createSyncConfigs(project, projectView) }`,
    // createSyncConfigs is suspend upstream, the plugins are asked one after another
    private static CompletableFuture<List<WorkspaceSyncConfig>> createSyncConfigs(
        Project project,
        ProjectView projectView,
        CommonWorkspaceSyncConfig commonSyncConfig
    ) {
        List<WorkspaceSyncConfig> result = new ArrayList<>();
        result.add(commonSyncConfig);
        return collectSyncConfigs(project, projectView, LanguagePlugin.getEpName().getExtensionList().iterator(), result);
    }

    private static CompletableFuture<List<WorkspaceSyncConfig>> collectSyncConfigs(
        Project project,
        ProjectView projectView,
        Iterator<LanguagePlugin> plugins,
        List<WorkspaceSyncConfig> result
    ) {
        if (!plugins.hasNext()) {
            return CompletableFuture.completedFuture(result);
        }
        return plugins.next().createSyncConfigs(project, projectView).thenCompose(configs -> {
            result.addAll(configs);
            return collectSyncConfigs(project, projectView, plugins, result);
        });
    }

    // consulo: Kotlin `associateBy { it.key }`
    private static Map<WorkspaceTargetKey, BuildTarget> associateByKey(List<BuildTarget> targets) {
        Map<WorkspaceTargetKey, BuildTarget> result = new LinkedHashMap<>();
        for (BuildTarget target : targets) {
            result.put(target.getKey(), target);
        }
        return result;
    }
}
