package org.jetbrains.bazel.languages.starlark.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionConfidence;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ThreeState;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression;

@ExtensionImpl
public class StarlarkCompletionConfidence extends CompletionConfidence {
    // consulo: Consulo's shouldSkipAutopopup has no Editor parameter
    @Override
    public ThreeState shouldSkipAutopopup(
        PsiElement contextElement,
        PsiFile psiFile,
        int offset
    ) {
        PsiElement parent = contextElement.getParent();
        if (parent instanceof StarlarkIntegerLiteralExpression || parent instanceof StarlarkFloatLiteralExpression) {
            return ThreeState.YES;
        }
        else {
            return ThreeState.UNSURE;
        }
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
