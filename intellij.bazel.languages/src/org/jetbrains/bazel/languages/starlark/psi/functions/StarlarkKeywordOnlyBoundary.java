package org.jetbrains.bazel.languages.starlark.psi.functions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

public class StarlarkKeywordOnlyBoundary extends StarlarkBaseElement {
    public StarlarkKeywordOnlyBoundary(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        // Pure marker which doesn't need to be visited.
    }
}
