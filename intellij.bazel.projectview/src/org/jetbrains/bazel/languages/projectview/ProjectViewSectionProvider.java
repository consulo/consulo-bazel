package org.jetbrains.bazel.languages.projectview;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.component.extension.ExtensionPoint;
import org.jetbrains.bazel.commons.ExcludableValue;
import org.jetbrains.bazel.commons.ShardingApproach;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.completion.PathCompletionProviders.DirectoriesCompletionProvider;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.ALLOW_MANUAL_TARGETS_SYNC_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.BAZEL_BINARY_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.BUILD_FLAGS_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.DEBUG_FLAGS_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.DERIVE_TARGETS_FROM_DIRECTORIES_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.DOT_IDEA_DIRECTORY_LOCATION_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.ENABLED_RULES_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.GAZELLE_TARGET_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.IMPORT_DEPTH_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.IMPORT_RUN_CONFIGURATIONS_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.INDEX_ALL_FILES_IN_DIRECTORIES_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.INDEX_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.PYTHON_DEBUG_FLAGS_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.RUN_CONFIG_RUN_WITH_BAZEL_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.SHARDING_APPROACH_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.SHARD_SYNC_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.SYNC_FLAGS_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.TARGET_SHARD_SIZE_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.TEST_SOURCES_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewTargets.DIRECTORIES_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewTargets.TARGETS_KEY;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProjectViewSectionProvider {
    List<ProjectViewSection<?>> getSections();

    // consulo: upstream EP_NAME, served by the Consulo extension point
    static ExtensionPoint<ProjectViewSectionProvider> getEpName() {
        return Application.get().getExtensionPoint(ProjectViewSectionProvider.class);
    }
}

@ExtensionImpl
class DefaultProjectViewSectionProvider implements ProjectViewSectionProvider {
    private final List<ProjectViewSection<?>> mySections =
        List.of(
            ALLOW_MANUAL_TARGETS_SYNC_SECTION,
            BAZEL_BINARY_SECTION,
            BUILD_FLAGS_SECTION,
            DEBUG_FLAGS_SECTION,
            DERIVE_TARGETS_FROM_DIRECTORIES_SECTION,
            DIRECTORIES_SECTION,
            DOT_IDEA_DIRECTORY_LOCATION_SECTION,
            ENABLED_RULES_SECTION,
            GAZELLE_TARGET_SECTION,
            IMPORT_DEPTH_SECTION,
            IMPORT_RUN_CONFIGURATIONS_SECTION,
            INDEX_SECTION,
            RUN_CONFIG_RUN_WITH_BAZEL_SECTION,
            SHARD_SYNC_SECTION,
            SHARDING_APPROACH_SECTION,
            SYNC_FLAGS_SECTION,
            TARGET_SHARD_SIZE_SECTION,
            TARGETS_SECTION,
            // deprecated sections
            INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_SECTION,
            INDEX_ALL_FILES_IN_DIRECTORIES_SECTION,
            PYTHON_DEBUG_FLAGS_SECTION,
            TEST_SOURCES_SECTION
        );

    // consulo: extensions need a public constructor
    public DefaultProjectViewSectionProvider() {
    }

    @Override
    public List<ProjectViewSection<?>> getSections() {
        return mySections;
    }

    // consulo: upstream private top-level declarations of this file

    private static final ProjectViewSection<Boolean> ALLOW_MANUAL_TARGETS_SYNC_SECTION = new ProjectViewSection<>(
        ALLOW_MANUAL_TARGETS_SYNC_KEY,
        ProjectViewSectionType.BOOLEAN,
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsAllow_manual_targets_sync()
    );

    private static final ProjectViewSection<@Nullable Path> BAZEL_BINARY_SECTION = new ProjectViewSection<>(
        BAZEL_BINARY_KEY,
        ProjectViewSectionType.path(true),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsBazel_binary()
    );

