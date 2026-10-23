package org.jetbrains.bazel.languages.bazelrc.elements;

import consulo.language.ast.IElementType;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;

public class BazelrcElementType extends IElementType {
    public BazelrcElementType(String debugName) {
        super(debugName, BazelrcLanguage.INSTANCE);
    }
}
