package org.jetbrains.bazel.action.registered;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jetbrains.bazel.project.DefaultProjectViewService;

import java.util.concurrent.CompletableFuture;

@ActionImpl(id = "Bazel.OpenProjectViewFile")
public class OpenProjectViewAction extends SuspendableAction implements DumbAware {
    public OpenProjectViewAction() {
        super(
            BazelPluginLocalize.widgetConfigFilePopupMessage(BazelPluginLocalize.toolWindowGenericConfigFile()),
            PlatformIconGroup.filetypesConfig()
        );
    }

    @Override
    protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
        return openProjectView(project);
    }

    public static CompletableFuture<?> openProjectView(Project project) {
        VirtualFile configFile = DefaultProjectViewService.getProjectViewFile(project);
        // consulo: upstream withContext(Dispatchers.EDT); ProjectViewUpdateCause is dropped
        return project.getUIAccess().giveAsync(() -> {
            ProjectView.getInstance(project).refresh();
            PsiFile psiFile = PsiManager.getInstance(project).findFile(configFile);
            if (psiFile != null) {
                psiFile.navigate(true);
            }
        });
    }
}
