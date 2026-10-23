package org.jetbrains.bazel.languages.bazelrc.elements;

import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;

import static org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.COMMAND;
import static org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.COMMENT;
import static org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.DOUBLE_QUOTE;
import static org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.FLAG;
import static org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.SINGLE_QUOTE;
import static org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.VALUE;

public final class BazelrcTokenSets {
    private BazelrcTokenSets() {
    }

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);

    public static final TokenSet COMMENTS = TokenSet.create(COMMENT);

    public static final TokenSet QUOTES = TokenSet.create(DOUBLE_QUOTE, SINGLE_QUOTE);

    public static final TokenSet BIBI = TokenSet.create(COMMAND, COMMENT, FLAG, VALUE);
}
