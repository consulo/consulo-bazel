package org.jetbrains.bazel.languages.projectview.annotation.suppress;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewSuppression;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// consulo: upstream internal top-level function of ProjectViewSuppressionCommand.kt
final class ProjectViewSuppressionCommand {
    private ProjectViewSuppressionCommand() {
    }

    /**
     * The edit that adds {@code id} to the suppression comment of {@code container}, or a new comment above {@code container} when
     * it has none.
     */
    // consulo: the ModCommand API (ModUpdateFileText) does not exist in Consulo; the command is a Runnable that changes the document
    // of the file, the caller runs it in a write action. null stands for ModCommand.nop()
    static @Nullable Runnable addProjectViewSuppressionCommand(
        PsiElement container,
        ProjectViewProblem.Id id
    ) {
        PsiFile file = container.getContainingFile();
        if (file == null) {
            return null;
        }
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        String text = file.getText();
        TextRange range;
        String replacement;
        ProjectViewSuppression existing = ProjectViewSuppression.from(container).findFirst().orElse(null);
        if (existing == null) {
            int containerStartOffset = container.getTextRange().getStartOffset();
            int lineStart = text.lastIndexOf('\n', containerStartOffset - 1) + 1;
            range = TextRange.from(lineStart, 0);
            replacement = text.substring(lineStart, containerStartOffset) + ProjectViewSuppression.commentText(List.of(id)) + "\n";
        }
        else {
            List<ProjectViewProblem.Id> ids = new ArrayList<>(existing.ids());
            ids.add(id);
            range = existing.idsRange();
            replacement = ProjectViewSuppression.commentIdsText(ids);
        }
        return () -> {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
            Document document = documentManager.getDocument(file);
            if (document == null) {
                return;
            }
            document.replaceString(range.getStartOffset(), range.getEndOffset(), replacement);
            documentManager.commitDocument(document);
        };
    }
}
