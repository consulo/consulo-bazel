package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

public class StarlarkStarArgumentExpression extends StarlarkBaseElement implements StarlarkArgumentElement {
    public StarlarkStarArgumentExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitStarArgumentExpression(this);
    }
}
