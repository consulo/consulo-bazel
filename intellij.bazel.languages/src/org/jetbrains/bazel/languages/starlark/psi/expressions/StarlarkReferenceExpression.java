package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport;
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableElement;
import org.jspecify.annotations.Nullable;

public class StarlarkReferenceExpression extends StarlarkBaseElement
    implements StarlarkLocalVariableElement, StarlarkQualifiableExpression {
    public StarlarkReferenceExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitReferenceExpression(this);
    }

    @Override
    public @Nullable PsiReference getReference() {
        StarlarkReferenceSupport support = StarlarkReferenceSupport.getInstance();
        return support == null ? null : support.getReference(this);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return findChildByType(StarlarkTokenTypes.IDENTIFIER);
    }

    @Override
    public @Nullable String getName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? null : nameIdentifier.getText();
    }
}
