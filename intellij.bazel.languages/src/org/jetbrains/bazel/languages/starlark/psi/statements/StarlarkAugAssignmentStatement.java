package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

public class StarlarkAugAssignmentStatement extends StarlarkBaseElement implements StarlarkAssigningStatement {
    public StarlarkAugAssignmentStatement(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitAugAssignmentStatement(this);
    }

    @Override
    public StarlarkTokenType operator() {
        return (StarlarkTokenType) getNode().getChildren(StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS)[0].getElementType();
    }
}
