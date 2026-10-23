package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport;
import org.jspecify.annotations.Nullable;

public class StarlarkArgumentExpression extends StarlarkBaseElement implements StarlarkArgumentElement {
    public StarlarkArgumentExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitArgumentExpression(this);
    }

    @Override
    public @Nullable PsiReference getReference() {
        StarlarkReferenceSupport support = StarlarkReferenceSupport.getInstance();
        return support == null ? null : support.getReference(this);
    }
}
