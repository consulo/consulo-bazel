package org.jetbrains.bazel.commons;

/** The general type of a rule (e.g. test, binary, etc.).  */
public enum RuleType {
    TEST,
    BINARY,
    LIBRARY,
    UNKNOWN,
}
