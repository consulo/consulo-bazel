package org.jetbrains.bazel.progress;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

@ServiceAPI(ComponentScope.PROJECT)
public interface ConsoleService {
    TaskConsole getBuildConsole();

    TaskConsole getSyncConsole();

    static ConsoleService getInstance(Project project) {
        return project.getInstance(ConsoleService.class);
    }
}
