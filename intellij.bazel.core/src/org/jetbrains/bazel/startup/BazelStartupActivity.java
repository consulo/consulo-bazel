package org.jetbrains.bazel.startup;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.projectAware.BazelWorkspace;
import org.jetbrains.bazel.settings.bazel.BazelApplicationSettingsService;
import org.jetbrains.bazel.startup.utils.BazelProjectActivity;
import org.jetbrains.bazel.sync.ProjectDirtyStateService;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.target.ModuleTargetService;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.ui.widgets.fileTargets.BazelFileTargetsWidget;
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/**
 * Runs actions after the project has started up and the index is up to date.
 *
 * @see org.jetbrains.bazel.flow.open.BazelProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
@ExtensionImpl
public class BazelStartupActivity extends BazelProjectActivity {
    private static final Logger LOG = Logger.getInstance(BazelStartupActivity.class);
    private static final Key<Boolean> SKIP_STARTUP_SYNC = Key.create("bazel.skip.startup.sync.in.tests");

    /**
     * Controls automatic startup sync for one test project. Explicit sync is not affected.
     */
    @TestOnly
    public static void setBazelStartupSyncEnabledInTests(Project project, boolean enabled) {
        project.putUserData(SKIP_STARTUP_SYNC, enabled ? null : true);
    }

    @Override
    public CompletableFuture<?> executeForBazelProject(Project project) {
        LOG.info("Executing Bazel startup activity for project: " + project);
        BspConfigurationTrackerService trackerService = BspConfigurationTrackerService.getInstance(project);
        trackerService.setRunning(true);
        CompletableFuture<?> result;
        try {
            result = executeOnEveryProjectStartup(project)
                .thenCompose(unused -> resyncProjectIfNeeded(project));
        }
        catch (Throwable e) {
            result = CompletableFuture.failedFuture(e);
        }
        return result.whenComplete((value, error) -> trackerService.setRunning(false));
    }

    private static CompletableFuture<?> executeOnEveryProjectStartup(Project project) {
        LOG.debug("Executing Bazel startup activities for every opening");
        return BazelFileTargetsWidget.updateBazelFileTargetsWidget(project)
            .thenCompose(unused -> project.getInstance(BazelWorkspace.class).initialize())
            .thenApply(unused -> {
                project.getInstance(ProjectDirtyStateService.class); // must come after initialize(), because it reads the registered project id
                project.getInstance(ModuleTargetService.class); // warm up service
                project.getInstance(TargetStorage.class); // warm up service

                // store generated IML files outside the project directory
                // consulo: ExternalProjectsManagerImpl.setStoreExternally does not exist, Consulo keeps the module files in .consulo
                return null;
            });
    }

    private static CompletableFuture<?> resyncProjectIfNeeded(Project project) {
        if (Boolean.TRUE.equals(project.getUserData(SKIP_STARTUP_SYNC))) {
            return CompletableFuture.completedFuture(null);
        }

        return isProjectInIncompleteState(project).thenCompose(incomplete -> {
            if (!incomplete) {
                return CompletableFuture.completedFuture(null);
            }

            boolean phased = BazelApplicationSettingsService.getInstance().getSettings().getEnablePhasedSync();
            LOG.info(phased ? "Running Bazel phased sync task" : "Running Bazel sync task");
            return project.getInstance(ProjectSyncService.class).sync(
                new ProjectSyncScope.Full(
                    BazelFeatureFlags.isBuildProjectOnSyncEnabled(),
                    phased
                )
            );
        });
    }

    private static CompletableFuture<Boolean> isProjectInIncompleteState(Project project) {
        TargetStorage targetStorage = project.getInstance(TargetStorage.class);
        return targetStorage.awaitLoaded().thenApply(unused ->
            targetStorage.getTotalTargetCount() == 0 ||
                // consulo: upstream checks that the IntelliJ workspace model was loaded from its cache; the Bazel data of the
                // classic project model (BazelProjectModelService) is kept in memory, so it is missing after a restart
                BazelProjectModelService.getInstance(project).getProjectData(BazelProjectDirectoriesEntity.class) == null ||
                !bazelExecPathExists(project)
        );
    }

    private static boolean bazelExecPathExists(Project project) {
        WorkspaceSnapshot snapshot = WorkspaceHelpers.getWorkspaceSnapshotOrNull(project);
        return snapshot != null && Files.isDirectory(snapshot.bazelInfo().execRoot());
    }
}
