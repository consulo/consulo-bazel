package org.jetbrains.bazel.ui.console;

import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.BuildViewManager;
import consulo.build.ui.SyncViewManager;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.progress.ConsoleService;
import org.jetbrains.bazel.progress.TaskConsole;

@ServiceImpl
@Singleton
public class DefaultConsoleService implements ConsoleService {
    private final TaskConsole myBuildConsole;

    private final TaskConsole mySyncConsole;

    @Inject
    public DefaultConsoleService(Project project, BuildViewManager buildViewManager, SyncViewManager syncViewManager) {
        String basePath = org.jetbrains.bazel.config.project.getRootDir(project).getPath();

        myBuildConsole =
            new BuildTaskConsole(
                buildViewManager,
                basePath,
                project
            );
        mySyncConsole =
            new SyncTaskConsole(
                syncViewManager,
                basePath,
                project
            );
    }

    @Override
    public TaskConsole getBuildConsole() {
        return myBuildConsole;
    }

    @Override
    public TaskConsole getSyncConsole() {
        return mySyncConsole;
    }
}
