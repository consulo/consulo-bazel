package org.jetbrains.bazel.workspace.sync.jvm;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JavaProviderData;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JavaToolchainData;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JdepsJar;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JvmDependency;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JvmOutputs;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.ScalaBuildTarget;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class JvmWorkspaceTypeContributor implements WorkspaceTypeContributor {
    @Override
    public List<WorkspaceTypeEntry> contribute(Project project) {
        List<WorkspaceTypeEntry> result = new ArrayList<>();
        WorkspaceTypeContributor.type(result, JvmBuildTarget.class);
        WorkspaceTypeContributor.type(result, KotlinBuildTarget.class);
        WorkspaceTypeContributor.type(result, ScalaBuildTarget.class);
        WorkspaceTypeContributor.type(result, JvmOutputs.class);
        WorkspaceTypeContributor.type(result, JdepsJar.class);
        WorkspaceTypeContributor.type(result, JavaProviderData.class);
        WorkspaceTypeContributor.type(result, JavaToolchainData.class);
        WorkspaceTypeContributor.sealed(result, JvmDependency.class);
        WorkspaceTypeContributor.type(result, JavaWorkspaceSyncConfig.class);
        return result;
    }
}
