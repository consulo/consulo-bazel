package org.jetbrains.bazel.languages.bazelquery.lexer;

import consulo.language.lexer.FlexAdapter;

public class BazelQueryLexer extends FlexAdapter {
    public BazelQueryLexer() {
        super(new _BazelQueryLexer(null));
    }
}
