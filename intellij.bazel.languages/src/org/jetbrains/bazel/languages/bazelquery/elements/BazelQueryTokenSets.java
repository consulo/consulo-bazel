package org.jetbrains.bazel.languages.bazelquery.elements;

import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;

import static org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes.COMMENT;

public final class BazelQueryTokenSets {
    private BazelQueryTokenSets() {
    }

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);

    public static final TokenSet COMMENTS = TokenSet.create(COMMENT);

    public static final TokenSet WORDS =
        TokenSet.create(
            BazelQueryTokenTypes.UNQUOTED_WORD,
            BazelQueryTokenTypes.SQ_WORD,
            BazelQueryTokenTypes.DQ_WORD,
            BazelQueryTokenTypes.SQ_UNFINISHED,
            BazelQueryTokenTypes.DQ_UNFINISHED,
            BazelQueryTokenTypes.SQ_EMPTY,
            BazelQueryTokenTypes.DQ_EMPTY,
            BazelQueryTokenTypes.ERR_WORD
        );

    public static final TokenSet PATTERNS =
        TokenSet.create(
            BazelQueryTokenTypes.UNQUOTED_WORD,
            BazelQueryTokenTypes.SQ_PATTERN,
            BazelQueryTokenTypes.DQ_PATTERN
        );

    public static final TokenSet FLAGS =
        TokenSet.create(
            BazelQueryTokenTypes.FLAG,
            BazelQueryTokenTypes.FLAG_NO_VAL,
            BazelQueryTokenTypes.UNFINISHED_FLAG
        );

    public static final TokenSet FLAG_VALS =
        TokenSet.create(
            BazelQueryTokenTypes.UNQUOTED_VAL,
            BazelQueryTokenTypes.SQ_VAL,
            BazelQueryTokenTypes.DQ_VAL,
            BazelQueryTokenTypes.UNFINISHED_VAL
        );

    public static final TokenSet OPERATIONS =
        TokenSet.create(
            BazelQueryTokenTypes.UNION,
            BazelQueryTokenTypes.EXCEPT,
            BazelQueryTokenTypes.INTERSECT,
            BazelQueryTokenTypes.LET,
            BazelQueryTokenTypes.SET,
            BazelQueryTokenTypes.IN
        );

    public static final TokenSet INFIX_OPERATORS =
        TokenSet.create(
            BazelQueryTokenTypes.UNION,
            BazelQueryTokenTypes.EXCEPT,
            BazelQueryTokenTypes.INTERSECT
        );

    public static final TokenSet COMMANDS =
        TokenSet.create(
            BazelQueryTokenTypes.ALLPATHS,
            BazelQueryTokenTypes.ATTR,
            BazelQueryTokenTypes.BUILDFILES,
            BazelQueryTokenTypes.RBUILDFILES,
            BazelQueryTokenTypes.DEPS,
            BazelQueryTokenTypes.FILTER,
            BazelQueryTokenTypes.KIND,
            BazelQueryTokenTypes.LABELS,
            BazelQueryTokenTypes.LOADFILES,
            BazelQueryTokenTypes.RDEPS,
            BazelQueryTokenTypes.ALLRDEPS,
            BazelQueryTokenTypes.SAME_PKG_DIRECT_RDEPS,
            BazelQueryTokenTypes.SIBLINGS,
            BazelQueryTokenTypes.SOME,
            BazelQueryTokenTypes.SOMEPATH,
            BazelQueryTokenTypes.TESTS,
            BazelQueryTokenTypes.VISIBLE
        );
}
