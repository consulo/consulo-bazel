package org.jetbrains.bazel.languages.starlark.formatting;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.settings.bazel.BazelProjectSettings;

// consulo: IntelliJ ActionsOnSaveFileDocumentManagerListener.DocumentUpdatingActionOnSave does not exist; the action is a
// FileDocumentManagerListener that formats the document right before it is written (like Consulo's TrailingSpacesStripper).
// TODO consulo: there is no "Actions on Save" settings page (presentableName is unused) and the listener runs for every
//  document save, not only for the explicit save actions handled by IntelliJ's ActionsOnSaveFileDocumentManagerListener
@TopicImpl(ComponentScope.APPLICATION)
public class StarlarkFormattingActionOnSave implements FileDocumentManagerListener {
    public String getPresentableName() {
        return "buildifier";
    }

    public boolean isEnabledForProject(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project)
            && BazelProjectSettings.getBazelProjectSettings(project).runBuildifierOnSave();
    }

    @RequiredWriteAction
    public void updateDocument(Project project, Document document) {
        // consulo: upstream readAction { }; beforeDocumentSaving() already runs in a write action
        if (!(PsiDocumentManager.getInstance(project).getPsiFile(document) instanceof StarlarkFile psiFile)) {
            return;
        }
        StarlarkFormattingService.formatBuildFile(psiFile);
    }

    @Override
    @RequiredWriteAction
    public void beforeDocumentSaving(Document document) {
        // consulo: upstream ActionsOnSaveFileDocumentManagerListener calls isEnabledForProject()/updateDocument() for the project
        // of the saved document
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }
        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        if (project == null || project.isDisposed()) {
            return;
        }
        if (!isEnabledForProject(project)) {
            return;
        }
        updateDocument(project, document);
    }
}
