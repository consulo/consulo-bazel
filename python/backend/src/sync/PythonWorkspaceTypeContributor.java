package com.intellij.bazel.python.backend.sync;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.python.lang.PythonBuildTarget;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class PythonWorkspaceTypeContributor implements WorkspaceTypeContributor {
    @Override
    public List<WorkspaceTypeEntry> contribute(Project project) {
        List<WorkspaceTypeEntry> result = new ArrayList<>();
        WorkspaceTypeContributor.type(result, PythonBuildTarget.class);
        WorkspaceTypeContributor.type(result, PythonWorkspaceSyncConfig.class);
        return result;
    }
}
