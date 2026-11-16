package org.jetbrains.bazel.flow.open;

import consulo.annotation.component.ExtensionImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService;
import org.jetbrains.bazel.flow.vcs.BazelVcsRootErrorFilter;
import org.jetbrains.bazel.project.DefaultProjectViewService;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

// consulo: upstream InitProjectActivity (projectPreInit) -> a post startup activity (plugins cannot order it first)
@ExtensionImpl
public class OpenBazelProjectAndSyncStartupActivity implements PostStartupActivity {
    private static final Logger LOG = Logger.getInstance(OpenBazelProjectAndSyncStartupActivity.class);

    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        // If flag is set and project is not yet linked as Bazel, link it first
        // This handles headless mode with -Dbazel.project.auto.open.if.present=true
        if (shouldLinkAsBazelProject(project)) {
            LOG.trace("Auto-linking Bazel project: " + project.getName());
            // consulo: upstream project.guessProjectDir()
            VirtualFile projectFolder = utils.findProjectFolderFromVFile(project.getBaseDir());
            if (projectFolder == null) {
                return;
            }
            Path projectFolderPath;
            try {
                projectFolderPath = projectFolder.toNioPath();
            }
            catch (UnsupportedOperationException e) {
                return;
            }
            Path workspaceFile = utils.getWorkspaceFile(projectFolderPath);
            if (workspaceFile == null) {
                return;
            }
            BazelApplicationCoroutineScopeService.getInstance().launch(() ->
                utils.closeAndReopenAsBazelProject(project, workspaceFile)
            );
            return;
        }

        if (org.jetbrains.bazel.config.project.isBazelProject(project)) {
            // consulo: upstream suspends the project initialization until this completes; Consulo runs post startup
            // activities after the project is opened, so the initialization is started without blocking
            doBazelProjectInitialization(project);
        }
    }

    private boolean shouldLinkAsBazelProject(Project project) {
        // Already linked - nothing to do
        if (org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return false;
        }
        PluginDescriptor googleBazelPlugin = PluginManager.findPlugin(PluginId.getId("com.google.idea.bazel.ijwb"));
        if (googleBazelPlugin != null && googleBazelPlugin.isEnabled()) {
            return false;
        }
        // Link if flag is set
        return BazelFeatureFlags.getAutoOpenProjectIfPresent();
    }

    private CompletableFuture<?> doBazelProjectInitialization(Project project) {
        // After project opening refactor Bazel plugin assumes that project view is available
        // immediately after project opening, because now the plugin literally "opens a project view".
        // Make sure that the file is loaded and available before project opens.
        DefaultProjectViewService.getInstance(project).ensureProjectViewInitialized();
        // Make sure that Bazel project has at least one root registered at startup, so
        // user can start working even when project isn't synced yet.
        // TODO consulo: registerProjectRoot (IntelliJ workspace model project root without a module) is missing

        // An earlier session may have persisted a Git mapping in a Bazel output directory in vcs.xml (BAZEL-948), e.g.,
        // the execution root or the bazel-<workspace> symlink. Remove it here, before the VCS initialization activates Git for it,
        // as Git over the whole Bazel output tree can freeze the IDE. This is the only cleanup: BazelVcsRootErrorFilter keeps
        // new such mappings from being auto-registered. Mappings outside the Bazel output directories stay untouched.
        // It has to be this activity, since projectPreInit accepts only approved classes (see ProjectManagerImpl.runApprovedExtensions).
        return BazelVcsRootErrorFilter.removeBazelOutputMappings(project);
    }
}
