package org.jetbrains.bazel.clion.workspace;

import consulo.application.progress.ProgressManager;
import consulo.bazel.cpp.workspace.CcWorkspaceModel;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jspecify.annotations.Nullable;

/**
 * Selects a configuration for files that are not a source of any configuration, e.g. newly created files.
 * <p>
 * consulo: CIDR {@code OCResolveConfigurationChooser} (extension {@code cidr.projectModel.resolveConfigurationChooser}) does
 * not exist in consulo-cpp; the class is not registered, {@code consulo.bazel.cpp.workspace.CcFileOptionsContributor}
 * asks it for the files that are not a source of a configuration. {@code OCWorkspace} is {@link CcWorkspaceModel},
 * {@code OCResolveConfiguration} is {@link CcWorkspaceModel.Configuration}.
 */
public class CcDirectoryBasedChooser {
    public CcDirectoryBasedChooser() {
    }

    // consulo: upstream also gets the OCResolveConfigurationSlice; consulo-cpp asks for every file without a slice, which
    // upstream handles as OCResolveConfigurationSlice.All
    public CcWorkspaceModel.@Nullable Configuration selectPreselectedResolveConfiguration(
        Project project,
        @Nullable VirtualFile file
    ) {
        // a specific slice means the file is known to the workspace, leave the choice to the other choosers
        if (file == null || !BazelProjectInfoService.getBazelProjectInfo(project).isBazelProject()) {
            return null;
        }

        VirtualFile projectRoot = BazelProjectInfoService.getBazelProjectInfo(project).getProjectRootDir();
        if (projectRoot == null) {
            return null;
        }
        CcWorkspaceModel workspace = CcWorkspaceModel.getInstance(project);
        // consulo: OCWorkspace.getInstance(project) always exists; the model is absent before the first C/C++ import
        if (workspace == null) {
            return null;
        }

        VirtualFile directory = file.getParent();
        while (directory != null && VirtualFileUtil.isAncestor(projectRoot, directory, /* strict = */ false)) {
            CcWorkspaceModel.Configuration configuration = findConfigurationInDirectory(workspace, directory);
            if (configuration != null) {
                return configuration;
            }
            directory = directory.getParent();
        }

        return null;
    }

    /**
     * Returns the first configuration of the sources in {@code directory}.
     */
    private static CcWorkspaceModel.@Nullable Configuration findConfigurationInDirectory(
        CcWorkspaceModel workspace,
        VirtualFile directory
    ) {
        VirtualFile[] children = directory.getChildren();
        // consulo: getChildren() is nullable in Consulo (an invalid directory)
        if (children == null) {
            return null;
        }
        for (VirtualFile child : children) {
            ProgressManager.checkCanceled();
            if (child.isDirectory()) {
                continue;
            }

            for (CcWorkspaceModel.Configuration configuration : workspace.getConfigurationsForFile(child)) {
                return configuration;
            }
        }

        return null;
    }
}
