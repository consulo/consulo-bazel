package org.jetbrains.bazel.sync.environment;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public interface BazelProjectInfoService {
    boolean isBazelProject();

    @Nullable VirtualFile getProjectRootDir();

    // consulo: upstream Kotlin extension property Project.bazelProjectInfo
    static BazelProjectInfoService getBazelProjectInfo(Project receiver) {
        return receiver.getInstance(BazelProjectInfoService.class);
    }

    // consulo: upstream Kotlin extension function BazelProjectInfoService.getProjectRootDirOrThrow()
    static VirtualFile getProjectRootDirOrThrow(BazelProjectInfoService receiver) {
        VirtualFile projectRootDir = receiver.getProjectRootDir();
        if (projectRootDir == null) {
            throw new IllegalStateException("Project root directory is not initialized");
        }
        return projectRootDir;
    }
}
