package org.jetbrains.bazel.sync;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@ServiceAPI(ComponentScope.PROJECT)
public interface ProjectSyncService {
    @Nullable TaskId getLastSyncTaskId();

    /**
     * Runs a sync and returns how it ended. The result is {@link ProjectSyncOutcome.ProjectSyncCompletionResult#SKIPPED} when another sync
     * is running and when the project is not trusted.
     */
    CompletableFuture<ProjectSyncOutcome> sync(ProjectSyncScope scope);
}
