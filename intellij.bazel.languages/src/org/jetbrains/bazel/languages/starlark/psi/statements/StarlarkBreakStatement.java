package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

public class StarlarkBreakStatement extends StarlarkBaseElement {
    public StarlarkBreakStatement(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitBreakStatement(this);
    }
}
