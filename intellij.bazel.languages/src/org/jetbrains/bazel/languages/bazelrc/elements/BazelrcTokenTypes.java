package org.jetbrains.bazel.languages.bazelrc.elements;

public final class BazelrcTokenTypes {
    private BazelrcTokenTypes() {
    }

    public static final BazelrcTokenType IMPORT = new BazelrcTokenType("IMPORT");

    // Comment
    public static final BazelrcTokenType COMMENT = new BazelrcTokenType("COMMENT");

    public static final BazelrcTokenType DOUBLE_QUOTE = new BazelrcTokenType("\"");

    public static final BazelrcTokenType SINGLE_QUOTE = new BazelrcTokenType("'");

    public static final BazelrcTokenType COMMAND = new BazelrcTokenType("COMMAND");

    public static final BazelrcTokenType COLON = new BazelrcTokenType(":");

    public static final BazelrcTokenType CONFIG = new BazelrcTokenType("CONFIG");

    public static final BazelrcTokenType FLAG = new BazelrcTokenType("FLAG");

    public static final BazelrcTokenType EQ = new BazelrcTokenType("=");

    public static final BazelrcTokenType VALUE = new BazelrcTokenType("VALUE");
}
