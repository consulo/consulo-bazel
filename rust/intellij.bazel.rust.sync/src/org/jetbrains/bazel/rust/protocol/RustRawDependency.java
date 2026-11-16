// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustRawDependency.kt (removed in 4699fc65a)
package org.jetbrains.bazel.rust.protocol;

import org.jspecify.annotations.Nullable;

import java.util.Set;

public record RustRawDependency(
    String name,
    boolean optional,
    boolean usesDefaultFeatures,
    Set<String> features,
    @Nullable String rename,
    @Nullable String kind,
    @Nullable String target
) {
    // consulo: overload for the upstream default arguments
    public RustRawDependency(String name, boolean optional, boolean usesDefaultFeatures, Set<String> features) {
        this(name, optional, usesDefaultFeatures, features, null, null, null);
    }
}
