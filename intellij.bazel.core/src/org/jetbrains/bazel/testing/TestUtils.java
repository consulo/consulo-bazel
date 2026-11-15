package org.jetbrains.bazel.testing;

public final class TestUtils {
    private TestUtils() {
    }

    public static final String IS_IN_IDE_STARTER_TEST = "is.in.ide.starter.test";

    public static boolean isInIdeStarterTest() {
        return Boolean.parseBoolean(System.getProperty(IS_IN_IDE_STARTER_TEST));
    }
}
