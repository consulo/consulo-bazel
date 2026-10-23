package org.jetbrains.bazel.commons.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Constants {
    private Constants() {
    }

    public static final String BAZEL_DISPLAY_NAME = "Bazel";
    public static final String NAME = "bazelbsp";
    public static final String VERSION = "3.2.0";
    public static final String BAZEL_BUILD_COMMAND = "build";
    public static final String BAZEL_TEST_COMMAND = "test";
    public static final String BAZEL_COVERAGE_COMMAND = "coverage";
    public static final String ASPECT_INFO_OUTPUT_EXTENSION = ".intellij-info.txt";
    public static final String DOT_BAZELBSP_DIR_NAME = ".bazelbsp";
    public static final String SYNTHETIC_TARGETS_DIR_NAME = "synthetic_targets";

    public static final String WORKSPACE_FILE_NAME = "WORKSPACE";
    public static final String MODULE_BAZEL_FILE_NAME = "MODULE.bazel";
    public static final String MODULE_BAZEL_LOCK_FILE_NAME = "MODULE.bazel.lock";
    public static final String BAZEL_VERSION_FILE_NAME = ".bazelversion";
    public static final String BAZELISK_VERSION_FILE_NAME = ".bazeliskversion";
    public static final String BAZELISK_RC_FILE_NAME = ".bazeliskrc";
    public static final String BAZEL_RC_FILE_NAME = ".bazelrc";
    public static final String BAZEL_IGNORE_FILE_NAME = ".bazelignore";
    public static final String REPO_BAZEL_FILE_NAME = "REPO.bazel";
    public static final String[] WORKSPACE_FILE_NAMES =
        {MODULE_BAZEL_FILE_NAME, WORKSPACE_FILE_NAME, "WORKSPACE.bazel", "WORKSPACE.bzlmod"};
    public static final String[] BUILD_FILE_NAMES = {"BUILD.bazel", "BUILD"};
    public static final List<String> BAZELISK_FILE_NAMES = List.of(BAZEL_VERSION_FILE_NAME, BAZELISK_RC_FILE_NAME);
    public static final String[] SUPPORTED_EXTENSIONS = {"bazel", "bazelproject", "bzlmod", "bzl"};
    public static final String[] SUPPORTED_CONFIG_FILE_NAMES = supportedConfigFileNames();

    // consulo: upstream WORKSPACE_FILE_NAMES + BUILD_FILE_NAMES + BAZELISK_FILE_NAMES
    private static String[] supportedConfigFileNames() {
        List<String> result = new ArrayList<>(Arrays.asList(WORKSPACE_FILE_NAMES));
        result.addAll(Arrays.asList(BUILD_FILE_NAMES));
        result.addAll(BAZELISK_FILE_NAMES);
        return result.toArray(String[]::new);
    }

    public static String defaultBuildFileName() {
        return BUILD_FILE_NAMES[0];
    }

    public static final String PROJECT_VIEW_FILE_EXTENSION = "bazelproject";
    public static final String DEFAULT_PROJECT_VIEW_FILE_NAME = ".bazelproject";
    public static final String LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME = "projectview.bazelproject";

    public static final String NO_IDE = "no-ide";

    public static final List<String> JVM_LANGUAGES_EXTENSIONS = List.of("java", "kt", "scala");
}
