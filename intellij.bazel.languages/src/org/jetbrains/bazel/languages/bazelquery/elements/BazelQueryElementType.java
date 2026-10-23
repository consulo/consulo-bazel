package org.jetbrains.bazel.languages.bazelquery.elements;

import consulo.language.ast.IElementType;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage;

public class BazelQueryElementType extends IElementType {
    public BazelQueryElementType(String debugName) {
        super(debugName, BazelQueryLanguage.INSTANCE);
    }

    public static class BazelQueryFlagsElementType extends IElementType {
        public BazelQueryFlagsElementType(String debugName) {
            super(debugName, BazelQueryFlagsLanguage.INSTANCE);
        }
    }
}
