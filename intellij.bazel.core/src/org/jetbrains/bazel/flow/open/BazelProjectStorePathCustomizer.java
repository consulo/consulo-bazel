package org.jetbrains.bazel.flow.open;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

import static org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.isDefaultProjectViewFile;
import static org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject;

// consulo: IntelliJ's ProjectStorePathCustomizer does not exist; the class computes the Bazel store descriptor that
// consulo.bazel.core.BazelProjectStoreService persists (used by BazelProjectOpenProcessor and closeAndReopenAsBazelProject)
public class BazelProjectStorePathCustomizer {
    private static final Logger LOG = Logger.getInstance(BazelProjectStorePathCustomizer.class);

    public @Nullable BazelProjectStoreDescriptor getStoreDirectoryPath(Path projectRoot) {
        if (!isFileSupported(projectRoot)) {
            return null;
        }
        return getBazelStoreDirectoryPath(projectRoot);
    }

    private boolean isFileSupported(Path fileBeingOpen) {
        // do not use isDirectory, as we also want to check that the file exists
        return utils.hasNameOf(fileBeingOpen, Constants.SUPPORTED_CONFIG_FILE_NAMES) ||
            utils.hasExtensionOf(fileBeingOpen, Constants.SUPPORTED_EXTENSIONS);
    }

    private @Nullable BazelProjectStoreDescriptor getBazelStoreDirectoryPath(Path openedFile) {
        LOG.info("Computing BazelProjectStoreDescriptor for file: " + openedFile);

        utils.ResolvedProjectRoot resolvedProjectRoot = utils.ResolvedProjectRoot.from(openedFile);
        if (resolvedProjectRoot == null) {
            return null;
        }
        Path projectRootPath = resolvedProjectRoot.getRootDir();
        Path fileBeingOpen = resolvedProjectRoot.getOpenedFile();
        Path projectIdentityFilePath = selectProjectIdentityFilePath(projectRootPath, fileBeingOpen);
        Path projectViewFile = pickProjectViewFileForProject(projectIdentityFilePath, projectRootPath);
        Path dotIdeaPath = selectDotIdeaPath(projectRootPath, projectViewFile);

        if (LOG.isTraceEnabled()) {
            LOG.trace("projectRootPath = " + projectRootPath + ", projectIdentityFilePath = " + projectIdentityFilePath +
                ", projectViewFile = " + projectViewFile + ", dotIdea = " + dotIdeaPath);
        }
        return new BazelProjectStoreDescriptor(
            projectIdentityFilePath,
            dotIdeaPath,
            projectRootPath,
            projectViewFile
        );
    }

    private Path selectProjectIdentityFilePath(Path projectRoot, Path fileBeingOpen) {
        // The purpose of this function is to improve the user's experience with project history.
        // When the file being opened is one of the project view files that the plugin can automatically pick
        // during project opening (the "default project view"), then the project identity file will become MODULE.bazel.
        // This way, we won't create multiple history entries that will ultimately open the same project view.
        if (isDefaultProjectViewFile(fileBeingOpen, projectRoot)) {
            Path workspaceFile = utils.getWorkspaceFile(projectRoot);
            if (workspaceFile != null) {
                return workspaceFile;
            }
        }
        return fileBeingOpen;
    }

    /**
     * Returns the {@code .idea} directory from the project view, or {@code <root>/.idea}.
     * The real path of the directory from the project view must be inside the real project root.
     * The function does not check {@code <root>/.idea}, like the platform does not check it for other projects.
     */
    private Path selectDotIdeaPath(Path projectRootPath, Path projectViewPath) {
        Path defaultDotIdeaPath = projectRootPath.resolve(Project.DIRECTORY_STORE_FOLDER);
        Project defaultProject = ProjectManager.getInstance().getDefaultProject();
        ProjectView projectView = ProjectViewFactory.fromOrNull(defaultProject, projectViewPath, projectRootPath);
        Path dotIdeaDirectoryLocation = projectView == null ? null : ProjectViewExtensions.getDotIdeaDirectoryLocation(projectView);
        if (dotIdeaDirectoryLocation == null) {
            return defaultDotIdeaPath;
        }
        Path dotIdeaPath = projectRootPath.resolve(dotIdeaDirectoryLocation).normalize();
        if (!PathUtil.isRealPathInside(dotIdeaPath, projectRootPath)) {
            LOG.warn("The .idea directory " + dotIdeaPath + " is outside of the project root " + projectRootPath +
                ", using " + defaultDotIdeaPath);
            return defaultDotIdeaPath;
        }
        return dotIdeaPath;
    }
}
