package org.jetbrains.bazel.sync.environment;

import consulo.annotation.component.ServiceImpl;
import consulo.bazel.core.BazelProjectStoreService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

@ServiceImpl
@Singleton
public class IJBazelProjectInfoService implements BazelProjectInfoService {
    private final Project myProject;

    @Inject
    public IJBazelProjectInfoService(Project project) {
        myProject = project;
    }

    @Override
    public boolean isBazelProject() {
        // consulo: the Bazel store descriptor is persisted by BazelProjectStoreService (upstream project.stateStoreOrNull?.storeDescriptor)
        return BazelProjectStoreService.getStoreDescriptor(myProject) != null;
    }

    @Override
    public @Nullable VirtualFile getProjectRootDir() {
        BazelProjectStoreDescriptor bazelProjectStoreDescriptor = BazelProjectStoreService.getStoreDescriptor(myProject);
        VirtualFile historicalProjectBaseDir = bazelProjectStoreDescriptor != null
            ? PathUtil.findVirtualFile(bazelProjectStoreDescriptor.getHistoricalProjectBasePath())
            : null;
        if (historicalProjectBaseDir != null) {
            return historicalProjectBaseDir;
        }
        //?: project.base
        // Fallback for running a Bazel run configuration in a non-Bazel project
        // consulo: Consulo has no Project.guessProjectDir(), the project base directory is used
        return myProject.getBaseDir();
    }

    // consulo: the Bazel store descriptor is persisted by BazelProjectStoreService, the upstream Project.stateStoreOrNull
    // extension property (IProjectStore / ProjectStoreOwner) is dropped
}
