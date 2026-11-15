package org.jetbrains.bazel.languages.bazelquery.options;

import org.jspecify.annotations.Nullable;

import java.util.List;

// TODO: refactoring to reuse code from bazelrc KnownFlags - BAZEL-2029
public final class BazelQueryCommonOptions {
    // consulo: upstream public top-level data class of BazelQueryCommonOptions.kt
    public record BazelQueryOption(String name, List<String> values) {
        // consulo: upstream default argument
        public BazelQueryOption(String name) {
            this(name, List.of());
        }
    }

    private BazelQueryCommonOptions() {
    }

    // Default value is always first in the list
    private static final List<BazelQueryOption> KNOWN_FLAGS =
        List.of(
            new BazelQueryOption(
                "output",
                List.of(
                    "label",
                    "label_kind",
                    "build",
                    "minrank",
                    "maxrank",
                    "package",
                    "location",
                    "graph",
                    "xml",
                    "proto",
                    "streamed_jsonproto",
                    "streamed_proto"
                )
            ),
            new BazelQueryOption(
                "keep_going"
            ),
            new BazelQueryOption(
                "aspect_deps",
                List.of("conservative", "precise", "off")
            ),
            new BazelQueryOption(
                "nonodep_deps"
            ),
            new BazelQueryOption(
                "consistent_labels"
            ),
            new BazelQueryOption(
                "relative_locations"
            ),
            new BazelQueryOption(
                "order_output",
                List.of("auto", "no", "deps", "full")
            ),
            new BazelQueryOption(
                "noimplicit_deps"
            ),
            new BazelQueryOption(
                "nofetch"
            )
        );

    public static @Nullable BazelQueryOption byName(String name) {
        return KNOWN_FLAGS.stream().filter(it -> it.name().equals(name)).findFirst().orElse(null);
    }

    public static List<BazelQueryOption> getAll() {
        return KNOWN_FLAGS;
    }
}
