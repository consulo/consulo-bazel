package org.jetbrains.bazel.action.registered;

import consulo.annotation.component.ActionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.platform.Platform;
import consulo.platform.PlatformFeature;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jetbrains.bazel.bazelrunner.BazelLog;

import java.nio.file.Files;
import java.nio.file.Path;

@ActionImpl(id = "Bazel.ShowBazelLogAction")
public class ShowBazelLogAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public ShowBazelLogAction() {
        super(BazelPluginLocalize.actionBazelShowbazellogactionText(Platform.current().fileManagerName()));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Path logFile = BazelLog.getLogPath();
        if (Files.exists(logFile)) {
            // consulo: upstream RevealFileAction.openFile(logFile)
            Platform.current().openFileInFileManager(logFile, UIAccess.current());
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        boolean isBazel = project != null && org.jetbrains.bazel.config.project.isBazelProject(project);
        boolean logExists = Files.exists(BazelLog.getLogPath());
        // consulo: upstream RevealFileAction.isSupported()
        e.getPresentation().setEnabledAndVisible(
            isBazel && logExists && Platform.current().supportsFeature(PlatformFeature.OPEN_FILE_IN_FILE_MANAGER)
        );
        if (e.getPresentation().isEnabledAndVisible()) {
            e.getPresentation().setText(BazelPluginLocalize.actionBazelShowbazellogactionText(
                // consulo: upstream RevealFileAction.getFileManagerName()
                Platform.current().fileManagerName()
            ));
        }
    }

    // consulo: AnActionWithSyncUpdate always updates in background (upstream ActionUpdateThread.BGT)
}
