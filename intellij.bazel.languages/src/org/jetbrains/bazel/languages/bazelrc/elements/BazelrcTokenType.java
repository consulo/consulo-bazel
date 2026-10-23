package org.jetbrains.bazel.languages.bazelrc.elements;

import consulo.language.ast.IElementType;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;

public class BazelrcTokenType extends IElementType {
    public BazelrcTokenType(String debugName) {
        super(debugName, BazelrcLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "Bazelrc:" + super.toString();
    }
}
