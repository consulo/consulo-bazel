package org.jetbrains.bazel.languages.projectview;

import org.jetbrains.bazel.commons.ShardingApproach;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

// Extension properties to provide convenient access to ProjectView sections

// consulo: upstream Kotlin top-level declarations; extension properties ProjectView.x become static getX(ProjectView)
public final class ProjectViewExtensions {
    private ProjectViewExtensions() {
    }

    public static final ProjectViewSectionKey<List<String>> BUILD_FLAGS_KEY = new ProjectViewSectionKey<>("build_flags", List.of());

    public static List<String> getBuildFlags(ProjectView receiver) {
        return receiver.getSection(BUILD_FLAGS_KEY);
    }

    public static final ProjectViewSectionKey<List<String>> SYNC_FLAGS_KEY = new ProjectViewSectionKey<>("sync_flags", List.of());

    public static List<String> getSyncFlags(ProjectView receiver) {
        return receiver.getSection(SYNC_FLAGS_KEY);
    }

    public static final ProjectViewSectionKey<List<String>> DEBUG_FLAGS_KEY = new ProjectViewSectionKey<>("debug_flags", List.of());

    public static List<String> getDebugFlags(ProjectView receiver) {
        return receiver.getSection(DEBUG_FLAGS_KEY);
    }

    public static final ProjectViewSectionKey<@Nullable Path> BAZEL_BINARY_KEY = new ProjectViewSectionKey<>("bazel_binary", null);

    public static @Nullable Path getBazelBinary(ProjectView receiver) {
        return receiver.getSection(BAZEL_BINARY_KEY);
    }

    public static final ProjectViewSectionKey<Boolean> ALLOW_MANUAL_TARGETS_SYNC_KEY =
        new ProjectViewSectionKey<>("allow_manual_targets_sync", false);

    public static boolean getAllowManualTargetsSync(ProjectView receiver) {
        return receiver.getSection(ALLOW_MANUAL_TARGETS_SYNC_KEY);
    }

    public static final ProjectViewSectionKey<@Nullable List<String>> ASPPECTS_ATTRS_KEY = new ProjectViewSectionKey<>("aspects_attrs", null);

    public static @Nullable List<String> getAspectsAttrs(ProjectView receiver) {
        return receiver.getSection(ASPPECTS_ATTRS_KEY);
    }

    public static final ProjectViewSectionKey<Boolean> DROP_NO_IDE_KEY = new ProjectViewSectionKey<>("drop_no_ide", false);

    public static boolean getDropNoIde(ProjectView receiver) {
        return receiver.getSection(DROP_NO_IDE_KEY);
    }

    public static final ProjectViewSectionKey<Boolean> DERIVE_TARGETS_FROM_DIRECTORIES_KEY =
        new ProjectViewSectionKey<>("derive_targets_from_directories", false);

    public static boolean getDeriveTargetsFromDirectories(ProjectView receiver) {
        return receiver.getSection(DERIVE_TARGETS_FROM_DIRECTORIES_KEY);
    }

    public static final ProjectViewSectionKey<Integer> IMPORT_DEPTH_KEY = new ProjectViewSectionKey<>("import_depth", -1);

    public static int getImportDepth(ProjectView receiver) {
        return receiver.getSection(IMPORT_DEPTH_KEY);
    }

    public static final ProjectViewSectionKey<List<String>> ENABLED_RULES_KEY = new ProjectViewSectionKey<>("enabled_rules", List.of());

    public static List<String> getEnabledRules(ProjectView receiver) {
        return receiver.getSection(ENABLED_RULES_KEY);
    }

    public static final ProjectViewSectionKey<Boolean> SHARD_SYNC_KEY = new ProjectViewSectionKey<>("shard_sync", false);

    public static boolean getShardSync(ProjectView receiver) {
        return receiver.getSection(SHARD_SYNC_KEY);
    }

    public static final ProjectViewSectionKey<Integer> TARGET_SHARD_SIZE_KEY = new ProjectViewSectionKey<>("target_shard_size", 1000);

    public static int getTargetShardSize(ProjectView receiver) {
        return receiver.getSection(TARGET_SHARD_SIZE_KEY);
    }

    public static final ProjectViewSectionKey<@Nullable ShardingApproach> SHARDING_APPROACH_KEY =
        new ProjectViewSectionKey<>("sharding_approach", null);

    public static @Nullable String getShardingApproach(ProjectView receiver) {
        ShardingApproach shardingApproach = receiver.getSection(SHARDING_APPROACH_KEY);
        return shardingApproach != null ? shardingApproach.name().toLowerCase(Locale.ROOT) : null;
    }

    public static final ProjectViewSectionKey<List<Path>> IMPORT_RUN_CONFIGURATIONS_KEY =
        new ProjectViewSectionKey<>("import_run_configurations", List.of());

    public static List<Path> getImportRunConfigurations(ProjectView receiver) {
        return receiver.getSection(IMPORT_RUN_CONFIGURATIONS_KEY);
    }

    public static final ProjectViewSectionKey<@Nullable Label> GAZELLE_TARGET_KEY = new ProjectViewSectionKey<>("gazelle_target", null);

    public static @Nullable Label getGazelleTarget(ProjectView receiver) {
        return receiver.getSection(GAZELLE_TARGET_KEY);
    }

    public static final ProjectViewSectionKey<List<String>> PYTHON_DEBUG_FLAGS_KEY =
        new ProjectViewSectionKey<>("python_debug_flags", List.of());

    public static List<String> getPythonDebugFlags(ProjectView receiver) {
        return receiver.getSection(PYTHON_DEBUG_FLAGS_KEY);
    }

    public static final ProjectViewSectionKey<Boolean> RUN_CONFIG_RUN_WITH_BAZEL_KEY =
        new ProjectViewSectionKey<>("run_config_run_with_bazel", BazelFeatureFlags.getRunConfigRunWithBazel());

    public static boolean getRunConfigRunWithBazel(ProjectView receiver) {
        return receiver.getSection(RUN_CONFIG_RUN_WITH_BAZEL_KEY);
    }

    public static final ProjectViewSectionKey<List<String>> TEST_SOURCES_KEY = new ProjectViewSectionKey<>("test_sources", List.of());

    public static List<String> getTestSources(ProjectView receiver) {
        return receiver.getSection(TEST_SOURCES_KEY);
    }

    public static final ProjectViewSectionKey<@Nullable Path> DOT_IDEA_DIRECTORY_LOCATION_KEY =
        new ProjectViewSectionKey<>("dot_idea_directory_location", null);

    public static @Nullable Path getDotIdeaDirectoryLocation(ProjectView receiver) {
        return receiver.getSection(DOT_IDEA_DIRECTORY_LOCATION_KEY);
    }

    public static final ProjectViewSectionKey<List<String>> INDEX_KEY = new ProjectViewSectionKey<>("index", List.of());

    public static List<String> getIndex(ProjectView receiver) {
        List<String> index = receiver.getSection(INDEX_KEY);
        if (!index.isEmpty()) {
            return index;
        }
        return receiver.getSection(INDEX_ALL_FILES_IN_DIRECTORIES_KEY)
            ? List.of("*")
            : receiver.getSection(INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY);
    }

    public static final ProjectViewSectionKey<Boolean> INDEX_ALL_FILES_IN_DIRECTORIES_KEY =
        new ProjectViewSectionKey<>("index_all_files_in_directories", false);
    public static final ProjectViewSectionKey<List<String>> INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY =
        new ProjectViewSectionKey<>("index_additional_files_in_directories", List.of());
}
