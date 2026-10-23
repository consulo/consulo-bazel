package org.jetbrains.bazel.languages.starlark.psi.functions;

import consulo.language.psi.PsiElement;

import java.util.List;

public interface StarlarkCallable extends PsiElement {
    List<StarlarkParameter> getNamedParameters();

    List<StarlarkParameter> getParameters();
}
