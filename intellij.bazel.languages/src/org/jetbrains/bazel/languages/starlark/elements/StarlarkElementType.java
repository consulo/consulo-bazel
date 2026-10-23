package org.jetbrains.bazel.languages.starlark.elements;

import consulo.language.ast.IElementType;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;

public class StarlarkElementType extends IElementType {
    public StarlarkElementType(String debugName) {
        super(debugName, StarlarkLanguage.INSTANCE);
    }
}
