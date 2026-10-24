package org.jetbrains.bazel.languages.projectview.annotation.quickfix;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// consulo: upstream internal top-level extension functions of Utils.kt
final class Utils {
    private Utils() {
    }

    static List<PsiElement> contentLines(ProjectViewPsiSection receiver) {
        List<PsiElement> lines = new ArrayList<>();
        for (PsiElement it = receiver.getFirstChild(); it != null; it = it.getNextSibling()) {
            if ((it instanceof ProjectViewPsiSectionItem && !it.getText().isBlank())
                || PsiUtilCore.getElementType(it) == ProjectViewTokenType.COMMENT) {
                lines.add(it);
            }
        }
        int lastIndex = -1;
        for (int index = lines.size() - 1; index >= 0; index--) {
            PsiElement it = lines.get(index);
            if (it instanceof ProjectViewPsiSectionItem
                || PsiUtilCore.getElementType(it.getPrevSibling()) == ProjectViewTokenType.WHITESPACE) {
                lastIndex = index;
                break;
            }
        }
        return lines.subList(0, lastIndex + 1);
    }

    // consulo: Kotlin default argument content = contentLines()
    static void deleteContent(ProjectViewPsiSection receiver) {
        deleteContent(receiver, contentLines(receiver));
    }

    static void deleteContent(ProjectViewPsiSection receiver, List<PsiElement> content) {
        PsiElement colon = receiver.getColon();
        PsiElement last = !content.isEmpty() ? content.getLast() : colon != null ? colon : receiver.getKeyword();
        PsiElement next = last.getNextSibling();
        PsiElement end = next != null && PsiUtilCore.getElementType(next) == ProjectViewTokenType.NEWLINE ? next : last;
        if (end.getNextSibling() == null) {
            receiver.delete();
        }
        else {
            receiver.deleteChildRange(Objects.requireNonNull(receiver.getFirstChild()), end);
        }
    }
}
