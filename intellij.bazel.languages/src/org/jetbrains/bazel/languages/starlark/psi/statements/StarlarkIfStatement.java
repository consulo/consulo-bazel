package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

import java.util.List;

public class StarlarkIfStatement extends StarlarkBaseElement implements StarlarkStatementContainer {
    public StarlarkIfStatement(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitIfStatement(this);
    }

    @Override
    public List<StarlarkStatementList> getStatementLists() {
        return List.of(findChildrenByClass(StarlarkStatementList.class));
    }
}
