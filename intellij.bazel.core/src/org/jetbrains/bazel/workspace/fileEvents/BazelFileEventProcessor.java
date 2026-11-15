package org.jetbrains.bazel.workspace.fileEvents;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.event.VFileEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// consulo: BazelFileEventProcessorResult and DefaultBazelFileEventProcessor, declared in this upstream file, have their own files
@ServiceAPI(ComponentScope.PROJECT)
public interface BazelFileEventProcessor {
    /**
     * Return true if events were queued and processed
     */
    // consulo: upstream `suspend fun enqueue(): Deferred<BazelFileEventProcessorResult>`; the returned future completes when
    // the queued events are processed
    CompletableFuture<BazelFileEventProcessorResult> enqueue(List<VFileEvent> events);

    /**
     * Check if queue is empty and no events are being processed
     */
    boolean isIdle();

    static BazelFileEventProcessor getInstance(Project project) {
        return project.getInstance(BazelFileEventProcessor.class);
    }
}
