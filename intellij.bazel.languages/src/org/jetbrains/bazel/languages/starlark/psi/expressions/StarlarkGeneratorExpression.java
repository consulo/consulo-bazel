package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

public class StarlarkGeneratorExpression extends StarlarkBaseElement {
    public StarlarkGeneratorExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitGeneratorExpression(this);
    }
}
