package org.jetbrains.bazel.config;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;

// consulo: upstream Kotlin extension properties of Project. The class keeps the upstream file name; callers that have a
// local variable named "project" use static imports (a variable obscures a class with the same simple name).
public final class project {
    private project() {
    }

    public static boolean isBazelProject(Project receiver) {
        return BazelProjectInfoService.getBazelProjectInfo(receiver).isBazelProject();
    }

    public static VirtualFile getRootDir(Project receiver) {
        return BazelProjectInfoService.getProjectRootDirOrThrow(BazelProjectInfoService.getBazelProjectInfo(receiver));
    }

    public static String getBazelProjectName(Project receiver) {
        return getRootDir(receiver).getName();
    }
}
