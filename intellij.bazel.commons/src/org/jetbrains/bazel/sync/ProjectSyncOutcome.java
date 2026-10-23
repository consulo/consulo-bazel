package org.jetbrains.bazel.sync;

import org.jspecify.annotations.Nullable;

/**
 * How a sync ended. {@code failureCause} is set when the sync ended with an exception.
 */
public record ProjectSyncOutcome(
    ProjectSyncCompletionResult completionResult,
    @Nullable Throwable failureCause
) {
    // consulo: upstream declares this enum as a top-level declaration in the same file
    public enum ProjectSyncCompletionResult {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILURE,
        CANCELLED,
        SKIPPED,
    }

    // consulo: upstream default argument
    public ProjectSyncOutcome(ProjectSyncCompletionResult completionResult) {
        this(completionResult, null);
    }

    /**
     * A partial success counts as a success: the project model was updated, and some targets have errors.
     */
    public boolean isSuccessful() {
        return completionResult == ProjectSyncCompletionResult.SUCCESS || completionResult == ProjectSyncCompletionResult.PARTIAL_SUCCESS;
    }

    /**
     * Throws {@link IllegalStateException} with the failure cause when the sync did not end in a success.
     */
    public void checkSuccessful() {
        if (!isSuccessful()) {
            // A skipped sync writes nothing to the Sync tab.
            String reason;
            if (failureCause != null) {
                String message = failureCause.getMessage();
                reason = ": " + (message != null ? message : failureCause.getClass().getName());
            }
            else if (completionResult == ProjectSyncCompletionResult.SKIPPED) {
                reason = ". The sync did not run, because another sync was in progress or the project is not trusted";
            }
            else {
                reason = ". See the Sync tab for the cause";
            }
            throw new IllegalStateException("The sync ended with " + completionResult + reason, failureCause);
        }
    }
}
