package org.jetbrains.bazel.languages.bazelrc.highlighting;

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
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes;
import org.jetbrains.bazel.languages.bazelrc.lexer.BazelrcLexer;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class BazelrcSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final BazelrcSyntaxHighlighter INSTANCE = new BazelrcSyntaxHighlighter();

    private static final Map<IElementType, TextAttributesKey> KEYS =
        Map.of(
            BazelrcTokenTypes.COMMENT, BazelrcHighlightingColors.COMMENT,
            BazelrcTokenTypes.IMPORT, BazelrcHighlightingColors.IMPORT,
            BazelrcTokenTypes.COMMAND, BazelrcHighlightingColors.COMMAND,
            BazelrcTokenTypes.CONFIG, BazelrcHighlightingColors.IDENTIFIER,
            BazelrcTokenTypes.FLAG, BazelrcHighlightingColors.STRING
        );

    private BazelrcSyntaxHighlighter() {
    }

    @Override
    public Lexer getHighlightingLexer() {
        return new BazelrcLexer();
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(KEYS.get(tokenType));
    }
}

@ExtensionImpl
class BazelrcSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    // consulo: extensions need a public constructor
    public BazelrcSyntaxHighlighterFactory() {
    }

    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        return BazelrcSyntaxHighlighter.INSTANCE;
    }

    @Override
    public Language getLanguage() {
        return BazelrcLanguage.INSTANCE;
    }
}
