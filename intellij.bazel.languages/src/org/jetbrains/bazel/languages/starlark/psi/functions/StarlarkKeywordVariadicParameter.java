package org.jetbrains.bazel.languages.starlark.psi.functions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

public class StarlarkKeywordVariadicParameter extends StarlarkParameter {
    public StarlarkKeywordVariadicParameter(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitKeywordVariadicParameter(this);
    }
}
