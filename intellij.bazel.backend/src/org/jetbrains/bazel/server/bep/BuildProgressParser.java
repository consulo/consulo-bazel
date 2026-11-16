package org.jetbrains.bazel.server.bep;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapted from {@code com.intellij.monorepo.devkit.bazel.BazelConsoleService.Output.BuildProgressOutput}
 */
public class BuildProgressParser {
    public record BuildProgress(@Nullable Double fraction, @Nullable String details) {
    }

    // Bazel streams progress in chunks. We surface whichever line is actually advancing:
    //  - while the "Loading:" / "Analyzing: N targets (... configured ...)" phase line keeps changing, show it;
    //  - once it freezes (analysis done, Bazel just checking cached actions) the phase line and the "[x / y]"
    //    fraction are both static, but the raw "[x / y]" counter still climbs, so show that whole line instead;
    //  - during real execution there's no phase line, so the "[x / y] N actions running" line with its fraction wins.
    private @Nullable String myLastPhaseLine = null;

    public @Nullable BuildProgress parse(List<String> lines) {
        ActionMatch action = null;
        String phaseInChunk = null;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String whitesapaceTrimmed = lines.get(i).trim();
            Matcher timestampMatch = TIMESTAMP_REGEX.matcher(whitesapaceTrimmed);
            String trimmed = timestampMatch.matches() ? timestampMatch.group("rest") : whitesapaceTrimmed;
            if (action == null) {
                action = parseActionLine(trimmed);
            }
            if (phaseInChunk == null && (trimmed.startsWith("Analyzing:") || trimmed.startsWith("Loading:"))) {
                phaseInChunk = trimmed;
            }
            if (action != null && phaseInChunk != null) {
                break;
            }
        }

        String previousPhase = myLastPhaseLine;
        if (phaseInChunk != null) {
            myLastPhaseLine = phaseInChunk;
        }

        if (phaseInChunk == null) {
            return action != null ? new BuildProgress(action.fraction(), action.details()) : null;
        }
        else if (phaseInChunk.equals(previousPhase) && action != null) {
            return new BuildProgress(null, action.line());
        }
        else {
            return new BuildProgress(null, phaseInChunk);
        }
    }

    private @Nullable ActionMatch parseActionLine(String trimmed) {
        Matcher match = PROGRESS_OUTPUT_REGEX.matcher(trimmed);
        if (!match.matches()) {
            return null;
        }
        long total = getProgressValue(match.group("total"), 1);
        long progress = Math.min(getProgressValue(match.group("progress"), 0), total - 1);
        return new ActionMatch(trimmed, match.group("details"), (double) progress / (double) total);
    }

    private long getProgressValue(@Nullable String text, long minValue) {
        if (text == null) {
            return minValue;
        }
        Long value = toLongOrNull(EVERYTHING_EXCEPT_DIGITS_REGEX.matcher(text).replaceAll(""));
        return value != null ? Math.max(value, minValue) : minValue;
    }

    // consulo: Kotlin String.toLongOrNull()
    private static @Nullable Long toLongOrNull(String text) {
        try {
            return Long.parseLong(text);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private record ActionMatch(String line, @Nullable String details, double fraction) {
    }

    private static final Pattern TIMESTAMP_REGEX =
        Pattern.compile("(?<timestamp>\\([0-9][0-9]:[0-9][0-9]:[0-9][0-9]\\) )(?<rest>.*)");
    private static final Pattern PROGRESS_OUTPUT_REGEX =
        Pattern.compile("\\[(?<progress>[.,0-9]+)\\s*/\\s*(?<total>[.,0-9]+)]\\s*(?<details>.*)");
    private static final Pattern EVERYTHING_EXCEPT_DIGITS_REGEX = Pattern.compile("[^0-9]");
}
