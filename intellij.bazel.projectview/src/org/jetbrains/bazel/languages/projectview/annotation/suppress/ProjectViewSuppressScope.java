package org.jetbrains.bazel.languages.projectview.annotation.suppress;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;
import org.jspecify.annotations.Nullable;

/**
 * {@code actionKey} and {@code batchKey} are keys of {@code BazelProjectViewLocalize}.
 */
enum ProjectViewSuppressScope {
    Item("suppress.for.item", "suppress.batch.for.item") {
        @Override
        public @Nullable PsiElement containerOf(PsiElement element) {
            ProjectViewPsiSectionItem item = PsiTreeUtil.getParentOfType(element, ProjectViewPsiSectionItem.class, false);
            if (item == null) {
                return null;
            }
            PsiElement prevLeaf = PsiTreeUtil.prevLeaf(item);
            return prevLeaf != null && PsiUtilCore.getElementType(prevLeaf) == ProjectViewTokenType.INDENT ? item : null;
        }
    },
    Section("suppress.for.section", "suppress.batch.for.section") {
        @Override
        public @Nullable PsiElement containerOf(PsiElement element) {
            return PsiTreeUtil.getParentOfType(element, ProjectViewPsiSection.class, false);
        }
    };

    private final String myActionKey;
    private final String myBatchKey;

    ProjectViewSuppressScope(String actionKey, String batchKey) {
        myActionKey = actionKey;
        myBatchKey = batchKey;
    }

    public String getActionKey() {
        return myActionKey;
    }

    public String getBatchKey() {
        return myBatchKey;
    }

    public abstract @Nullable PsiElement containerOf(PsiElement element);
}
