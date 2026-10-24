package org.jetbrains.bazel.server.bep;

import java.util.regex.Pattern;

/** Strips ANSI color/cursor escape sequences from Bazel console output (Bazel emits them with `--color=true`). */
final class AnsiEscapeCodes {
    private static final Pattern ESCAPE_SEQUENCE = Pattern.compile("\\u001B\\[[\\d;]*[^\\d;]");
    private static final char ESC = (char) 27; // ESC (0x1B), built from its code point to keep the source ASCII-only

    private AnsiEscapeCodes() {
    }

    /** Removes ANSI escape sequences. Cheap: skips the regex (and its per-line Matcher) when no escape char is present. */
    public static String strip(String line) {
        return line.indexOf(ESC) < 0 ? line : ESCAPE_SEQUENCE.matcher(line).replaceAll("");
    }
}
