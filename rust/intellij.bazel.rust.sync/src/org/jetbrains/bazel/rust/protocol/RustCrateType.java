// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustCrateType.kt (removed in 4699fc65a)
package org.jetbrains.bazel.rust.protocol;

public enum RustCrateType {
    BIN(1),
    LIB(2),
    RLIB(3),
    DYLIB(4),
    CDYLIB(5),
    STATICLIB(6),
    PROC_MACRO(7),
    UNKNOWN(8);

    private final int myValue;

    RustCrateType(int value) {
        myValue = value;
    }

    public int getValue() {
        return myValue;
    }
}
