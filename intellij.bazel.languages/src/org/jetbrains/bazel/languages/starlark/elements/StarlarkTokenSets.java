package org.jetbrains.bazel.languages.starlark.elements;

import consulo.language.ast.TokenSet;

public final class StarlarkTokenSets {
    private StarlarkTokenSets() {
    }

    public static final TokenSet WHITESPACE =
        TokenSet.create(
            StarlarkTokenTypes.SPACE,
            StarlarkTokenTypes.TAB,
            StarlarkTokenTypes.LINE_BREAK,
            StarlarkTokenTypes.LINE_CONTINUATION
        );

    public static final TokenSet IDENTIFIER = TokenSet.create(StarlarkTokenTypes.IDENTIFIER);

    public static final TokenSet COMMENT = TokenSet.create(StarlarkTokenTypes.COMMENT);

    public static final TokenSet STRINGS = TokenSet.create(StarlarkTokenTypes.STRING, StarlarkTokenTypes.BYTES);

    public static final TokenSet OPEN_BRACKETS =
        TokenSet.create(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.LBRACE, StarlarkTokenTypes.LPAR);

    public static final TokenSet CLOSE_BRACKETS =
        TokenSet.create(StarlarkTokenTypes.RBRACKET, StarlarkTokenTypes.RBRACE, StarlarkTokenTypes.RPAR);

    public static final TokenSet RESERVED_KEYWORDS =
        TokenSet.create(
            StarlarkTokenTypes.AS_KEYWORD,
            StarlarkTokenTypes.ASSERT_KEYWORD,
            StarlarkTokenTypes.ASYNC_KEYWORD,
            StarlarkTokenTypes.AWAIT_KEYWORD,
            StarlarkTokenTypes.CLASS_KEYWORD,
            StarlarkTokenTypes.DEL_KEYWORD,
            StarlarkTokenTypes.EXCEPT_KEYWORD,
            StarlarkTokenTypes.FINALLY_KEYWORD,
            StarlarkTokenTypes.FROM_KEYWORD,
            StarlarkTokenTypes.GLOBAL_KEYWORD,
            StarlarkTokenTypes.IMPORT_KEYWORD,
            StarlarkTokenTypes.IS_KEYWORD,
            StarlarkTokenTypes.NONLOCAL_KEYWORD,
            StarlarkTokenTypes.RAISE_KEYWORD,
            StarlarkTokenTypes.TRY_KEYWORD,
            StarlarkTokenTypes.WHILE_KEYWORD,
            StarlarkTokenTypes.WITH_KEYWORD,
            StarlarkTokenTypes.YIELD_KEYWORD
        );

    public static final TokenSet PRIMARY_EXPRESSION_STARTERS =
        TokenSet.create(
            StarlarkTokenTypes.IDENTIFIER,
            StarlarkTokenTypes.INT,
            StarlarkTokenTypes.FLOAT,
            StarlarkTokenTypes.LPAR,
            StarlarkTokenTypes.LBRACKET,
            StarlarkTokenTypes.LBRACE,
            StarlarkTokenTypes.STRING,
            StarlarkTokenTypes.BYTES,
            StarlarkTokenTypes.NONE_KEYWORD,
            StarlarkTokenTypes.TRUE_KEYWORD,
            StarlarkTokenTypes.FALSE_KEYWORD
        );

    public static final TokenSet ENDS_OF_STATEMENT = TokenSet.create(StarlarkTokenTypes.STATEMENT_BREAK, StarlarkTokenTypes.SEMICOLON);

    public static final TokenSet COMPOUND_ASSIGN_OPERATIONS =
        TokenSet.create(
            StarlarkTokenTypes.PLUSEQ,
            StarlarkTokenTypes.MINUSEQ,
            StarlarkTokenTypes.MULTEQ,
            StarlarkTokenTypes.DIVEQ,
            StarlarkTokenTypes.FLOORDIVEQ,
            StarlarkTokenTypes.PERCEQ,
            StarlarkTokenTypes.ANDEQ,
            StarlarkTokenTypes.OREQ,
            StarlarkTokenTypes.XOREQ,
            StarlarkTokenTypes.LTLTEQ,
            StarlarkTokenTypes.GTGTEQ
        );

    public static final TokenSet COMPARISON_OPERATIONS =
        TokenSet.create(
            StarlarkTokenTypes.LT,
            StarlarkTokenTypes.GT,
            StarlarkTokenTypes.EQEQ,
            StarlarkTokenTypes.LE,
            StarlarkTokenTypes.GE,
            StarlarkTokenTypes.NE,
            StarlarkTokenTypes.IN_KEYWORD,
            StarlarkTokenTypes.NOT_KEYWORD
        );

    public static final TokenSet SHIFT_OPERATIONS = TokenSet.create(StarlarkTokenTypes.LTLT, StarlarkTokenTypes.GTGT);

    public static final TokenSet ADDITIVE_OPERATIONS = TokenSet.create(StarlarkTokenTypes.PLUS, StarlarkTokenTypes.MINUS);

    public static final TokenSet MULTIPLICATIVE_OPERATIONS =
        TokenSet.create(
            StarlarkTokenTypes.MULT,
            StarlarkTokenTypes.FLOORDIV,
            StarlarkTokenTypes.DIV,
            StarlarkTokenTypes.PERC
        );

    public static final TokenSet UNARY_OPERATIONS =
        TokenSet.create(
            StarlarkTokenTypes.PLUS,
            StarlarkTokenTypes.MINUS,
            StarlarkTokenTypes.TILDE,
            StarlarkTokenTypes.NOT_KEYWORD
        );

    public static final TokenSet BINARY_OPERATIONS =
        TokenSet.orSet(
            TokenSet.create(
                StarlarkTokenTypes.AND_KEYWORD,
                StarlarkTokenTypes.AND,
                StarlarkTokenTypes.OR_KEYWORD,
                StarlarkTokenTypes.OR,
                StarlarkTokenTypes.XOR,
                StarlarkTokenTypes.EXP,
                StarlarkTokenTypes.NOT_KEYWORD,
                StarlarkTokenTypes.IN_KEYWORD,
                StarlarkTokenTypes.IS_KEYWORD
            ),
            ADDITIVE_OPERATIONS,
            COMPARISON_OPERATIONS,
            MULTIPLICATIVE_OPERATIONS,
            SHIFT_OPERATIONS
        );
}
