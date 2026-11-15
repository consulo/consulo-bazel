package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement;
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkParameterLookupElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class StarlarkArgumentReference extends PsiReferenceBase<StarlarkArgumentElement> {
    public StarlarkArgumentReference(StarlarkArgumentElement element, TextRange range) {
        super(element, range, true);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        return null;
    }

    @Override
    @RequiredReadAction
    public StarlarkLookupElement[] getVariants() {
        StarlarkFunctionDeclaration originalFunction = resolveFunction();
        if (originalFunction == null) {
            return new StarlarkLookupElement[0];
        }
        Set<String> passedArgumentNames = getArgumentNames();
        return originalFunction
            .getNamedParameters()
            .stream()
            .filter(it -> !passedArgumentNames.contains(it.getName()))
            .map(it -> new StarlarkParameterLookupElement(it))
            .toArray(StarlarkLookupElement[]::new);
    }

    @RequiredReadAction
    public @Nullable StarlarkFunctionDeclaration resolveFunction() {
        StarlarkCallExpression functionCall = getFunctionCall();
        PsiElement calledExpression = functionCall == null ? null : functionCall.getCalledExpression();
        PsiReference reference = calledExpression == null ? null : calledExpression.getReference();
        PsiElement resolved = reference == null ? null : reference.resolve();
        return resolved instanceof StarlarkFunctionDeclaration functionDeclaration ? functionDeclaration : null;
    }

    private Set<String> getArgumentNames() {
        StarlarkCallExpression functionCall = getFunctionCall();
        StarlarkArgumentList argumentList = functionCall == null ? null : functionCall.getArgumentList();
        return argumentList != null ? argumentList.getArgumentNames() : Set.of();
    }

    private @Nullable StarlarkCallExpression getFunctionCall() {
        return PsiTreeUtil.getParentOfType(myElement, StarlarkCallExpression.class);
    }
}
