package org.jetbrains.bazel.languages.starlark.psi.statements;

import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jspecify.annotations.Nullable;

public interface StarlarkLoadValue extends StarlarkElement {
    default @Nullable StarlarkLoadStatement getLoadStatement() {
        return getParent() instanceof StarlarkLoadStatement loadStatement ? loadStatement : null;
    }

    default @Nullable StarlarkStringLiteralExpression getLoadValueExpression() {
        return getLastChild() instanceof StarlarkStringLiteralExpression expression ? expression : null;
    }

    default @Nullable String getLoadValueExpressionContent() {
        StarlarkStringLiteralExpression loadValueExpression = getLoadValueExpression();
        return loadValueExpression != null ? loadValueExpression.getStringContents() : null;
    }
}
