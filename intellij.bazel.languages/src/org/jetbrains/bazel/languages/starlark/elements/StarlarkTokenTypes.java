package org.jetbrains.bazel.languages.starlark.elements;

public final class StarlarkTokenTypes {
    private StarlarkTokenTypes() {
    }

    // Whitespaces
    public static final StarlarkTokenType SPACE = new StarlarkTokenType("SPACE");

    public static final StarlarkTokenType TAB = new StarlarkTokenType("TAB");

    public static final StarlarkTokenType LINE_CONTINUATION = new StarlarkTokenType("LINE_CONTINUATION");

    public static final StarlarkTokenType LINE_BREAK = new StarlarkTokenType("LINE_BREAK");

    // Significant whitespaces
    public static final StarlarkTokenType INDENT = new StarlarkTokenType("INDENT");

    public static final StarlarkTokenType DEDENT = new StarlarkTokenType("DEDENT");

    public static final StarlarkTokenType INCONSISTENT_DEDENT = new StarlarkTokenType("INCONSISTENT_DEDENT");

    public static final StarlarkTokenType STATEMENT_BREAK = new StarlarkTokenType("STATEMENT_BREAK");

    // Comment
    public static final StarlarkTokenType COMMENT = new StarlarkTokenType("COMMENT");

    // Literals
    public static final StarlarkTokenType STRING = new StarlarkTokenType("STRING");

    public static final StarlarkTokenType BYTES = new StarlarkTokenType("BYTES");

    public static final StarlarkTokenType INT = new StarlarkTokenType("INT");

    public static final StarlarkTokenType FLOAT = new StarlarkTokenType("FLOAT");

    // Keywords
    public static final StarlarkTokenType AND_KEYWORD = new StarlarkTokenType("and");

    public static final StarlarkTokenType BREAK_KEYWORD = new StarlarkTokenType("break");

    public static final StarlarkTokenType CONTINUE_KEYWORD = new StarlarkTokenType("continue");

    public static final StarlarkTokenType DEF_KEYWORD = new StarlarkTokenType("def");

    public static final StarlarkTokenType ELIF_KEYWORD = new StarlarkTokenType("elif");

    public static final StarlarkTokenType ELSE_KEYWORD = new StarlarkTokenType("else");

    public static final StarlarkTokenType FOR_KEYWORD = new StarlarkTokenType("for");

    public static final StarlarkTokenType IF_KEYWORD = new StarlarkTokenType("if");

    public static final StarlarkTokenType IN_KEYWORD = new StarlarkTokenType("in");

    public static final StarlarkTokenType LAMBDA_KEYWORD = new StarlarkTokenType("lambda");

    public static final StarlarkTokenType LOAD_KEYWORD = new StarlarkTokenType("load");

    public static final StarlarkTokenType NOT_KEYWORD = new StarlarkTokenType("not");

    public static final StarlarkTokenType OR_KEYWORD = new StarlarkTokenType("or");

    public static final StarlarkTokenType PASS_KEYWORD = new StarlarkTokenType("pass");

    public static final StarlarkTokenType RETURN_KEYWORD = new StarlarkTokenType("return");

    public static final StarlarkTokenType NONE_KEYWORD = new StarlarkTokenType("None");

    public static final StarlarkTokenType TRUE_KEYWORD = new StarlarkTokenType("True");

    public static final StarlarkTokenType FALSE_KEYWORD = new StarlarkTokenType("False");

    // Possible future keywords
    public static final StarlarkTokenType AS_KEYWORD = new StarlarkTokenType("as");

    public static final StarlarkTokenType ASSERT_KEYWORD = new StarlarkTokenType("assert");

    public static final StarlarkTokenType ASYNC_KEYWORD = new StarlarkTokenType("async");

    public static final StarlarkTokenType AWAIT_KEYWORD = new StarlarkTokenType("await");

    public static final StarlarkTokenType CLASS_KEYWORD = new StarlarkTokenType("class");

    public static final StarlarkTokenType DEL_KEYWORD = new StarlarkTokenType("del");

    public static final StarlarkTokenType EXCEPT_KEYWORD = new StarlarkTokenType("except");

    public static final StarlarkTokenType FINALLY_KEYWORD = new StarlarkTokenType("finally");

    public static final StarlarkTokenType FROM_KEYWORD = new StarlarkTokenType("from");

    public static final StarlarkTokenType GLOBAL_KEYWORD = new StarlarkTokenType("global");

