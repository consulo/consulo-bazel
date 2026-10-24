package org.jetbrains.bazel.sync.workspace.persistence;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.DependencyLabel.DependencyLabelKind;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.HeavyWorkspaceTarget;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.PartialWorkspaceTarget;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.WorkspaceTargetDeps;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.CommonWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationSummary;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSnapshotMetadata;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph.LabelConfigKey;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph.WorkspaceTargetGraphImpl;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.sealed;
import static org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.type;

@ExtensionImpl
class DefaultWorkspaceTypeContributor implements WorkspaceTypeContributor {
    public DefaultWorkspaceTypeContributor() {
    }

    @Override
    public List<WorkspaceTypeEntry> contribute(Project project) {
        List<WorkspaceTypeEntry> result = new ArrayList<>();
        // blob and MVStore envelope types
        type(result, PersistentWorkspaceSnapshot.class);
        type(result, PartialWorkspaceTarget.class);
        type(result, WorkspaceTargetDeps.class);
        type(result, HeavyWorkspaceTarget.class);

        // configuration
        type(result, WorkspaceConfigurationId.class);
        type(result, WorkspaceConfiguration.class);
        type(result, WorkspaceConfigurationSummary.class);

        // graph
        type(result, LabelConfigKey.class);
        type(result, WorkspaceTargetGraphImpl.class);

        // core types
        type(result, DependencyLabel.class);
        type(result, DependencyLabelKind.class);
        type(result, WorkspaceTargetKey.class);
        // consulo: upstream WorkspaceAspectIds is an inline value class, in Java it is a real class stored inside WorkspaceTargetKey
        type(result, WorkspaceAspectIds.class);
        type(result, WorkspaceSnapshotMetadata.class);
        type(result, CommonWorkspaceSyncConfig.class);
        type(result, TargetKind.class);
        type(result, RuleType.class);
        type(result, LanguageClass.class);
        sealed(result, RepoMapping.class);

        type(result, StrictDependencyCheckedType.class);

        // TrieOutputLocationCollection/PathsTrie/TrieNode are NOT contributed: they are registered as builtins with a
        // handwritten serializer (see SnapshotSerializers.registerBuiltinTypes) - contributing them here
        // would re-register the class with the default reflective serializer
        type(result, OutputLocationCollection.class);

        type(result, BazelInfo.class);
        type(result, BazelRelease.class);
        return result;
    }
}
