package org.jetbrains.bazel.sync.task;

import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.sync.ProjectSyncOutcome;
import org.jetbrains.bazel.sync.ProjectSyncOutcome.ProjectSyncCompletionResult;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public record ProjectSyncResult(
    ProjectSyncCompletionResult completionResult,
    @Nullable ProjectSyncStatistics statistics,
    List<ProjectSyncPhaseDuration> phaseDurations,
    @Nullable Throwable failureCause
) {
    // consulo: overloads for the upstream default arguments
    public ProjectSyncResult(ProjectSyncCompletionResult completionResult) {
        this(completionResult, null, List.of(), null);
    }

    public ProjectSyncResult(ProjectSyncCompletionResult completionResult, @Nullable Throwable failureCause) {
        this(completionResult, null, List.of(), failureCause);
    }

    public ProjectSyncOutcome toOutcome() {
        return new ProjectSyncOutcome(completionResult, failureCause);
    }

    public enum ProjectSyncPhase {
        COLLECT_PROJECT_DETAILS,
        APPLY_PROJECT_MODEL,
    }

    public record ProjectSyncPhaseDuration(
        ProjectSyncPhase phase,
        long durationMs
    ) {
    }

    public record ProjectSyncStatistics(
        int resolvedTargetCount,
        List<Set<LanguageClass>> targetLanguageClasses
    ) {
    }

    public static <T> CompletableFuture<T> trackSyncPhase(
        List<ProjectSyncPhaseDuration> receiver,
        ProjectSyncPhase phase,
        Supplier<CompletableFuture<T>> action
    ) {
        long startedAt = System.currentTimeMillis();
        CompletableFuture<T> result;
        try {
            result = action.get();
        }
        catch (Throwable e) {
            result = CompletableFuture.failedFuture(e);
        }
        // consulo: the upstream finally block runs when the future completes
        return result.whenComplete(
            (value, error) -> receiver.add(new ProjectSyncPhaseDuration(phase, System.currentTimeMillis() - startedAt))
        );
    }

    public static ProjectSyncStatistics syncStatistics(List<BuildTarget> receiver) {
        List<Set<LanguageClass>> targetLanguageClasses = receiver.stream().map(it -> it.getKind().languageClasses()).toList();
        return new ProjectSyncStatistics(receiver.size(), targetLanguageClasses);
    }
}
