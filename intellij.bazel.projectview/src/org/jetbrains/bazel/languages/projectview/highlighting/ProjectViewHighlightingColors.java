package org.jetbrains.bazel.languages.projectview.highlighting;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;

import static consulo.colorScheme.TextAttributesKey.createTextAttributesKey;

public final class ProjectViewHighlightingColors {
    private ProjectViewHighlightingColors() {
    }

    public static final TextAttributesKey COLON = createTextAttributesKey("PROJECTVIEW_COLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey IDENTIFIER =
        createTextAttributesKey("PROJECTVIEW_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey KEYWORD =
        createTextAttributesKey("PROJECTVIEW_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey COMMENT =
        createTextAttributesKey("PROJECTVIEW_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
}
