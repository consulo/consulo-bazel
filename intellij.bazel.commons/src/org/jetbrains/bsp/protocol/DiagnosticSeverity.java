package org.jetbrains.bsp.protocol;

public enum DiagnosticSeverity {
    ERROR(1),
    WARNING(2),
    INFORMATION(3),
    HINT(4),
    ;

    private final int myValue;

    DiagnosticSeverity(int value) {
        myValue = value;
    }

    public int getValue() {
        return myValue;
    }
}
