package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList;

import java.util.List;

public class StarlarkLambdaExpression extends StarlarkBaseElement implements StarlarkCallable {
    public StarlarkLambdaExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitLambdaExpression(this);
    }

    @Override
    public List<StarlarkParameter> getNamedParameters() {
        return getParameters()
            .stream()
            .filter(it -> it instanceof StarlarkMandatoryParameter || it instanceof StarlarkOptionalParameter)
            .toList();
    }

    @Override
    public List<StarlarkParameter> getParameters() {
        StarlarkParameterList parameterList = findChildByClass(StarlarkParameterList.class);
        return parameterList == null ? List.of() : parameterList.getParameters();
    }
}
