package org.jetbrains.bazel.languages.bazelrc.lexer;

import consulo.language.lexer.FlexAdapter;

public class BazelrcLexer extends FlexAdapter {
    public BazelrcLexer() {
        super(new _BazelrcLexer(null));
    }
}
