package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jspecify.annotations.Nullable;

public class StarlarkParenthesizedExpression extends StarlarkBaseElement {
    public StarlarkParenthesizedExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitParenthesizedExpression(this);
    }

    public @Nullable StarlarkTupleExpression getTuple() {
        return findChildByClass(StarlarkTupleExpression.class);
    }
}
