// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustDepKind.kt (removed in 4699fc65a)
package org.jetbrains.bazel.rust.protocol;

public final class RustDepKind {
    private RustDepKind() {
    }

    public static final String BUILD = "build";
    public static final String DEV = "dev";
    public static final String NORMAL = "normal";
    public static final String UNCLASSIFIED = "unclassified";
}
