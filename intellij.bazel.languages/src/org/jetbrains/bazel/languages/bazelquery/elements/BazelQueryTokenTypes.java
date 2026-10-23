package org.jetbrains.bazel.languages.bazelquery.elements;

public final class BazelQueryTokenTypes {
    private BazelQueryTokenTypes() {
    }

    public static final BazelQueryTokenType COMMENT = new BazelQueryTokenType("COMMENT");

    public static final BazelQueryTokenType DOUBLE_QUOTE = new BazelQueryTokenType("\"");

    public static final BazelQueryTokenType SINGLE_QUOTE = new BazelQueryTokenType("'");

    public static final BazelQueryTokenType LEFT_PAREN = new BazelQueryTokenType("(");

    public static final BazelQueryTokenType RIGHT_PAREN = new BazelQueryTokenType(")");

    public static final BazelQueryTokenType ALLPATHS = new BazelQueryTokenType("ALLPATHS");

    public static final BazelQueryTokenType ATTR = new BazelQueryTokenType("ATTR");

    public static final BazelQueryTokenType BUILDFILES = new BazelQueryTokenType("BUILDFILES");

    public static final BazelQueryTokenType RBUILDFILES = new BazelQueryTokenType("RBUILDFILES");

    public static final BazelQueryTokenType DEPS = new BazelQueryTokenType("DEPS");

    public static final BazelQueryTokenType FILTER = new BazelQueryTokenType("FILTER");

    public static final BazelQueryTokenType KIND = new BazelQueryTokenType("KIND");

    public static final BazelQueryTokenType LABELS = new BazelQueryTokenType("LABELS");

    public static final BazelQueryTokenType LOADFILES = new BazelQueryTokenType("LOADFILES");

    public static final BazelQueryTokenType RDEPS = new BazelQueryTokenType("RDEPS");

    public static final BazelQueryTokenType ALLRDEPS = new BazelQueryTokenType("ALLRDEPS");

    public static final BazelQueryTokenType SAME_PKG_DIRECT_RDEPS = new BazelQueryTokenType("SAME_PKG_DIRECT_RDEPS");

    public static final BazelQueryTokenType SIBLINGS = new BazelQueryTokenType("SIBLINGS");

    public static final BazelQueryTokenType SOME = new BazelQueryTokenType("SOME");

    public static final BazelQueryTokenType SOMEPATH = new BazelQueryTokenType("SOMEPATH");

    public static final BazelQueryTokenType TESTS = new BazelQueryTokenType("TESTS");

    public static final BazelQueryTokenType VISIBLE = new BazelQueryTokenType("VISIBLE");

    public static final BazelQueryTokenType UNQUOTED_WORD = new BazelQueryTokenType("UNQUOTED_WORD");

    public static final BazelQueryTokenType SQ_WORD = new BazelQueryTokenType("SQ_WORD");

    public static final BazelQueryTokenType DQ_WORD = new BazelQueryTokenType("DQ_WORD");

    public static final BazelQueryTokenType ERR_WORD = new BazelQueryTokenType("ERR_WORD");

    public static final BazelQueryTokenType SQ_PATTERN = new BazelQueryTokenType("SQ_PATTERN");

    public static final BazelQueryTokenType DQ_PATTERN = new BazelQueryTokenType("DQ_PATTERN");

    public static final BazelQueryTokenType COLON = new BazelQueryTokenType(":");

    public static final BazelQueryTokenType COMMA = new BazelQueryTokenType(",");

    public static final BazelQueryTokenType FLAG = new BazelQueryTokenType("FLAG");

    public static final BazelQueryTokenType FLAG_NO_VAL = new BazelQueryTokenType("FLAG_NO_VAL");

    public static final BazelQueryTokenType UNFINISHED_FLAG = new BazelQueryTokenType("UNFINISHED_FLAG");

    public static final BazelQueryTokenType UNQUOTED_VAL = new BazelQueryTokenType("UNQUOTED_VAL");

    public static final BazelQueryTokenType SQ_VAL = new BazelQueryTokenType("SQ_VAL");

    public static final BazelQueryTokenType DQ_VAL = new BazelQueryTokenType("DQ_VAL");

    public static final BazelQueryTokenType UNION = new BazelQueryTokenType("UNION");

    public static final BazelQueryTokenType EXCEPT = new BazelQueryTokenType("EXCEPT");

    public static final BazelQueryTokenType INTERSECT = new BazelQueryTokenType("INTERSECT");

    public static final BazelQueryTokenType LET = new BazelQueryTokenType("LET");

    public static final BazelQueryTokenType IN = new BazelQueryTokenType("IN");

    public static final BazelQueryTokenType EQUALS = new BazelQueryTokenType("EQUALS");

    public static final BazelQueryTokenType SET = new BazelQueryTokenType("SET");

    public static final BazelQueryTokenType INTEGER = new BazelQueryTokenType("INTEGER");

    public static final BazelQueryTokenType WHITE_SPACE = new BazelQueryTokenType("WHITE_SPACE");

    public static final BazelQueryTokenType UNEXPECTED = new BazelQueryTokenType("UNEXPECTED");

    public static final BazelQueryTokenType UNFINISHED_VAL = new BazelQueryTokenType("UNFINISHED_VAL");

    public static final BazelQueryTokenType SQ_UNFINISHED = new BazelQueryTokenType("SQ_UNFINISHED");

    public static final BazelQueryTokenType DQ_UNFINISHED = new BazelQueryTokenType("DQ_UNFINISHED");

    public static final BazelQueryTokenType SQ_EMPTY = new BazelQueryTokenType("SQ_EMPTY");

    public static final BazelQueryTokenType DQ_EMPTY = new BazelQueryTokenType("DQ_EMPTY");

    public static final BazelQueryTokenType MISSING_SPACE = new BazelQueryTokenType("MISSING_SPACE");
}
