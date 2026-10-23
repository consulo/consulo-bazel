package org.jetbrains.bazel.languages.bazelquery.elements;

import consulo.language.ast.IElementType;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage;

import java.util.Locale;

public class BazelQueryTokenType extends IElementType {
    public BazelQueryTokenType(String debugName) {
        super(debugName, BazelQueryLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "BazelQuery:" + super.toString();
    }

    public String completionText() {
        return super.toString().toLowerCase(Locale.ROOT);
    }
}
