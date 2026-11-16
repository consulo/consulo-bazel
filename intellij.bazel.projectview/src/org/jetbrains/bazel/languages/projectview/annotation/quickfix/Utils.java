package org.jetbrains.bazel.languages.projectview.annotation.quickfix;

import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// consulo: upstream internal top-level extension functions of Utils.kt
public final class Utils {
    private Utils() {
    }

    // consulo: ModCommandAction -> classic IntentionAction
    public static List<IntentionAction> quickFixActions(ProjectViewProblem receiver) {
        PsiElement element = receiver.element();
        if (element == null) {
            return List.of();
        }
        List<IntentionAction> result = new ArrayList<>();
        for (ProjectViewProblem.QuickFix it : receiver.quickFixes()) {
            IntentionAction action = toAction(it, element);
            if (action != null) {
                result.add(action);
            }
        }
        return result;
    }

    static @Nullable IntentionAction toAction(ProjectViewProblem.QuickFix receiver, PsiElement element) {
        ProjectViewPsiSection section = PsiTreeUtil.getParentOfType(element, ProjectViewPsiSection.class, false);
        if (section == null) {
            return null;
        }
        return switch (receiver) {
            case ProjectViewProblem.QuickFix.MergeIntoSection mergeIntoSection ->
                new MergeIntoSectionQuickFix(section, mergeIntoSection.targetSectionKey());
            case ProjectViewProblem.QuickFix.ReplaceWithIndex replaceWithIndex -> new ReplaceWithIndexQuickFix(section);
        };
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
