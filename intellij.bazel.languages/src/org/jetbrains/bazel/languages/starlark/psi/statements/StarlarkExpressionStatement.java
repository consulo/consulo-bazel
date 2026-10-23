package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jspecify.annotations.Nullable;

public class StarlarkExpressionStatement extends StarlarkBaseElement {
    public StarlarkExpressionStatement(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitExpressionStatement(this);
    }

    public @Nullable StarlarkCallExpression callExpressionOrNull() {
        return findChildByClass(StarlarkCallExpression.class);
    }
}
