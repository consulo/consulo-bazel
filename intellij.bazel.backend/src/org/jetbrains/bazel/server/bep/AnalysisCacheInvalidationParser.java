package org.jetbrains.bazel.server.bep;

import org.jetbrains.bsp.protocol.BazelInvocationMetrics.AnalysisCacheInvalidation;
import org.jetbrains.bsp.protocol.BazelInvocationMetrics.AnalysisCacheInvalidationCause;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects a discard of Bazel's in-server (Skyframe) analysis cache from a Bazel invocation's stderr.
 * <p>
 * Bazel prints `... discarding analysis cache` (e.g. `WARNING: Build option --X has changed, discarding analysis
 * cache.`) to its process stderr when it drops the cache, forcing a full re-analysis. This is parsed from the bazel
 * client's stderr rather than from BEP progress events, since that is the authoritative capture of console output.
 */
public final class AnalysisCacheInvalidationParser {
    private static final String DISCARD_MARKER = "discarding analysis cache";
    private static final Pattern OPTION_NAME_PATTERN = Pattern.compile("--[\\w-]+");

    private AnalysisCacheInvalidationParser() {
    }

    // consulo: upstream default value of stdoutLines
    public static @Nullable AnalysisCacheInvalidation parse(List<String> stderrLines) {
        return parse(stderrLines, List.of());
    }

    /**
     * Returns the first analysis-cache discard detected in {@code stderrLines} or {@code stdoutLines}, or `null` if none found.
     * <p>
     * Both sources are needed because the capture stream depends on the process mode:
     * - Non-PTY (`--curses=false`): Bazel writes warnings to its process stderr → {@code stderrLines} has the message.
     * - PTY (`--curses=true`, e.g. when a terminal is attached): the OS merges stdout and stderr into one PTY stream;
     *   `process.errorStream` is empty and all output goes to `process.inputStream` → {@code stdoutLines} has the message.
     * <p>
     * In PTY mode, `\r` is used to overwrite progress fragments within a single stdout chunk; it is treated as
     * whitespace before the marker check.
     */
    public static @Nullable AnalysisCacheInvalidation parse(List<String> stderrLines, List<String> stdoutLines) {
        List<String> lines = new ArrayList<>(stderrLines);
        lines.addAll(stdoutLines);
        for (String raw : lines) {
            // Strip ANSI color/cursor codes (always present with --color=true, sometimes with cursor-control in PTY mode).
            // In PTY mode, \r separates overwritten progress fragments within a single line — treat them as whitespace.
            String line = AnsiEscapeCodes.strip(raw).replace('\r', ' ');
            String lower = line.toLowerCase(Locale.ROOT);
            if (!lower.contains(DISCARD_MARKER)) {
                continue;
            }

            AnalysisCacheInvalidationCause cause;
            if (lower.contains("memory")) {
                cause = AnalysisCacheInvalidationCause.MEMORY_PRESSURE;
            }
            else if (lower.contains("discard_analysis_cache") || lower.contains("as requested")) {
                cause = AnalysisCacheInvalidationCause.EXPLICIT_DISCARD;
            }
            else if (lower.contains("option")) {
                cause = AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED;
            }
            else {
                cause = AnalysisCacheInvalidationCause.UNKNOWN;
            }
            Integer changedOptionCount;
            if (cause == AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED) {
                int count = 0;
                Matcher matcher = OPTION_NAME_PATTERN.matcher(line);
                while (matcher.find()) {
                    count++;
                }
                changedOptionCount = count > 0 ? count : null;
            }
            else {
                changedOptionCount = null;
            }
            return new AnalysisCacheInvalidation(cause, changedOptionCount);
        }
        return null;
    }
}
