package org.jetbrains.bazel.commons;

import org.jspecify.annotations.Nullable;

public enum ShardingApproach {
    EXPAND_AND_SHARD, // expand wildcard targets to package targets, query single targets, and then shard to batches
    QUERY_AND_SHARD, // query single targets from the given list of targets, and then shard to batches
    SHARD_ONLY, // split unexpanded wildcard targets into batches
    ;

    public static @Nullable ShardingApproach fromString(@Nullable String rawValue) {
        for (ShardingApproach it : ShardingApproach.values()) {
            if (it.name().equalsIgnoreCase(rawValue)) {
                return it;
            }
        }
        return null;
    }
}
