package org.jetbrains.bazel.languages.starlark.highlighting;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;

import static consulo.colorScheme.TextAttributesKey.createTextAttributesKey;

public final class StarlarkHighlightingColors {
    private StarlarkHighlightingColors() {
    }

    public static final TextAttributesKey KEYWORD = createTextAttributesKey("STARLARK_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey STRING = createTextAttributesKey("STARLARK_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER = createTextAttributesKey("STARLARK_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey LINE_COMMENT =
        createTextAttributesKey("STARLARK_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey SEMICOLON =
        createTextAttributesKey("STARLARK_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey COMMA = createTextAttributesKey("STARLARK_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey DOT = createTextAttributesKey("STARLARK_DOT", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey PARENTHESES =
        createTextAttributesKey("STARLARK_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey BRACKETS =
        createTextAttributesKey("STARLARK_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey IDENTIFIER =
        createTextAttributesKey("STARLARK_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey FUNCTION_DECLARATION =
        createTextAttributesKey("STARLARK_FUNCTION_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
    public static final TextAttributesKey NAMED_ARGUMENT = createTextAttributesKey("STARLARK_NAMED_ARGUMENT");
}
