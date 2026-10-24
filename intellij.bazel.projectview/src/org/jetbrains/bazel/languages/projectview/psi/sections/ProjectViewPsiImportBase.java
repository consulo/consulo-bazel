package org.jetbrains.bazel.languages.projectview.psi.sections;

import consulo.language.ast.ASTNode;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement;
import org.jspecify.annotations.Nullable;

public abstract class ProjectViewPsiImportBase extends ProjectViewBaseElement {
    public ProjectViewPsiImportBase(ASTNode node) {
        super(node);
    }

    public @Nullable ProjectViewPsiImportItem getImportPath() {
        return PsiTreeUtil.getChildOfType(this, ProjectViewPsiImportItem.class);
    }

    public abstract boolean isImportRequired();
}
