package org.jetbrains.bazel.languages.projectview;

import consulo.logging.Logger;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class ProjectViewServiceBase implements ProjectViewService {
    private static final Logger log = Logger.getInstance(ProjectViewServiceBase.class);

    protected final Project myProject;

    private volatile ProjectView myProjectView = ProjectView.EMPTY;

    protected ProjectViewServiceBase(Project project) {
        myProject = project;
    }

    protected Project getProject() {
        return myProject;
    }

    @Override
    public final ProjectView getProjectView() {
        return myProjectView;
    }

    @Override
    public CompletableFuture<Void> forceReparseCurrentProjectViewFiles(@Nullable Consumer<ProjectViewProblem> onEachProblem) {
        // consulo: upstream suspend function, runs on the calling thread
        try {
            loadProjectView(getProjectViewPath(), onEachProblem);
            return CompletableFuture.completedFuture(null);
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // consulo: overload for the upstream default argument (onEachProblem = null)
    protected void loadProjectView(@Nullable Path path) {
        loadProjectView(path, null);
    }

    protected void loadProjectView(@Nullable Path path, @Nullable Consumer<ProjectViewProblem> onEachProblem) {
        if (path == null) {
            log.info("Project view path is missing. Falling back to the default project view.");
        }
        else {
            log.info("Using " + path + " project view file.");
        }
        ProjectView parsed = path != null ? ProjectViewFactory.fromOrNull(myProject, path, onEachProblem) : null;
        myProjectView = transformProjectView(parsed != null ? parsed : defaultProjectView());
    }

    protected ProjectView defaultProjectView() {
        return ProjectViewFactory.fromDefault(myProject);
    }

    protected ProjectView transformProjectView(ProjectView projectView) {
        return projectView;
    }
}
