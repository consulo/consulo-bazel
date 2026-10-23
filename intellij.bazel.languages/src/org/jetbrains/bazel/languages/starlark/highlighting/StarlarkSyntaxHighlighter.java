package org.jetbrains.bazel.languages.starlark.highlighting;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.lexer.Lexer;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.lexer.StarlarkHighlightingLexer;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class StarlarkSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final StarlarkSyntaxHighlighter INSTANCE = new StarlarkSyntaxHighlighter();

    private StarlarkSyntaxHighlighter() {
    }

    private static final Map<IElementType, TextAttributesKey> KEYS =
        Map.ofEntries(
            Map.entry(StarlarkTokenTypes.DEF_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.LAMBDA_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.IF_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.ELIF_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.ELSE_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.FOR_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.IN_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.RETURN_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.BREAK_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.CONTINUE_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.PASS_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.LOAD_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.FROM_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            // boolean operators
            Map.entry(StarlarkTokenTypes.OR_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.AND_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.NOT_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            // code organization
            Map.entry(StarlarkTokenTypes.SEMICOLON, StarlarkHighlightingColors.SEMICOLON),
            Map.entry(StarlarkTokenTypes.COMMA, StarlarkHighlightingColors.COMMA),
            Map.entry(StarlarkTokenTypes.DOT, StarlarkHighlightingColors.DOT),
            Map.entry(StarlarkTokenTypes.LPAR, StarlarkHighlightingColors.PARENTHESES),
            Map.entry(StarlarkTokenTypes.RPAR, StarlarkHighlightingColors.PARENTHESES),
            Map.entry(StarlarkTokenTypes.LBRACE, StarlarkHighlightingColors.BRACKETS),
            Map.entry(StarlarkTokenTypes.RBRACE, StarlarkHighlightingColors.BRACKETS),
            // literals
            Map.entry(StarlarkTokenTypes.INT, StarlarkHighlightingColors.NUMBER),
            Map.entry(StarlarkTokenTypes.FLOAT, StarlarkHighlightingColors.NUMBER),
            Map.entry(StarlarkTokenTypes.STRING, StarlarkHighlightingColors.STRING),
            Map.entry(StarlarkTokenTypes.BYTES, StarlarkHighlightingColors.STRING),
            Map.entry(StarlarkTokenTypes.NONE_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.TRUE_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            Map.entry(StarlarkTokenTypes.FALSE_KEYWORD, StarlarkHighlightingColors.KEYWORD),
            // random
            Map.entry(StarlarkTokenTypes.IDENTIFIER, StarlarkHighlightingColors.IDENTIFIER),
            Map.entry(StarlarkTokenTypes.COMMENT, StarlarkHighlightingColors.LINE_COMMENT)
        );

    @Override
    public Lexer getHighlightingLexer() {
        return new StarlarkHighlightingLexer();
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(KEYS.get(tokenType));
    }
}

@ExtensionImpl
class StarlarkSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    // consulo: extensions need a public constructor
    public StarlarkSyntaxHighlighterFactory() {
    }

    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        return StarlarkSyntaxHighlighter.INSTANCE;
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
