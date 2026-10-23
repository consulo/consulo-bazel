package org.jetbrains.bazel.commons;

import consulo.platform.Platform;

public final class ExecUtils {
    private ExecUtils() {
    }

    public static String calculateExecutableName(String name) {
        if (Platform.current().os().isWindows()) {
            return name + ".exe";
        }
        else {
            return name;
        }
    }
}
