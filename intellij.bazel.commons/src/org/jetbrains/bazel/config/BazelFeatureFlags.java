package org.jetbrains.bazel.config;

import consulo.bazel.commons.BazelRegistryKeys;

public final class BazelFeatureFlags {
    private BazelFeatureFlags() {
    }

    public static final String BUILD_PROJECT_ON_SYNC = "bazel.build.project.on.sync";
    private static final String SYNC_REQUEST_SOURCES = "bazel.sync.request.sources";
    private static final String SHORTEN_MODULE_LIBRARY_NAMES = "bsp.shorten.module.library.names";
    private static final String EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS = "bsp.exclude.compiled.source.code.inside.jars";
    private static final String SYMLINK_SCAN_MAX_DEPTH = "bazel.symlink.scan.max.depth";
    private static final String SHUTDOWN_BEFORE_SHARD_BUILD = "bazel.shutdown.before.shard.build";
    public static final String MERGE_SOURCE_ROOTS = "bazel.merge.source.roots";

    public static final String FAST_BUILD_ENABLED = "bazel.enable.jvm.fastbuild";
    public static final String AUTO_OPEN_PROJECT_IF_PRESENT = "bazel.project.auto.open.if.present";

    public static final String SYNTHETIC_RUN_ENABLE = "bazel.run.synthetic.enable";
    private static final String SYNTHETIC_RUN_DISABLE_VISIBILITY_CHECK = "bazel.run.synthetic.disable.visibility.check";

    public static final String RUN_CONFIG_RUN_WITH_BAZEL = "bazel.run.config.run.with.bazel";
    public static final String USE_PTY = "bazel.use.pty";
    private static final String SHOW_BAZEL_STDOUT = "bazel.show.stdout";
    private static final String HARD_LINK_OUTPUT_FILES = "bazel.hard.link.output.files";

    private static final String QUERY_ON_FILE_EVENTS = "bazel.query.on.file.events";
    private static final String HIGHLIGHT_UNSYNCED_SOURCE_FILES = "bazel.highlight.unsynced.source.files";
    private static final String HIGHLIGHT_IGNORED_SOURCE_FILES = "bazel.highlight.ignored.source.files";

    public static final String ENABLE_LOG = "bazel.enable.log";
    private static final String MASK_SECRETS_IN_LOG = "bazel.log.mask.secrets";
    private static final String KILL_TREE_ON_CANCEL = "bazel.kill.tree.on.cancel";
    private static final String KILL_SERVER_ON_CANCEL = "bazel.kill.server.on.cancel";

    public static boolean isBuildProjectOnSyncEnabled() {
        return isEnabled(BUILD_PROJECT_ON_SYNC);
    }

    public static boolean isSyncRequestSourcesEnabled() {
        return isEnabled(SYNC_REQUEST_SOURCES);
    }

    public static boolean isShortenModuleLibraryNamesEnabled() {
        return isEnabled(SHORTEN_MODULE_LIBRARY_NAMES);
    }

    public static boolean getExcludeCompiledSourceCodeInsideJars() {
        return isEnabled(EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS);
    }

    public static int getSymlinkScanMaxDepth() {
        return BazelRegistryKeys.intValue(SYMLINK_SCAN_MAX_DEPTH);
    }

    public static boolean getShutDownBeforeShardBuild() {
        return isEnabled(SHUTDOWN_BEFORE_SHARD_BUILD);
    }

    public static boolean getMergeSourceRoots() {
        return isEnabled(MERGE_SOURCE_ROOTS);
    }

    public static boolean getFastBuildEnabled() {
        return isEnabled(FAST_BUILD_ENABLED);
    }

    public static boolean getAutoOpenProjectIfPresent() {
        return isEnabled(AUTO_OPEN_PROJECT_IF_PRESENT);
    }

    public static boolean getRunConfigRunWithBazel() {
        return isEnabled(RUN_CONFIG_RUN_WITH_BAZEL);
    }

    public static boolean getSyntheticRunEnable() {
        return isEnabled(SYNTHETIC_RUN_ENABLE);
    }

    public static boolean getSyntheticRunDisableVisibilityCheck() {
        return isEnabled(SYNTHETIC_RUN_DISABLE_VISIBILITY_CHECK);
    }

    public static boolean getUsePty() {
        return isEnabled(USE_PTY);
    }

    public static boolean getShowBazelStdout() {
        return isEnabled(SHOW_BAZEL_STDOUT);
    }

    public static boolean getHardLinkOutputFiles() {
        return isEnabled(HARD_LINK_OUTPUT_FILES);
    }

    public static boolean getQueryBazelOnFileEvents() {
        return isEnabled(QUERY_ON_FILE_EVENTS);
    }

    public static boolean getHighlightUnsyncedSourceFiles() {
        return isEnabled(HIGHLIGHT_UNSYNCED_SOURCE_FILES);
    }

    public static boolean getHighlightIgnoredSourceFiles() {
        return isEnabled(HIGHLIGHT_IGNORED_SOURCE_FILES);
    }

    public static boolean getKillServerOnCancel() {
        return BazelRegistryKeys.is(KILL_SERVER_ON_CANCEL);
    }

    public static boolean getKillClientTreeOnCancel() {
        return BazelRegistryKeys.is(KILL_TREE_ON_CANCEL);
    }

    public static boolean isLogEnabled() {
        return BazelRegistryKeys.is(ENABLE_LOG);
    }

    public static boolean getMaskSecretsInLog() {
        return BazelRegistryKeys.is(MASK_SECRETS_IN_LOG);
    }

    private static boolean isEnabled(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            // consulo: Kotlin String.toBooleanStrict()
            if (value.equals("true")) {
                return true;
            }
            if (value.equals("false")) {
                return false;
            }
            throw new IllegalArgumentException("The string doesn't represent a boolean value: " + value);
        }
        return BazelRegistryKeys.is(key);
    }
}
