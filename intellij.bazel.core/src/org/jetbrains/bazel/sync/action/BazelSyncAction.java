package org.jetbrains.bazel.sync.action;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import org.jetbrains.bazel.action.SaveAllFiles;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jspecify.annotations.Nullable;

public abstract class BazelSyncAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public BazelSyncAction() {
        super();
    }

    // consulo: upstream takes a `() -> String` text supplier, LocalizeValue is computed lazily too
    public BazelSyncAction(LocalizeValue text, @Nullable Image icon) {
        super(text, LocalizeValue.empty(), icon);
    }

    protected abstract @Nullable ProjectSyncScope computeScope(Project project, AnActionEvent e);

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        ProjectSyncScope scope = computeScope(project, e);
        if (scope == null) {
            return;
        }
        BazelCoroutineService.getInstance(project).start(() ->
            SaveAllFiles.saveAllFiles().thenCompose(ignored -> project.getInstance(ProjectSyncService.class).sync(scope))
        );
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null || !org.jetbrains.bazel.config.project.isBazelProject(project)) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        boolean visible = shouldBeVisible(project, e);
        e.getPresentation().setVisible(visible);
        // consulo: Consulo has no trusted projects (upstream TrustedProjects.isProjectTrusted(project))
        e.getPresentation().setEnabled(visible &&
            !SyncStatusService.isSyncInProgress(project) &&
            !org.jetbrains.bazel.ui.console.TaskConsole.isBuildInProgress(project));
        if (visible) {
            updatePresentation(project, e);
        }
    }

    protected boolean shouldBeVisible(Project project, AnActionEvent e) {
        return true;
    }

    protected void updatePresentation(Project project, AnActionEvent e) {
    }

    // consulo: AnActionWithSyncUpdate always updates in background (upstream ActionUpdateThread.BGT)
}
