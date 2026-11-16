package org.jetbrains.bazel.languages.projectview.annotation.suppress;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.ChangeLocalityDetector;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jspecify.annotations.Nullable;

/**
 * A comment can suppress problems of the elements below it, so a change in a comment highlights the whole file again.
 */
@ExtensionImpl
public class ProjectViewSuppressionChangeLocalityDetector implements ChangeLocalityDetector {
    public ProjectViewSuppressionChangeLocalityDetector() {
    }

    @Override
    public @Nullable PsiElement getChangeHighlightingDirtyScopeFor(PsiElement changedElement) {
        if (changedElement instanceof PsiComment && PsiUtilCore.getElementType(changedElement) == ProjectViewTokenType.COMMENT) {
            return changedElement.getContainingFile();
        }
        else {
            return null;
        }
    }
}
