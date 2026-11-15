package org.jetbrains.bazel.flow.exclude;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Scans the project workspace for Bazel convenience symlinks and excludes them when the project opens.
 * It intentionally doesn't check if it's a Bazel project, so it will work even when
 * the user hasn't yet requested to attach the Bazel support to the project.
 */
@ExtensionImpl
public class BazelSymlinkExcludeStartupActivity implements PostStartupActivity {
    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        Path bazelWorkspace = guessBazelWorkspaceDir(project);
        if (bazelWorkspace == null) {
            return;
        }
        BazelSymlinkExcludeService bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(project);
        bazelSymlinkExcludeService.scanForBazelSymlinksToExclude(bazelWorkspace);
    }

    private @Nullable Path guessBazelWorkspaceDir(Project receiver) {
        VirtualFile projectRootDir = BazelProjectInfoService.getBazelProjectInfo(receiver).getProjectRootDir();
        // consulo: Consulo has no Project.guessProjectDir(), the project base directory is used
        VirtualFile virtualFile = projectRootDir != null ? projectRootDir : receiver.getBaseDir();
        if (virtualFile == null) {
            return null;
        }
        // consulo: IntelliJ VirtualFile.toNioPathOrNull()
        try {
            return virtualFile.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
