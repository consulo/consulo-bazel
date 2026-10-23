package org.jetbrains.bazel.languages.starlark.psi.functions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

import java.util.List;

public class StarlarkParameterList extends StarlarkBaseElement {
    public StarlarkParameterList(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitParameterList(this);
    }

    public List<StarlarkParameter> getParameters() {
        return List.of(findChildrenByClass(StarlarkParameter.class));
    }
}
