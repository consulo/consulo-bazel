package org.jetbrains.bazel.rust.sync;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * consulo: Consulo-only, the Rust build target data persisted in the workspace snapshot (like {@code JvmWorkspaceTypeContributor}).
 */
@ExtensionImpl
public class RustWorkspaceTypeContributor implements WorkspaceTypeContributor {
    @Override
    public List<WorkspaceTypeEntry> contribute(Project project) {
        List<WorkspaceTypeEntry> result = new ArrayList<>();
        WorkspaceTypeContributor.type(result, RustModule.class);
        WorkspaceTypeContributor.type(result, RustModule.RustCrateLocation.class);
        WorkspaceTypeContributor.type(result, RustToolchain.class);
        return result;
    }
}
