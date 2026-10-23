package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;

import java.util.List;

public class StarlarkStatementList extends StarlarkBaseElement {
    public StarlarkStatementList(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitStatementListImpl(this);
    }

    public List<StarlarkAssignmentStatement> getAssignments() {
        return List.of(findChildrenByClass(StarlarkAssignmentStatement.class));
    }

    public List<StarlarkFunctionDeclaration> getFunctionDeclarations() {
        return List.of(findChildrenByClass(StarlarkFunctionDeclaration.class));
    }
}
