package org.jetbrains.bazel.sync.task;

import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.List;

// consulo: holder class for the top-level declarations of upstream SyncWorkspaceProvider.kt
public final class SyncWorkspaceProvider {
    private SyncWorkspaceProvider() {
    }

    public enum SyncPhase {
        FIRST,
        SECOND,
    }

    public record SyncWorkspaceContext(
        SyncPhase phase,
        boolean buildProject,
        @Nullable List<Label> allKnownTargets,
        @Nullable RepoMapping knownRepoMapping,
        BazelServerFacade server,
        TaskId taskId
    ) {
    }

    public enum SyncWorkspaceStatus {
        SUCCESS,

        // partial error
        PARTIAL,

        // error and no targets
        FATAL,
    }
}
