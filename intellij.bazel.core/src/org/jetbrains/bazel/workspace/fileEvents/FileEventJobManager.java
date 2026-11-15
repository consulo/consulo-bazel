package org.jetbrains.bazel.workspace.fileEvents;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.sync.status.SyncStatusListener;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class FileEventJobManager {
    private final Project myProject;

    // consulo: kotlinx Job -> CompletableFuture
    private final AtomicReference<@Nullable CompletableFuture<?>> myFileEventProcessingJob = new AtomicReference<>(null);

    @Inject
    public FileEventJobManager(Project project) {
        myProject = project;
        project.getMessageBus().connect().subscribe(SyncStatusListener.TOPIC, new StatusListener());
    }

    public <T extends CompletableFuture<?>> @Nullable T runFileEventsProcessing(Supplier<T> jobStarter) {
        SyncStatusService syncStatusService = SyncStatusService.getInstance(myProject);
        // consulo: Optional instead of a nullable result of withSyncStatus
        T job = syncStatusService
            .withSyncStatus(syncInProgress -> Optional.ofNullable(!syncInProgress ? jobStarter.get() : null))
            .orElse(null);
        if (job == null) {
            return null;
        }
        setJobAndCancelPrevious(job);
        return job;
    }

    private void cancelCurrentJob() {
        setJobAndCancelPrevious(null);
    }

    private void setJobAndCancelPrevious(@Nullable CompletableFuture<?> job) {
        CompletableFuture<?> previous = myFileEventProcessingJob.getAndSet(job);
        if (previous != null) {
            previous.cancel(true);
        }
    }

    public static FileEventJobManager getInstance(Project project) {
        return project.getInstance(FileEventJobManager.class);
    }

    private class StatusListener implements SyncStatusListener {
        @Override
        public void syncStarted() {
            FileEventJobManager jobRunner = FileEventJobManager.this;
            jobRunner.cancelCurrentJob();
        }

        @Override
        public void syncFinished(boolean canceled) {
        }
    }
}
