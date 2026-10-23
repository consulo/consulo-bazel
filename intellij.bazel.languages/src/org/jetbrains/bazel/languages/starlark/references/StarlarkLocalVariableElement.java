package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jspecify.annotations.Nullable;

public interface StarlarkLocalVariableElement extends StarlarkElement {
    @RequiredReadAction
    @Nullable PsiElement getNameIdentifier();
}
