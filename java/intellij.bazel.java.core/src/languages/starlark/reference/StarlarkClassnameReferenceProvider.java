package org.jetbrains.bazel.languages.starlark.reference;

import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.util.ProcessingContext;
import org.jetbrains.bazel.languages.starlark.bazel.java.StarlarkClassParametersProvider;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.references.java.StarlarkClassnameReference;

import java.util.Set;
import java.util.stream.Collectors;

public class StarlarkClassnameReferenceProvider extends PsiReferenceProvider {
    public static final StarlarkClassnameReferenceProvider INSTANCE = new StarlarkClassnameReferenceProvider();

    private StarlarkClassnameReferenceProvider() {
    }

    private static Set<String> classParametersList() {
        return Application.get().getExtensionList(StarlarkClassParametersProvider.class).stream()
            .flatMap(it -> it.getClassnameParameters().stream())
            .collect(Collectors.toSet());
    }

    @Override
    public PsiReference[] getReferencesByElement(
        PsiElement element,
        ProcessingContext context
    ) {
        if (element instanceof StarlarkStringLiteralExpression stringLiteral && isClassnameValue(element)) {
            return new PsiReference[]{new StarlarkClassnameReference(stringLiteral)};
        }
        return PsiReference.EMPTY_ARRAY;
    }

    // consulo: upstream object member, static here (called as StarlarkClassnameReferenceProvider.isClassnameValue)
    public static boolean isClassnameValue(PsiElement element) {
        return element.getParent() instanceof StarlarkNamedArgumentExpression namedArgument
            && classParametersList().contains(namedArgument.getName());
    }
}
