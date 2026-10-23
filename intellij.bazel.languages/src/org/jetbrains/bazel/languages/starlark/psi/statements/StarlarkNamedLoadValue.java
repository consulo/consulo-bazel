package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;

public class StarlarkNamedLoadValue extends StarlarkNamedElement implements StarlarkLoadValue {
    public StarlarkNamedLoadValue(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitNamedLoadValue(this);
    }
}