    private static final ProjectViewSection<List<String>> BUILD_FLAGS_SECTION = new ProjectViewSection<>(
        BUILD_FLAGS_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.flag("build")),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsBuild_flags()
    );

    private static final ProjectViewSection<List<String>> DEBUG_FLAGS_SECTION = new ProjectViewSection<>(
        DEBUG_FLAGS_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.flag("run", "test")),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsDebug_flags()
    );

    private static final ProjectViewSection<Boolean> DERIVE_TARGETS_FROM_DIRECTORIES_SECTION = new ProjectViewSection<>(
        DERIVE_TARGETS_FROM_DIRECTORIES_KEY,
        ProjectViewSectionType.BOOLEAN,
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsDerive_targets_from_directories()
    );

    private static final ProjectViewSection<List<ExcludableValue<Path>>> DIRECTORIES_SECTION = new ProjectViewSection<>(
        DIRECTORIES_KEY,
        ProjectViewSectionType.list(
            ProjectViewSectionType.excludable(ProjectViewSectionType.path(false, new DirectoriesCompletionProvider()))
        ),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsDirectories()
    );

    private static final ProjectViewSection<@Nullable Path> DOT_IDEA_DIRECTORY_LOCATION_SECTION = new ProjectViewSection<>(
        DOT_IDEA_DIRECTORY_LOCATION_KEY,
        ProjectViewSectionType.path(),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsDot_idea_directory_location()
    );

    private static final ProjectViewSection<List<String>> ENABLED_RULES_SECTION = new ProjectViewSection<>(
        ENABLED_RULES_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.string()),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsEnabled_rules()
    );

    private static final ProjectViewSection<@Nullable Label> GAZELLE_TARGET_SECTION = new ProjectViewSection<>(
        GAZELLE_TARGET_KEY,
        ProjectViewSectionType.LABEL,
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsGazelle_target()
    );

    private static final ProjectViewSection<Integer> IMPORT_DEPTH_SECTION = new ProjectViewSection<>(
        IMPORT_DEPTH_KEY,
        ProjectViewSectionType.INT,
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsImport_depth()
    );

    private static final ProjectViewSection<List<Path>> IMPORT_RUN_CONFIGURATIONS_SECTION = new ProjectViewSection<>(
        IMPORT_RUN_CONFIGURATIONS_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.file(".xml")),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsImport_run_configurations()
    );

    private static final ProjectViewSection<List<String>> INDEX_SECTION = new ProjectViewSection<>(
        INDEX_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.string()),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsIndex()
    );

    private static final ProjectViewSection<Boolean> RUN_CONFIG_RUN_WITH_BAZEL_SECTION = new ProjectViewSection<>(
        RUN_CONFIG_RUN_WITH_BAZEL_KEY,
        ProjectViewSectionType.BOOLEAN,
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsRun_config_run_with_bazel()
    );

    private static final ProjectViewSection<Boolean> SHARD_SYNC_SECTION = new ProjectViewSection<>(
        SHARD_SYNC_KEY,
        ProjectViewSectionType.BOOLEAN,
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsShard_sync()
    );

    private static final ProjectViewSection<@Nullable ShardingApproach> SHARDING_APPROACH_SECTION = new ProjectViewSection<>(
        SHARDING_APPROACH_KEY,
        ProjectViewSectionType.enumType(ShardingApproach.class),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsSharding_approach()
    );

    private static final ProjectViewSection<List<String>> SYNC_FLAGS_SECTION = new ProjectViewSection<>(
        SYNC_FLAGS_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.flag("build")),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsSync_flags()
    );

    private static final ProjectViewSection<Integer> TARGET_SHARD_SIZE_SECTION = new ProjectViewSection<>(
        TARGET_SHARD_SIZE_KEY,
        ProjectViewSectionType.INT,
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsTarget_shard_size()
    );

    private static final ProjectViewSection<List<ExcludableValue<Label>>> TARGETS_SECTION = new ProjectViewSection<>(
        TARGETS_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.excludable(ProjectViewSectionType.LABEL)),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsTargets()
    );

    // deprecated sections

    private static final ProjectViewSection.Deprecation INDEXING_DEPRECATION = new ProjectViewSection.Deprecation(
        BazelProjectViewLocalize.annotatorDeprecatedSectionWithReplacementWarning(INDEX_KEY.getName()),
        ProjectViewSection.Deprecation.Level.Warning,
        List.of(ProjectViewProblem.QuickFix.ReplaceWithIndex.INSTANCE)
    );

    private static final ProjectViewSection<List<String>> INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_SECTION = new ProjectViewSection<>(
        INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.string()),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsIndex_additional_files_in_directories(),
        INDEXING_DEPRECATION
    );

    private static final ProjectViewSection<Boolean> INDEX_ALL_FILES_IN_DIRECTORIES_SECTION = new ProjectViewSection<>(
        INDEX_ALL_FILES_IN_DIRECTORIES_KEY,
        ProjectViewSectionType.BOOLEAN,
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsIndex_all_files_in_directories(),
        INDEXING_DEPRECATION
    );

    private static final ProjectViewSection<List<String>> PYTHON_DEBUG_FLAGS_SECTION = new ProjectViewSection<>(
        PYTHON_DEBUG_FLAGS_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.flag("run", "test")),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsPython_debug_flags(),
        ProjectViewSection.Deprecation.withMergeQuickFix(DEBUG_FLAGS_KEY)
    );

    private static final ProjectViewSection<List<String>> TEST_SOURCES_SECTION = new ProjectViewSection<>(
        TEST_SOURCES_KEY,
        ProjectViewSectionType.list(ProjectViewSectionType.string()),
        BazelProjectViewLocalize.bazelLanguageProjectviewDocsTest_sources(),
        new ProjectViewSection.Deprecation(BazelProjectViewLocalize.annotatorDeprecatedSectionTest_sources())
    );
}
