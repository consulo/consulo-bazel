package org.jetbrains.bazel.flow.open;

import consulo.annotation.component.ExtensionImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService;

import java.nio.file.Path;
import java.util.Objects;

@ExtensionImpl
public class LegacyIjwbMigrationActivity implements PostStartupActivity {
    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        if (!isLegacyIjwbProjectWithoutLegacyPlugin(project)) {
            return;
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        Path ijwbPath = Path.of(basePath);
        Path workspaceRoot = utils.findProjectFolderFromFile(ijwbPath);
        if (workspaceRoot == null) {
            return;
        }
        Path workspaceFile = utils.getWorkspaceFile(workspaceRoot);
        if (workspaceFile == null) {
            return;
        }
        BazelApplicationCoroutineScopeService.getInstance().launch(() ->
            utils.closeAndReopenAsBazelProject(project, workspaceFile)
        );
    }


    private boolean isLegacyIjwbProjectWithoutLegacyPlugin(Project project) {
        String externalProjectPath = Objects.requireNonNullElse(project.getBasePath(), "");
        if (!externalProjectPath.endsWith(".ijwb")) {
            return false;
        }
        if (org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return false;
        }
        PluginDescriptor googleBazelPlugin = PluginManager.findPlugin(PluginId.getId("com.google.idea.bazel.ijwb"));
        boolean isGoogleBazelPluginEnabled = googleBazelPlugin != null && googleBazelPlugin.isEnabled();
        return !isGoogleBazelPluginEnabled;
    }

}
