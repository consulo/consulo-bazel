package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.StarlarkUtils;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType;
import org.jspecify.annotations.Nullable;

public interface StarlarkAssigningStatement extends PsiElement {
    StarlarkTokenType operator();

    default @Nullable PsiElement leftHandSide() {
        return StarlarkUtils.nearestRelevantBeforeOperator(getFirstChild());
    }

    default @Nullable PsiElement rightHandSide() {
        return StarlarkUtils.nearestRelevantChildAfter(this, operator());
    }
}
