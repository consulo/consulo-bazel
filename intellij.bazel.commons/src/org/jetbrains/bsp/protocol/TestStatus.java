package org.jetbrains.bsp.protocol;

public enum TestStatus {
    PASSED(1),
    FAILED(2),
    IGNORED(3),
    CANCELLED(4),
    SKIPPED(5),
    ;

    private final int myValue;

    TestStatus(int value) {
        myValue = value;
    }

    public int getValue() {
        return myValue;
    }
}
