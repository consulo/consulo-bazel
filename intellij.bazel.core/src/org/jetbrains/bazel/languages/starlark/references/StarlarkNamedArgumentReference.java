package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class StarlarkNamedArgumentReference extends StarlarkArgumentReference {
    public StarlarkNamedArgumentReference(StarlarkNamedArgumentExpression element, TextRange range) {
        super(element, range);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        String argumentName = myElement.getName();
        if (argumentName == null) {
            return null;
        }
        StarlarkFunctionDeclaration function = resolveFunction();
        if (function == null) {
            return null;
        }
        List<StarlarkParameter> parameters = function.getParameters();
        for (StarlarkParameter it : parameters) {
            if (Objects.equals(it.getName(), argumentName)) {
                return it;
            }
        }
        for (StarlarkParameter it : parameters) {
            if (it instanceof StarlarkKeywordVariadicParameter) {
                return it;
            }
        }
        return null;
    }
}
