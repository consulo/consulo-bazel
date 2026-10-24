package org.jetbrains.bazel.languages.projectview.psi;

import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElementVisitor;
import org.jspecify.annotations.Nullable;

public abstract class ProjectViewBaseElement extends ASTWrapperPsiElement implements ProjectViewElement {
    public ProjectViewBaseElement(ASTNode node) {
        super(node);
    }

    @Override
    public void accept(PsiElementVisitor visitor) {
        if (visitor instanceof ProjectViewElementVisitor projectViewVisitor) {
            acceptVisitor(projectViewVisitor);
        }
        else {
            super.accept(visitor);
        }
    }

    protected abstract void acceptVisitor(ProjectViewElementVisitor visitor);

    @Override
    public @Nullable String getName() {
        return getNode().getText();
    }
}
