package org.jetbrains.bazel.server.bep;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import consulo.util.lang.StringUtil;
import org.jetbrains.bsp.protocol.BazelInvocationMetrics.BazelActionCacheStats;
import org.jspecify.annotations.Nullable;

import java.util.function.ToLongFunction;

// consulo: holder class for the top-level declarations of upstream BazelInvocationMetricsMapper.kt
public final class BazelInvocationMetricsMapper {
    private BazelInvocationMetricsMapper() {
    }

    /**
     * Extracts local/remote cache effectiveness from a Bazel `BuildMetrics` BEP event.
     * Remote and disk cache hits are taken from per-strategy {@link BuildEventStreamProtos.BuildMetrics.ActionSummary.RunnerCount}
     * entries; local action-cache hits come from `ActionCacheStatistics`.
     */
    public static BazelActionCacheStats toActionCacheStats(BuildEventStreamProtos.BuildMetrics receiver) {
        BuildEventStreamProtos.BuildMetrics.ActionSummary summary = receiver.getActionSummary();
        ToLongFunction<String> runnerSum = needle ->
            summary.getRunnerCountList()
                .stream()
                .filter(it -> StringUtil.containsIgnoreCase(it.getName(), needle))
                .mapToLong(it -> (long) it.getCount())
                .sum();
        return new BazelActionCacheStats(
            summary.getActionsCreated(),
            summary.getActionsExecuted(),
            (long) summary.getActionCacheStatistics().getHits(),
            runnerSum.applyAsLong("remote cache hit"),
            runnerSum.applyAsLong("disk cache hit")
        );
    }

    /** Wall time spent in the analysis phase, in milliseconds, or `null` when not reported. */
    public static @Nullable Long analysisPhaseTimeMsOrNull(BuildEventStreamProtos.BuildMetrics receiver) {
        if (receiver.hasTimingMetrics()) {
            long analysisPhaseTimeInMs = receiver.getTimingMetrics().getAnalysisPhaseTimeInMs();
            return analysisPhaseTimeInMs > 0 ? analysisPhaseTimeInMs : null;
        }
        else {
            return null;
        }
    }
}
