package org.jetbrains.bazel.languages.starlark.psi.functions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class StarlarkArgumentList extends StarlarkBaseElement {
    public StarlarkArgumentList(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitArgumentList(this);
    }

    public Set<String> getArgumentNames() {
        return Arrays.stream(findChildrenByClass(StarlarkNamedArgumentExpression.class))
            .map(StarlarkNamedArgumentExpression::getName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public @Nullable String getNameArgumentValue() {
        StarlarkNamedArgumentExpression nameArgument = getNameArgument();
        return nameArgument != null ? nameArgument.getArgumentStringValue() : null;
    }

    public @Nullable StarlarkNamedArgumentExpression getSrcsArgument() {
        return Arrays.stream(findChildrenByClass(StarlarkNamedArgumentExpression.class))
            .filter(StarlarkNamedArgumentExpression::isSrcsArgument)
            .findFirst()
            .orElse(null);
    }

    public @Nullable StarlarkNamedArgumentExpression getResourcesArgument() {
        return Arrays.stream(findChildrenByClass(StarlarkNamedArgumentExpression.class))
            .filter(StarlarkNamedArgumentExpression::isResourcesArgument)
            .findFirst()
            .orElse(null);
    }

    public @Nullable StarlarkNamedArgumentExpression getNameArgument() {
        return Arrays.stream(findChildrenByClass(StarlarkNamedArgumentExpression.class))
            .filter(StarlarkNamedArgumentExpression::isNameArgument)
            .findFirst()
            .orElse(null);
    }

    public @Nullable StarlarkNamedArgumentExpression getKeywordArgument(String name) {
        return Arrays.stream(findChildrenByClass(StarlarkNamedArgumentExpression.class))
            .filter(it -> Objects.equals(it.getName(), name))
            .findFirst()
            .orElse(null);
    }

    public StarlarkArgumentElement[] getArguments() {
        return findChildrenByClass(StarlarkArgumentElement.class);
    }

    public @Nullable StarlarkNamedArgumentExpression getDepsArgument() {
        StarlarkNamedArgumentExpression[] arguments = findChildrenByClass(StarlarkNamedArgumentExpression.class);
        StarlarkNamedArgumentExpression depsArgument = Arrays.stream(arguments)
            .filter(StarlarkNamedArgumentExpression::isDepsArgument)
            .findFirst()
            .orElse(null);
        if (depsArgument != null) {
            return depsArgument;
        }
        return Arrays.stream(arguments)
            .filter(StarlarkNamedArgumentExpression::isDependenciesArgument)
            .findFirst()
            .orElse(null);
    }
}
