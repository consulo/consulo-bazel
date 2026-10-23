package org.jetbrains.bsp.protocol;

import org.jspecify.annotations.Nullable;

/**
 * Metrics derived from a single Bazel invocation's Build Event Protocol stream, reported once per invocation.
 * Either of {@link #analysisCacheInvalidation} / {@link #actionCacheStats} may be {@code null} when the corresponding signal was
 * not observed (e.g. no discard happened, or no {@code BuildMetrics} event was emitted).
 */
public record BazelInvocationMetrics(
    BazelInvocationContext context,
    @Nullable AnalysisCacheInvalidation analysisCacheInvalidation,
    @Nullable BazelActionCacheStats actionCacheStats,
    @Nullable Long analysisPhaseTimeMs
) {
    /**
     * The kind of top-level user operation that triggered a Bazel invocation.
     * A single sync may fan out into several invocations; in that case all of them carry {@link #SYNC}.
     */
    public enum BazelInvocationContext {
        SYNC,
        BUILD,
        RUN,
        TEST,
        OTHER,
    }

    /**
     * Why Bazel discarded its in-server (Skyframe) analysis cache, forcing a full re-analysis.
     * Derived from the {@code ... discarding analysis cache} message Bazel prints to stderr.
     */
    public enum AnalysisCacheInvalidationCause {
        /**
         * Build options changed between two invocations on the same server (the common, expensive case).
         */
        BUILD_OPTIONS_CHANGED,

        /**
         * The cache was discarded on request (e.g. {@code --discard_analysis_cache}).
         */
        EXPLICIT_DISCARD,

        /**
         * Bazel dropped analysis state to relieve memory pressure.
         */
        MEMORY_PRESSURE,

        /**
         * The discard message was recognized but its reason could not be classified.
         */
        UNKNOWN,
    }

    /**
     * A detected discard of Bazel's analysis cache during a single invocation.
     *
     * @param changedOptionCount number of build options Bazel reported as changed, when the cause is
     *                           {@link AnalysisCacheInvalidationCause#BUILD_OPTIONS_CHANGED} and the names were listed; {@code null} otherwise.
     */
    public record AnalysisCacheInvalidation(
        AnalysisCacheInvalidationCause cause,
        @Nullable Integer changedOptionCount
    ) {
    }

    /**
     * Local/remote cache effectiveness for a single invocation, taken from the Bazel {@code BuildMetrics} BEP event.
     * {@code actionsExecuted} excludes local action-cache hits but includes remote cache hits (per Bazel's definition).
     */
    public record BazelActionCacheStats(
        long totalActions,
        long actionsExecuted,
        long localActionCacheHits,
        long remoteCacheHits,
        long diskCacheHits
    ) {
    }
}
