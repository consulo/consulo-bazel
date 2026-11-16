package org.jetbrains.bazel.languages.projectview;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@ServiceAPI(ComponentScope.PROJECT)
public interface ProjectViewService {

    ProjectView getProjectView();

    @Nullable Path getProjectViewPath();

    default CompletableFuture<Void> forceReparseCurrentProjectViewFiles() {
        return forceReparseCurrentProjectViewFiles(null);
    }

    CompletableFuture<Void> forceReparseCurrentProjectViewFiles(@Nullable Consumer<ProjectViewProblem> onEachProblem);

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
