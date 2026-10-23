package org.jetbrains.bazel.languages.starlark.bazel;

import java.util.Map;

public enum BazelFileType {
    BUILD,
    EXTENSION,
    MODULE,
    WORKSPACE,
    ;

    public static BazelFileType ofFileName(String name) {
        return OF_FILE_NAME.getOrDefault(name, EXTENSION);
    }

    private static final Map<String, BazelFileType> OF_FILE_NAME =
        Map.of(
            "BUILD.bazel", BUILD,
            "BUILD", BUILD,
            "MODULE.bazel", MODULE,
            "WORKSPACE.bazel", WORKSPACE,
            "WORKSPACE", WORKSPACE
        );
}
