package org.jetbrains.bazel.ui.tabs;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileUtils;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

@ExtensionImpl
public class BazelEditorTabTitleProvider implements EditorTabTitleProvider {
    @Override
    public @Nullable String getEditorTabTitle(Project project, VirtualFile file) {
        if (requiresDecoration(project, file.getName())) {
            return BazelFileUtils.getContainingDirectoryPresentablePath(project, file);
        }
        else {
            return null;
        }
    }

    private boolean requiresDecoration(Project project, String fileName) {
        return org.jetbrains.bazel.config.project.isBazelProject(project) &&
            (Arrays.asList(Constants.BUILD_FILE_NAMES).contains(fileName)
                || Arrays.asList(Constants.WORKSPACE_FILE_NAMES).contains(fileName));
    }
}
