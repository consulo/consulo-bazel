package org.jetbrains.bazel.clion.sync;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class CcWorkspaceTypeContributor implements WorkspaceTypeContributor {
    public CcWorkspaceTypeContributor() {
    }

    @Override
    public List<WorkspaceTypeEntry> contribute(Project project) {
        List<WorkspaceTypeEntry> result = new ArrayList<>();
        WorkspaceTypeContributor.type(result, CcBuildTarget.class);
        WorkspaceTypeContributor.type(result, CcBuildTarget.RuleContext.class);
        WorkspaceTypeContributor.type(result, CcBuildTarget.CompilationContext.class);
        WorkspaceTypeContributor.type(result, CcToolchainBuildTarget.class);
        WorkspaceTypeContributor.type(result, CcToolchainBuildTarget.XcodeInfo.class);
        return result;
    }
}
