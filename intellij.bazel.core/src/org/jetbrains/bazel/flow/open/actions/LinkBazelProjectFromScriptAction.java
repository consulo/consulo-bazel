package org.jetbrains.bazel.flow.open.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService;
import org.jetbrains.bazel.flow.open.utils;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@ActionImpl(
    id = "Bazel.ImportExternalProject",
    parents = {
        // consulo: upstream group ProjectViewPopupMenuSettingsGroup
        @ActionParentRef(value = @ActionRef(id = "ProjectViewPopupMenu"), anchor = ActionRefAnchor.LAST),
        @ActionParentRef(value = @ActionRef(id = "EditorPopupMenu"), anchor = ActionRefAnchor.LAST)
    }
)
public class LinkBazelProjectFromScriptAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public LinkBazelProjectFromScriptAction() {
        super(BazelPluginLocalize.actionLinkBazelProjectFromLinkedProject(), LocalizeValue.empty(), BazelPluginIcons.bazel());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        VirtualFile virtualFile = e.getData(VirtualFile.KEY);
        if (virtualFile == null) {
            return;
        }
        Path filePath = virtualFile.toNioPath();

        BazelApplicationCoroutineScopeService.getInstance().launch(() ->
            CompletableFuture.supplyAsync(() -> utils.findProjectFolderFromVFile(virtualFile))
                .thenCompose(projectFolder -> {
                    if (projectFolder == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return utils.closeAndReopenAsBazelProject(project, filePath);
                })
        );
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        VirtualFile file = e.getData(VirtualFile.KEY);
        e.getPresentation().setEnabledAndVisible(
            (project == null || !org.jetbrains.bazel.config.project.isBazelProject(project)) &&
                file != null && utils.isBazelWorkspaceFile(file)
        );
    }
}
