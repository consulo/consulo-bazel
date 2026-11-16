// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustTargetKind.kt (removed in 4699fc65a)
package org.jetbrains.bazel.rust.protocol;

public enum RustTargetKind {
    LIB(1),
    BIN(2),
    TEST(3),
    EXAPMLE(4),
    BENCH(5),
    CUSTOM_BUILD(6),
    UNKNOWN(7);

    private final int myValue;

    RustTargetKind(int value) {
        myValue = value;
    }

    public int getValue() {
        return myValue;
    }
}
