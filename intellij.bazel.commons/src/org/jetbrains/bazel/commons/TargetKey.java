package org.jetbrains.bazel.commons;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.jetbrains.bazel.label.Label;

import java.util.List;

/** A key that uniquely identifies a target in the target map */
record TargetKey(Label label, List<String> aspectIds) implements Comparable<TargetKey> {
    @Override
    public int compareTo(TargetKey other) {
        return ComparisonChain
            .start()
            .compare(label, other.label)
            .compare(aspectIds, other.aspectIds, Ordering.<String>natural().lexicographical())
            .result();
    }

    public boolean isPlainTarget() {
        return aspectIds.isEmpty();
    }
}
