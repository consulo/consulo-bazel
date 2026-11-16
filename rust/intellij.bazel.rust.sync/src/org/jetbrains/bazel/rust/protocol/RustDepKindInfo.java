// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustDepKindInfo.kt (removed in 4699fc65a)
package org.jetbrains.bazel.rust.protocol;

import org.jspecify.annotations.Nullable;

public record RustDepKindInfo(String kind, @Nullable String target) {
    // consulo: overload for the upstream default argument
    public RustDepKindInfo(String kind) {
        this(kind, null);
    }
}
