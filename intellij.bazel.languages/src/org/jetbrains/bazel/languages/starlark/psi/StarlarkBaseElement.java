package org.jetbrains.bazel.languages.starlark.psi;

import consulo.content.scope.SearchScope;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.scope.LocalSearchScope;

public abstract class StarlarkBaseElement extends ASTWrapperPsiElement implements StarlarkElement {
    public StarlarkBaseElement(ASTNode node) {
        super(node);
    }

    @Override
    public void accept(PsiElementVisitor visitor) {
        if (visitor instanceof StarlarkElementVisitor starlarkVisitor) {
            acceptVisitor(starlarkVisitor);
        }
        else {
            super.accept(visitor);
        }
    }

    protected abstract void acceptVisitor(StarlarkElementVisitor visitor);

    @Override
    public SearchScope getUseScope() {
        return new LocalSearchScope(getContainingFile());
    }

    public static TextRange relativeTo(TextRange receiver, PsiElement element) {
        return receiver.shiftLeft(element.getTextRange().getStartOffset());
    }
}
