package org.jetbrains.bazel.languages.bazelquery.psi;

import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElementVisitor;

public abstract class BazelQueryBaseElement extends ASTWrapperPsiElement implements BazelQueryElement {
    public BazelQueryBaseElement(ASTNode node) {
        super(node);
    }

    @Override
    public void accept(PsiElementVisitor visitor) {
        if (visitor instanceof BazelQueryElementVisitor bazelQueryVisitor) {
            acceptVisitor(bazelQueryVisitor);
        }
        else {
            super.accept(visitor);
        }
    }

    protected abstract void acceptVisitor(BazelQueryElementVisitor visitor);
}
