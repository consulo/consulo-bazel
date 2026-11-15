package org.jetbrains.bazel.action.registered;

import consulo.annotation.component.ActionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.sync.status.SyncStatusService;

import java.util.concurrent.CompletableFuture;

abstract class BazelSyncActionBase extends SuspendableAction {
    // consulo: the icon comes from the upstream <action icon="..."> registration
    BazelSyncActionBase(LocalizeValue text, Image icon) {
        super(text, icon);
    }

    protected abstract ProjectSyncScope getScope();

    @Override
    protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
        return project.getInstance(ProjectSyncService.class).sync(getScope());
    }

    @Override
    protected void update(Project project, AnActionEvent e) {
        e.getPresentation().setEnabled(
            !SyncStatusService.isSyncInProgress(project) && !org.jetbrains.bazel.ui.console.TaskConsole.isBuildInProgress(project)
        );
    }
}

@ActionImpl(id = "Bazel.BuildAndResyncAction")
class BuildAndResyncAction extends BazelSyncActionBase {
    private final ProjectSyncScope myScope = new ProjectSyncScope.Full(true, false);

    public BuildAndResyncAction() {
        super(BazelPluginLocalize.buildAndResyncActionText(), BazelPluginIcons.buildAndResync());
    }

    @Override
    protected ProjectSyncScope getScope() {
        return myScope;
    }
}

@ActionImpl(id = "Bazel.ResyncAction")
class ResyncAction extends BazelSyncActionBase {
    private final ProjectSyncScope myScope = new ProjectSyncScope.Full(false, false);

    public ResyncAction() {
        super(BazelPluginLocalize.resyncActionText(), BazelPluginIcons.resync());
    }

    @Override
    protected ProjectSyncScope getScope() {
        return myScope;
    }
}
