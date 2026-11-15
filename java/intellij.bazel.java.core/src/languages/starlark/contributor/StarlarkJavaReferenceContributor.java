package org.jetbrains.bazel.languages.starlark.contributor;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceRegistrar;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.reference.StarlarkClassnameReferenceProvider;

@ExtensionImpl
public class StarlarkJavaReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StarlarkStringLiteralExpression.class),
            StarlarkClassnameReferenceProvider.INSTANCE
        );
    }

    // consulo: reference contributors are language-bound extensions (upstream XML language="Starlark")
    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
