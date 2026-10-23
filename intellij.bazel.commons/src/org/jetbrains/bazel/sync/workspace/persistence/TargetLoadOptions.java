package org.jetbrains.bazel.sync.workspace.persistence;

import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Describes which sections of a target are loaded eagerly.
 * Sections not loaded eagerly will be loaded at access time.
 *
 * @param sections   Eagerly loaded sections, everything not listed here is loaded lazily
 * @param targetData Select what {@link BuildTargetData} types to load eagerly
 */
public record TargetLoadOptions(
    Set<TargetSection> sections,
    BuildTargetLoadHint targetData
) {
    /**
     * Independently loadable section of a target
     */
    public enum TargetSection {
        // generator name, `isTestOnly`, tags
        INFO,

        // target deps
        DEPS,

        // file collections like: sources, generated sources, etc.
        FILE_SETS,
    }

    public sealed interface BuildTargetLoadHint permits BuildTargetLoadHint.All, BuildTargetLoadHint.None, BuildTargetLoadHint.Subset {
        final class All implements BuildTargetLoadHint {
            public static final All INSTANCE = new All();

            private All() {
            }

            // consulo: data object equality
            @Override
            public boolean equals(@Nullable Object o) {
                return o instanceof All;
            }

            @Override
            public int hashCode() {
                return All.class.getName().hashCode();
            }

            @Override
            public String toString() {
                return "All";
            }
        }

        final class None implements BuildTargetLoadHint {
            public static final None INSTANCE = new None();

            private None() {
            }

            // consulo: data object equality
            @Override
            public boolean equals(@Nullable Object o) {
                return o instanceof None;
            }

            @Override
            public int hashCode() {
                return None.class.getName().hashCode();
            }

            @Override
            public String toString() {
                return "None";
            }
        }

        record Subset(Set<Class<? extends BuildTargetData>> data) implements BuildTargetLoadHint {
        }
    }

    // entire target
    public static final TargetLoadOptions ALL = new TargetLoadOptions(EnumSet.allOf(TargetSection.class), BuildTargetLoadHint.All.INSTANCE);

    // only target summary
    public static final TargetLoadOptions SUMMARY = new TargetLoadOptions(Set.of(TargetSection.INFO), BuildTargetLoadHint.None.INSTANCE);

    // only target key
    public static final TargetLoadOptions MINIMAL = new TargetLoadOptions(Set.of(), BuildTargetLoadHint.None.INSTANCE);

    // consulo: upstream top-level factory function TargetLoadOptions(vararg sections, targetData = BuildTargetLoadHint.None)
    public TargetLoadOptions(TargetSection... sections) {
        this(BuildTargetLoadHint.None.INSTANCE, sections);
    }

    // consulo: varargs must be last, so targetData comes first
    public TargetLoadOptions(BuildTargetLoadHint targetData, TargetSection... sections) {
        this(new LinkedHashSet<>(Arrays.asList(sections)), targetData);
    }
}
