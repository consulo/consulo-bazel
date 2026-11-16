package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.workspace.CcWorkspaceModel;
import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Selects the resolve configuration that has a configuration of the build of the last debug session.
 */
// consulo: upstream implements CLion's OCResolveConfigurationChooser (cidr projectModel.resolveConfigurationChooser
//   Bazel.ccDebugSessionChooser); consulo-cpp has no choosers, consulo.bazel.cpp.workspace.CcFileOptionsContributor asks this class
//   with the configurations of the file (upstream OCResolveConfigurationSlice.resolve)
public class CcDebugSessionChooser {
    public CcWorkspaceModel.@Nullable Configuration selectPreselectedResolveConfiguration(
        Project project,
        List<CcWorkspaceModel.Configuration> candidates
    ) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }

        List<WorkspaceConfigurationId> configurations = CcDebugSessionConfigurations.getInstance(project).getConfigurations();
        if (configurations.isEmpty()) {
            return null;
        }

        for (WorkspaceConfigurationId configuration : configurations) {
            for (CcWorkspaceModel.Configuration candidate : candidates) {
                CcResolveConfiguration.@Nullable Identifier identifier = buildEquivalenceClasses.getCcIdentifier(candidate);
                if (identifier != null && configuration.equals(identifier.getConfigurationId())) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
