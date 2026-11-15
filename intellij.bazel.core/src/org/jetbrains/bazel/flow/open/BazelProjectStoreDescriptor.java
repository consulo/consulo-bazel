package org.jetbrains.bazel.flow.open;

import org.jetbrains.bazel.commons.constants.Constants;

import java.nio.file.Path;

// consulo: IntelliJ's ProjectStoreDescriptor does not exist; the descriptor is a plain class persisted by
// consulo.bazel.core.BazelProjectStoreService. The storage spec overrides (getModuleStorageSpecs, getStorageSpecs,
// testStoreDirectoryExistsForProjectRoot, getJpsBridgeAwareStorageSpec) are IntelliJ project store specific and are dropped.
public class BazelProjectStoreDescriptor {
    private final Path myProjectIdentityFile;
    private final Path myDotIdea;
    private final Path myHistoricalProjectBasePath;
    private final Path myProjectViewFile;

    public BazelProjectStoreDescriptor(
        Path projectIdentityFile,
        Path dotIdea,
        Path historicalProjectBasePath,
        Path projectViewFile
    ) {
        myProjectIdentityFile = projectIdentityFile;
        myDotIdea = dotIdea;
        myHistoricalProjectBasePath = historicalProjectBasePath;
        myProjectViewFile = projectViewFile;
    }

    public Path getProjectIdentityFile() {
        return myProjectIdentityFile;
    }

    public Path getDotIdea() {
        return myDotIdea;
    }

    public Path getHistoricalProjectBasePath() {
        return myHistoricalProjectBasePath;
    }

    public Path getProjectViewFile() {
        return myProjectViewFile;
    }

    public String getProjectName() {
        String fileName = myProjectIdentityFile.getFileName().toString();

        String projectViewName;
        if (fileName.endsWith(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME) &&
            !fileName.equals(Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME)
        ) {
            String name = fileName.substring(0, fileName.length() - Constants.DEFAULT_PROJECT_VIEW_FILE_NAME.length());
            projectViewName = name.isEmpty() ? null : name;
        }
        else {
            projectViewName = null;
        }

        // consulo: upstream super.projectName of ProjectStoreDescriptor is the file name of the historical project base path
        Path baseFileName = myHistoricalProjectBasePath.getFileName();
        String projectName = baseFileName == null ? myHistoricalProjectBasePath.toString() : baseFileName.toString();
        if (projectViewName != null && !projectViewName.equals(projectName)) {
            return projectName + " (" + projectViewName + ")";
        }
        else {
            return projectName;
        }
    }
}
