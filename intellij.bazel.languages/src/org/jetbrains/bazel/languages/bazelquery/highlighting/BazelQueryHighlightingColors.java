package org.jetbrains.bazel.languages.bazelquery.highlighting;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;

import static consulo.colorScheme.TextAttributesKey.createTextAttributesKey;

public final class BazelQueryHighlightingColors {
    private BazelQueryHighlightingColors() {
    }

    public static final TextAttributesKey COMMAND =
        createTextAttributesKey("BAZELQUERY_COMMAND", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey WORD = createTextAttributesKey("BAZELQUERY_WORD", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey PROMPT = createTextAttributesKey("BAZELQUERY_PROMPT", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey COMMENT =
        createTextAttributesKey("BAZELQUERY_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey OPERATION =
        createTextAttributesKey("BAZELQUERY_OPERATION", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey FLAG = createTextAttributesKey("BAZELQUERY_FLAG", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey FLAG_VAL =
        createTextAttributesKey("BAZELQUERY_FLAG_VAL", DefaultLanguageHighlighterColors.CONSTANT);

    public static final TextAttributesKey INTEGER = createTextAttributesKey("BAZELQUERY_INTEGER", DefaultLanguageHighlighterColors.NUMBER);
}
