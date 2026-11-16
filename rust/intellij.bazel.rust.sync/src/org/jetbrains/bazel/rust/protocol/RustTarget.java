// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustTarget.kt (removed in 4699fc65a)
package org.jetbrains.bazel.rust.protocol;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public record RustTarget(
    String name,
    String crateRootUrl,
    RustTargetKind kind,
    String edition,
    boolean doctest,
    @Nullable List<RustCrateType> crateTypes,
    @Nullable Set<String> requiredFeatures
) {
}
