package org.jetbrains.bazel.workspacemodel.entities;

import consulo.bazel.workspace.BazelProjectModelService;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;
import org.jspecify.annotations.Nullable;

import java.util.List;

// consulo: classic data instead of workspace entities. Module and library extensions are attached with
// BazelModelBuilder.putModuleData / putLibraryData and read through BazelProjectModelService.
public final class extensions {
    private extensions() {
    }

    public record BazelModuleExtensionEntity(
        WorkspaceModelTargetKey _targetKey,
        WorkspaceModelTargetSourceRootTypeId rootTypeId,
        WorkspaceModelTargetLabelList strictDependencies
    ) {
        public WorkspaceTargetKey getTargetKey() {
            return _targetKey.toWorkspaceTarget();
        }
    }

    public record BazelLibraryExtensionEntity(
        WorkspaceModelTargetKey _targetKey,
        boolean isSynthetic
    ) {
        public WorkspaceTargetKey getTargetKey() {
            return _targetKey.toWorkspaceTarget();
        }
    }

    public static @Nullable BazelModuleExtensionEntity getBazelModuleExtension(Module module) {
        return BazelProjectModelService.getInstance(module.getProject()).getModuleData(module, BazelModuleExtensionEntity.class);
    }

    public static @Nullable BazelLibraryExtensionEntity getBazelLibraryExtension(Project project, Library library) {
        return BazelProjectModelService.getInstance(project).getLibraryData(library, BazelLibraryExtensionEntity.class);
    }

    public record WorkspaceModelTargetKey(
        String label,
        @Nullable String configuration,
        List<String> aspectIds
    ) {
        public WorkspaceTargetKey toWorkspaceTarget() {
            return new WorkspaceTargetKey(
                Label.parse(label),
                WorkspaceConfigurationId.of(configuration),
                WorkspaceAspectIds.of(aspectIds)
            );
        }

        public static WorkspaceModelTargetKey of(WorkspaceTargetKey key) {
            return new WorkspaceModelTargetKey(
                key.label().toString(),
                key.configuration().getShortChecksum(),
                key.aspectIds().getIds()
            );
        }
    }

    public record WorkspaceModelTargetLabelList(StrictDependencyCheckedType check, List<String> labels) {
    }

    /**
     * consulo: upstream {@code SourceRootTypeId} is a workspace model id; here it is the id of the Consulo
     * {@code ContentFolderTypeProvider}.
     */
    public record WorkspaceModelTargetSourceRootTypeId(@Nullable String defaultId) {
    }
}
