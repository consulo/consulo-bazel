package org.jetbrains.bazel.config;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;

// TODO Remove when Scala plugin no longer uses these methods
public final class BazelProjectProperties {
    private BazelProjectProperties() {
    }

    /**
     * @deprecated Exists only for binary compatibility
     */
    @Deprecated
    public static boolean isBazelProject(Project project) {
        return BazelProjectInfoService.getBazelProjectInfo(project).isBazelProject();
    }

    /**
     * @deprecated Exists only for binary compatibility
     */
    @Deprecated
    public static VirtualFile getRootDir(Project project) {
        return BazelProjectInfoService.getProjectRootDirOrThrow(BazelProjectInfoService.getBazelProjectInfo(project));
    }
}
