package org.jetbrains.bazel.sync;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.sync.task.ProjectSyncTask;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@ServiceImpl
@Singleton
public class DefaultProjectSyncService implements ProjectSyncService {
    private final Project myProject;

    private volatile @Nullable TaskId myLastSyncTaskId = null;

    @Inject
    public DefaultProjectSyncService(Project project) {
        myProject = project;
    }

    @Override
    public @Nullable TaskId getLastSyncTaskId() {
        return myLastSyncTaskId;
    }

    @Override
    public CompletableFuture<ProjectSyncOutcome> sync(ProjectSyncScope scope) {
        ProjectSyncTask task = new ProjectSyncTask(
            /* project = */ myProject,
            /* scope = */ scope,
            /* onSyncTaskStarted = */ it -> myLastSyncTaskId = it
        );
        return task.sync().thenApply(outcome -> {
            ProjectDirtyStateService.getInstance(myProject).finishSync(scope);
            return outcome;
        });
    }
}
