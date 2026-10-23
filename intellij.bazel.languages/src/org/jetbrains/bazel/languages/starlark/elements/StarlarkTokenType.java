package org.jetbrains.bazel.languages.starlark.elements;

import consulo.language.ast.IElementType;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;

public class StarlarkTokenType extends IElementType {
    public StarlarkTokenType(String debugName) {
        super(debugName, StarlarkLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "Starlark:" + super.toString();
    }
}
