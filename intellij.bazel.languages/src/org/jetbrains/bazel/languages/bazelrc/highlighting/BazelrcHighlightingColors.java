package org.jetbrains.bazel.languages.bazelrc.highlighting;

import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;

import static consulo.colorScheme.TextAttributesKey.createTextAttributesKey;

public final class BazelrcHighlightingColors {
    private BazelrcHighlightingColors() {
    }

    public static final TextAttributesKey COMMAND = createTextAttributesKey("BAZELRC_COMMAND", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey IMPORT =
        createTextAttributesKey("BAZELRC_IMPORT", DefaultLanguageHighlighterColors.STATIC_METHOD);
    public static final TextAttributesKey STRING = createTextAttributesKey("BAZELRC_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER = createTextAttributesKey("BAZELRC_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey CONST = createTextAttributesKey("BAZELRC_CONST", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey COMMENT =
        createTextAttributesKey("BAZELRC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey IDENTIFIER =
        createTextAttributesKey("BAZELRC_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey UNKNOWN_FLAG =
        createTextAttributesKey("BAZELRC_UNKNOWN_FLAG", CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
    public static final TextAttributesKey UNDOCUMENTED_FLAG =
        createTextAttributesKey("BAZELRC_UNDOCUMENTED_FLAG", CodeInsightColors.WARNINGS_ATTRIBUTES);
    public static final TextAttributesKey DEPRECATED_FLAG =
        createTextAttributesKey("BAZELRC_DEPRECATED_FLAG", CodeInsightColors.DEPRECATED_ATTRIBUTES);
    public static final TextAttributesKey NOOP_FLAG =
        createTextAttributesKey("BAZELRC_NOOP_FLAG", CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES);
}
