package org.jetbrains.bazel.languages.starlark.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.documentation.DocumentationTarget;
import consulo.bazel.documentation.PsiDocumentationTargetProvider;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class BazelRuleCallPsiDocumentationTargetProvider implements PsiDocumentationTargetProvider {

    @Override
    public @Nullable DocumentationTarget documentationTarget(PsiElement element, @Nullable PsiElement originalElement) {
        if (!StarlarkCallExpression.isRuleTarget(element)) {
            return null;
        }
        // consulo: upstream smart cast from the isRuleTarget() contract
        return new BazelRuleCallDocumentationTarget((StarlarkCallExpression) element);
    }
}
