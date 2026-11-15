package org.jetbrains.bazel.flow.open;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

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

    private BazelProjectStoreDescriptor getBazelStoreDirectoryPath(Path fileBeingOpen) {
        LOG.info("Computing BazelProjectStoreDescriptor for file: " + fileBeingOpen);

        Path projectRootPath = findProjectRootPath(fileBeingOpen);
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

    private Path findProjectRootPath(Path fileBeingOpen) {
        Path projectFolder = utils.findProjectFolderFromFile(fileBeingOpen);
        return projectFolder != null ? projectFolder : Objects.requireNonNull(fileBeingOpen.getParent());
    }

    private Path selectDotIdeaPath(Path projectRootPath, Path projectViewPath) {
        Project defaultProject = ProjectManager.getInstance().getDefaultProject();
        ProjectView projectView = ProjectViewFactory.fromOrNull(defaultProject, projectViewPath, projectRootPath);
        Path dotIdeaDirectoryLocation = projectView == null ? null : ProjectViewExtensions.getDotIdeaDirectoryLocation(projectView);
        return dotIdeaDirectoryLocation != null ? projectRootPath.resolve(dotIdeaDirectoryLocation) : defaultDotIdeaDirectory(projectRootPath);
    }

    private static Path defaultDotIdeaDirectory(Path receiver) {
        return receiver.resolve(Project.DIRECTORY_STORE_FOLDER);
    }
}
