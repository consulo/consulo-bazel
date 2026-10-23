package org.jetbrains.bazel.languages.starlark.lexer;

import consulo.language.lexer.FlexAdapter;

public class StarlarkHighlightingLexer extends FlexAdapter {
    public StarlarkHighlightingLexer() {
        super(new _StarlarkLexer(null));
    }
}
