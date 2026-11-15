package org.jetbrains.bazel.golang.sync;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class GoWorkspaceTypeContributor implements WorkspaceTypeContributor {
    // consulo: upstream private top-level const
    @SuppressWarnings("unused")
    private static final String GOLANG_NAMESPACE = "intellij.bazel.golang";

    @Override
    public List<WorkspaceTypeEntry> contribute(Project project) {
        List<WorkspaceTypeEntry> result = new ArrayList<>();
        WorkspaceTypeContributor.type(result, GoBuildTarget.class);
        return result;
    }
}
