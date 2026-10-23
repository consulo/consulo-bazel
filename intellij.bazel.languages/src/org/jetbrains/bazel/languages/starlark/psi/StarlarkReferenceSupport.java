package org.jetbrains.bazel.languages.starlark.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Builds references for Starlark PSI elements.
 * The PSI has no references when no implementation is registered.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface StarlarkReferenceSupport {
    static @Nullable StarlarkReferenceSupport getInstance() {
        return Application.get().getExtensionPoint(StarlarkReferenceSupport.class).findFirstSafe(it -> true);
    }

    @Nullable PsiReference getReference(StarlarkReferenceExpression element);

    PsiReference getReference(StarlarkTargetExpression element);

    PsiReference getReference(StarlarkArgumentExpression element);

    @Nullable PsiReference getReference(StarlarkNamedArgumentExpression element);

    // consulo: upstream PsiSymbolReference own references are merged into getReferences()
    Collection<PsiReference> getOwnReferences(StarlarkNamedArgumentExpression element);

    PsiReference getReference(StarlarkGlobExpression element);

    @Nullable PsiReference getReference(StarlarkStringLiteralExpression element);

    // consulo: upstream PsiSymbolReference own references are merged into getReferences()
    Collection<PsiReference> getOwnReferences(StarlarkCallExpression element);
}
