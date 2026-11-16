package org.jetbrains.bazel.languages.starlark.globbing;

import org.jspecify.annotations.Nullable;

/**
 * Support for resolving globs.
 */
public final class GlobPatternValidator {
    private GlobPatternValidator() {
    }

    /**
     * Validate a single glob pattern. If it's invalid, returns an error message. Otherwise, returns
     * null.
     */
    public static @Nullable String validate(String pattern) {
        String error = checkPatternForError(pattern);
        if (error != null) {
            return "Invalid glob pattern: " + error;
        }
        return null;
    }

    private static @Nullable String checkPatternForError(String pattern) {
        if (pattern.isEmpty()) {
            return "pattern cannot be empty";
        }
        if (pattern.charAt(0) == '/') {
            return "pattern cannot be absolute";
        }
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '(', ')', '{', '}', '[', ']' -> {
                    return "illegal character '" + c + "'";
                }
                default -> {
                }
            }
        }
        // consulo: limit -1 keeps the trailing empty segments, like Kotlin split('/')
        String[] segments = pattern.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty()) {
                return "empty segment not permitted";
            }
            if (segment.equals(".") || segment.equals("..")) {
                return "segment '" + segment + "' not permitted";
            }
            if (segment.contains("**") && !segment.equals("**")) {
                return "recursive wildcard must be its own segment";
            }
        }
        return null;
    }
}
