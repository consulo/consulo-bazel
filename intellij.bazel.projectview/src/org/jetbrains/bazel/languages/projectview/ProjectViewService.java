package org.jetbrains.bazel.languages.projectview;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@ServiceAPI(ComponentScope.PROJECT)
public interface ProjectViewService {
    // consulo: StateFlow replaced by current value
    ProjectView getProjectViewState();

    CompletableFuture<Void> forceReparseCurrentProjectViewFiles();

    default ProjectView getProjectView() {
        return getProjectViewState();
    }

    @Nullable Path getProjectViewPath();

    static ProjectViewService getInstance(Project project) {
        return project.getInstance(ProjectViewService.class);
    }

    // consulo: upstream Kotlin top-level extension functions of Project

    static ProjectView projectView(Project receiver) {
        return ProjectViewService.getInstance(receiver).getProjectView();
    }

    static @Nullable Path projectViewPath(Project receiver) {
        return ProjectViewService.getInstance(receiver).getProjectViewPath();
    }
}
