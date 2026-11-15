package org.jetbrains.bazel.clion.workspace;

import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * TODO consulo: CIDR {@code OCWorkspaceStorageCustomizer} (extension {@code cidr.projectModel.ocWorkspaceStorageCustomizer},
 *  the module CLion stores the OC workspace entities under) does not exist in consulo-cpp; the class is not registered.
 *  The C/C++ workspace is project data of the Bazel model ({@code consulo.bazel.cpp.workspace.CcWorkspaceModel}).
 */
public class CcWorkspaceStorageCustomizer {
    public CcWorkspaceStorageCustomizer() {
    }

    // consulo: upstream returns the ModuleId, here the module name
    public @Nullable String getModuleId(Project project) {
        return workspaceModule.findCcWorkspaceModuleId(project);
    }
}
