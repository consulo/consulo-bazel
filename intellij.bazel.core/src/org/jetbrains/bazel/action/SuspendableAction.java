package org.jetbrains.bazel.action;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.image.Image;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class SuspendableAction extends AnAction implements DumbAware, AnActionWithSyncUpdate {
    private static final Logger LOG = Logger.getInstance(SuspendableAction.class);

    // consulo: upstream takes a `() -> String` text supplier, LocalizeValue is computed lazily too
    public SuspendableAction(LocalizeValue text, @Nullable Image icon) {
        super(text, LocalizeValue.empty(), icon);
    }

    public SuspendableAction(LocalizeValue text) {
        this(text, null);
    }

    public SuspendableAction(String text, @Nullable Image icon) {
        this(LocalizeValue.of(text), icon);
    }

    public SuspendableAction(String text) {
        this(text, null);
    }

    @RequiredUIAccess
    @Override
    public final void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        if (project != null) {
            BazelCoroutineService.getInstance(project).start(() ->
                SaveAllFiles.saveAllFiles().thenCompose(ignored -> actionPerformed(project, e))
            );
        }
        else {
            LOG.warn("`actionPerformed` for action '" + e.getPresentation().getText() + "' cannot be performed. Project is missing.");
        }
    }

    protected abstract CompletableFuture<?> actionPerformed(Project project, AnActionEvent e);

    @Override
    public final void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        if (project != null) {
            doUpdate(project, e);
        }
        else {
            LOG.warn("`update` for action '" + e.getPresentation().getText() + "' cannot be performed. Project is missing.");
        }
    }

    private void doUpdate(Project project, AnActionEvent e) {
        // consulo: Consulo has no trusted projects, the trusted branch is always taken
        boolean enabled = org.jetbrains.bazel.config.project.isBazelProject(project);
        e.getPresentation().setVisible(enabled);
        if (enabled) {
            update(project, e);
        }
    }

    protected void update(Project project, AnActionEvent e) {
        // do nothing by default
    }

    // consulo: AnActionWithSyncUpdate always updates in background (upstream ActionUpdateThread.BGT)

    public static @Nullable PsiFile getPsiFile(AnActionEvent receiver) {
        return receiver.getData(PsiFile.KEY);
    }

    public static @Nullable Editor getEditor(AnActionEvent receiver) {
        return receiver.getData(Editor.KEY);
    }
}
