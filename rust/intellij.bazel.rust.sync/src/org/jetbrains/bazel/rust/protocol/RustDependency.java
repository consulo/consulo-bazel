// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustDependency.kt (removed in 4699fc65a)
package org.jetbrains.bazel.rust.protocol;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record RustDependency(
    String pkg,
    @Nullable String name,
    @Nullable List<RustDepKindInfo> depKinds
) {
    // consulo: overload for the upstream default arguments
    public RustDependency(String pkg) {
        this(pkg, null, null);
    }
}
