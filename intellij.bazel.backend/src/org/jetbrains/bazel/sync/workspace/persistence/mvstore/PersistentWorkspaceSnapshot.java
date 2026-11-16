package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSnapshotMetadata;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

// contain lightweight parts of `WorkspaceSnapshot` which
// shall be serialized into on disk blob
public class PersistentWorkspaceSnapshot {

    // direct references inside `WorkspaceSnapshot`
    private final @Nullable String myWorkspaceName;
    private final Map<WorkspaceConfigurationId, WorkspaceConfiguration> myConfigurations;
    private final WorkspaceTargetGraph myTargetGraph;
    private final List<WorkspaceSyncConfig> mySyncConfigs;
    private final RepoMapping myRepoMapping;
    private final BazelInfo myBazelInfo;
    private final WorkspaceSnapshotMetadata myMetadata;

    // extra things
    private final Int2ObjectBiMap<WorkspaceTargetKey> myKeyId2Target;
    private final Int2ObjectBiMap<Label> myLabelId2Label;

    public PersistentWorkspaceSnapshot(
        @Nullable String workspaceName,
        Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations,
        WorkspaceTargetGraph targetGraph,
        List<WorkspaceSyncConfig> syncConfigs,
        RepoMapping repoMapping,
        BazelInfo bazelInfo,
        WorkspaceSnapshotMetadata metadata,
        Int2ObjectBiMap<WorkspaceTargetKey> keyId2Target,
        Int2ObjectBiMap<Label> labelId2Label
    ) {
        myWorkspaceName = workspaceName;
        myConfigurations = configurations;
        myTargetGraph = targetGraph;
        mySyncConfigs = syncConfigs;
        myRepoMapping = repoMapping;
        myBazelInfo = bazelInfo;
        myMetadata = metadata;
        myKeyId2Target = keyId2Target;
        myLabelId2Label = labelId2Label;
    }

    public @Nullable String getWorkspaceName() {
        return myWorkspaceName;
    }

    public Map<WorkspaceConfigurationId, WorkspaceConfiguration> getConfigurations() {
        return myConfigurations;
    }

    public WorkspaceTargetGraph getTargetGraph() {
        return myTargetGraph;
    }

    public List<WorkspaceSyncConfig> getSyncConfigs() {
        return mySyncConfigs;
    }

    public RepoMapping getRepoMapping() {
        return myRepoMapping;
    }

    public BazelInfo getBazelInfo() {
        return myBazelInfo;
    }

    public WorkspaceSnapshotMetadata getMetadata() {
        return myMetadata;
    }

    public Int2ObjectBiMap<WorkspaceTargetKey> getKeyId2Target() {
        return myKeyId2Target;
    }

    public Int2ObjectBiMap<Label> getLabelId2Label() {
        return myLabelId2Label;
    }

    public static class PartialWorkspaceTarget {
        private final TargetKind myKind;
        private final @Nullable String myGeneratorName;
        private final boolean myIsTestOnly;
        private final List<String> myTags;
        private final String myWorkspaceName;

        public PartialWorkspaceTarget(
            TargetKind kind,
            @Nullable String generatorName,
            boolean isTestOnly,
            List<String> tags,
            String workspaceName
        ) {
            myKind = kind;
            myGeneratorName = generatorName;
            myIsTestOnly = isTestOnly;
            myTags = tags;
            myWorkspaceName = workspaceName;
        }

        public TargetKind getKind() {
            return myKind;
        }

        public @Nullable String getGeneratorName() {
            return myGeneratorName;
        }

        public boolean isTestOnly() {
            return myIsTestOnly;
        }

        public List<String> getTags() {
            return myTags;
        }

        public String getWorkspaceName() {
            return myWorkspaceName;
        }
    }

    public static class WorkspaceTargetContent {
        private final List<DependencyLabel> myDependencies;
        private final OutputLocationCollection mySources;
        private final OutputLocationCollection myResources;

        public WorkspaceTargetContent(
            List<DependencyLabel> dependencies,
            OutputLocationCollection sources,
            OutputLocationCollection resources
        ) {
            myDependencies = dependencies;
            mySources = sources;
            myResources = resources;
        }

        public List<DependencyLabel> getDependencies() {
            return myDependencies;
        }

        public OutputLocationCollection getSources() {
            return mySources;
        }

        public OutputLocationCollection getResources() {
            return myResources;
        }
    }

    public static class WorkspaceTargetToSave {
        private final int myKeyId;
        private final BuildTarget myTarget;

        public WorkspaceTargetToSave(
            int keyId,
            BuildTarget target
        ) {
            myKeyId = keyId;
            myTarget = target;
        }

        public int getKeyId() {
            return myKeyId;
        }

        public BuildTarget getTarget() {
            return myTarget;
        }
    }
}
