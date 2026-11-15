package org.jetbrains.bazel.settings;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService;
import org.jetbrains.bazel.flow.open.utils;
import org.jetbrains.bazel.project.DefaultProjectViewService;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@ActionImpl(
    id = "LoadProjectViewFileAction",
    parents = {
        @ActionParentRef(value = @ActionRef(id = "EditorPopupMenu"), anchor = ActionRefAnchor.LAST),
        @ActionParentRef(value = @ActionRef(id = "ProjectViewPopupMenu"), anchor = ActionRefAnchor.LAST)
    }
)
public class LoadProjectViewFileAction extends SuspendableAction implements DumbAware {
    public LoadProjectViewFileAction() {
        // consulo: the icon comes from the upstream <action icon="/icons/bazel.svg"> registration
        super(
            BazelPluginLocalize.actionLoadProjectViewFile(),
            BazelPluginIcons.bazel()
        );
    }

    @Override
    protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
        Path projectViewFile = Objects.requireNonNull(e.getData(VirtualFile.KEY)).toNioPath();
        BazelApplicationCoroutineScopeService.getInstance().launch(() ->
            utils.closeAndReopenAsBazelProject(project, projectViewFile)
        );
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void update(Project project, AnActionEvent e) {
        VirtualFile virtualFile = e.getData(VirtualFile.KEY);

        e.getPresentation().setEnabledAndVisible(
            virtualFile != null &&
                Constants.PROJECT_VIEW_FILE_EXTENSION.equals(virtualFile.getExtension()) &&
                !virtualFile.equals(DefaultProjectViewService.getProjectViewFile(project))
        );
    }
}
