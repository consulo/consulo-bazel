package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SourcePatternEval {
    private SourcePatternEval() {
    }

    public record PatternEvalResult<T>(List<T> included, List<T> excluded) {
    }

    public static <T> PatternEvalResult<T> eval(
        Iterable<T> items,
        List<Predicate<T>> includes,
        List<Predicate<T>> excludes
    ) {
        List<T> matched = new ArrayList<>();
        List<T> unmatched = new ArrayList<>();
        for (T item : items) {
            boolean excluded;
            if (includes.isEmpty()) {
                excluded = false;
            }
            else {
                excluded = excludes.stream().anyMatch(it -> it.test(item));
            }
            if (excluded) {
                unmatched.add(item);
            }
            else {
                if (includes.stream().anyMatch(it -> it.test(item))) {
                    matched.add(item);
                }
                else {
                    unmatched.add(item);
                }
            }
        }
        return new PatternEvalResult<>(matched, unmatched);
    }
}
