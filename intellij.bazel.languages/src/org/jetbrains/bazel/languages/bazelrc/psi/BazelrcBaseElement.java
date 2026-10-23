package org.jetbrains.bazel.languages.bazelrc.psi;

import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElementVisitor;

public abstract class BazelrcBaseElement extends ASTWrapperPsiElement implements BazelrcElement {
    public BazelrcBaseElement(ASTNode node) {
        super(node);
    }

    @Override
    public void accept(PsiElementVisitor visitor) {
        if (visitor instanceof BazelrcElementVisitor bazelrcVisitor) {
            acceptVisitor(bazelrcVisitor);
        }
        else {
            super.accept(visitor);
        }
    }

    protected abstract void acceptVisitor(BazelrcElementVisitor visitor);
}
