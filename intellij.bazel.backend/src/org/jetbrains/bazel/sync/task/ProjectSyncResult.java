package org.jetbrains.bazel.sync.task;

import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.sync.ProjectSyncOutcome;
import org.jetbrains.bazel.sync.ProjectSyncOutcome.ProjectSyncCompletionResult;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ProjectSyncResult(
    ProjectSyncCompletionResult completionResult,
    @Nullable ProjectSyncStatistics statistics,
    @Nullable Throwable failureCause
) {
    // consulo: overloads for the upstream default arguments
    public ProjectSyncResult(ProjectSyncCompletionResult completionResult) {
        this(completionResult, null, null);
    }

    public ProjectSyncResult(ProjectSyncCompletionResult completionResult, @Nullable Throwable failureCause) {
        this(completionResult, null, failureCause);
    }

    public ProjectSyncOutcome toOutcome() {
        return new ProjectSyncOutcome(completionResult, failureCause);
    }

    public enum ProjectSyncPhase {
        COLLECT_PROJECT_DETAILS,
        APPLY_PROJECT_MODEL,
    }

    /**
     * @param resolvedTargetCount The number of resolved targets, or {@code null} when the sync stopped before it resolved the targets.
     * @param phaseDurations      The duration of each sync phase that ran, in milliseconds.
     */
    public record ProjectSyncStatistics(
        @Nullable Integer resolvedTargetCount,
        List<Set<LanguageClass>> targetLanguageClasses,
        Map<String, Long> importerDurations,
        Map<ProjectSyncPhase, Long> phaseDurations
    ) {
        // consulo: upstream default arguments
        public ProjectSyncStatistics() {
            this(null, List.of(), Map.of(), Map.of());
        }

        // consulo: Kotlin data class copy(phaseDurations = ...)
        public ProjectSyncStatistics withPhaseDurations(Map<ProjectSyncPhase, Long> phaseDurations) {
            return new ProjectSyncStatistics(resolvedTargetCount, targetLanguageClasses, importerDurations, phaseDurations);
        }
    }

    public ProjectSyncResult withPhaseDurations(Map<ProjectSyncPhase, Long> phaseDurations) {
        ProjectSyncStatistics current = statistics != null ? statistics : new ProjectSyncStatistics();
        return new ProjectSyncResult(
            completionResult,
            current.withPhaseDurations(new LinkedHashMap<>(phaseDurations)),
            failureCause
        );
    }
}