    public static final StarlarkTokenType IMPORT_KEYWORD = new StarlarkTokenType("import");

    public static final StarlarkTokenType IS_KEYWORD = new StarlarkTokenType("is");

    public static final StarlarkTokenType NONLOCAL_KEYWORD = new StarlarkTokenType("nonlocal");

    public static final StarlarkTokenType RAISE_KEYWORD = new StarlarkTokenType("raise");

    public static final StarlarkTokenType TRY_KEYWORD = new StarlarkTokenType("try");

    public static final StarlarkTokenType WHILE_KEYWORD = new StarlarkTokenType("while");

    public static final StarlarkTokenType WITH_KEYWORD = new StarlarkTokenType("with");

    public static final StarlarkTokenType YIELD_KEYWORD = new StarlarkTokenType("yield");

    // Identifier
    public static final StarlarkTokenType IDENTIFIER = new StarlarkTokenType("IDENTIFIER");

    // Punctuation
    public static final StarlarkTokenType PLUS = new StarlarkTokenType("+");

    public static final StarlarkTokenType MINUS = new StarlarkTokenType("-");

    public static final StarlarkTokenType MULT = new StarlarkTokenType("*");

    public static final StarlarkTokenType DIV = new StarlarkTokenType("/");

    public static final StarlarkTokenType FLOORDIV = new StarlarkTokenType("//");

    public static final StarlarkTokenType PERC = new StarlarkTokenType("%");

    public static final StarlarkTokenType EXP = new StarlarkTokenType("**");

    public static final StarlarkTokenType TILDE = new StarlarkTokenType("~");

    public static final StarlarkTokenType AND = new StarlarkTokenType("&");

    public static final StarlarkTokenType OR = new StarlarkTokenType("|");

    public static final StarlarkTokenType XOR = new StarlarkTokenType("^");

    public static final StarlarkTokenType LTLT = new StarlarkTokenType("<<");

    public static final StarlarkTokenType GTGT = new StarlarkTokenType(">>");

    public static final StarlarkTokenType DOT = new StarlarkTokenType(".");

    public static final StarlarkTokenType COMMA = new StarlarkTokenType(",");

    public static final StarlarkTokenType EQ = new StarlarkTokenType("=");

    public static final StarlarkTokenType SEMICOLON = new StarlarkTokenType(";");

    public static final StarlarkTokenType COLON = new StarlarkTokenType(":");

    public static final StarlarkTokenType LPAR = new StarlarkTokenType("(");

    public static final StarlarkTokenType RPAR = new StarlarkTokenType(")");

    public static final StarlarkTokenType LBRACKET = new StarlarkTokenType("[");

    public static final StarlarkTokenType RBRACKET = new StarlarkTokenType("]");

    public static final StarlarkTokenType LBRACE = new StarlarkTokenType("{");

    public static final StarlarkTokenType RBRACE = new StarlarkTokenType("}");

    public static final StarlarkTokenType LT = new StarlarkTokenType("<");

    public static final StarlarkTokenType GT = new StarlarkTokenType(">");

    public static final StarlarkTokenType LE = new StarlarkTokenType("<=");

    public static final StarlarkTokenType GE = new StarlarkTokenType(">=");

    public static final StarlarkTokenType EQEQ = new StarlarkTokenType("==");

    public static final StarlarkTokenType NE = new StarlarkTokenType("!=");

    public static final StarlarkTokenType PLUSEQ = new StarlarkTokenType("+=");

    public static final StarlarkTokenType MINUSEQ = new StarlarkTokenType("-=");

    public static final StarlarkTokenType MULTEQ = new StarlarkTokenType("*=");

    public static final StarlarkTokenType DIVEQ = new StarlarkTokenType("/=");

    public static final StarlarkTokenType FLOORDIVEQ = new StarlarkTokenType("//=");

    public static final StarlarkTokenType PERCEQ = new StarlarkTokenType("%=");

    public static final StarlarkTokenType ANDEQ = new StarlarkTokenType("&=");

    public static final StarlarkTokenType OREQ = new StarlarkTokenType("|=");

    public static final StarlarkTokenType XOREQ = new StarlarkTokenType("^=");

    public static final StarlarkTokenType LTLTEQ = new StarlarkTokenType("<<=");

    public static final StarlarkTokenType GTGTEQ = new StarlarkTokenType(">>=");
}
