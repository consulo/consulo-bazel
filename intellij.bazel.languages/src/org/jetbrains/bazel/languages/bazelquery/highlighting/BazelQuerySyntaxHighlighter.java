package org.jetbrains.bazel.languages.bazelquery.highlighting;

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
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes;
import org.jetbrains.bazel.languages.bazelquery.lexer.BazelQueryLexer;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BazelQuerySyntaxHighlighter extends SyntaxHighlighterBase {
    public static final BazelQuerySyntaxHighlighter INSTANCE = new BazelQuerySyntaxHighlighter();

    private static final Map<IElementType, TextAttributesKey> KEYS = new HashMap<>();

    static {
        for (IElementType type : BazelQueryTokenSets.WORDS.getTypes()) {
            KEYS.put(type, BazelQueryHighlightingColors.WORD);
        }
        for (IElementType type : BazelQueryTokenSets.PATTERNS.getTypes()) {
            KEYS.put(type, BazelQueryHighlightingColors.WORD);
        }
        KEYS.put(BazelQueryTokenTypes.INTEGER, BazelQueryHighlightingColors.INTEGER);
        for (IElementType type : BazelQueryTokenSets.OPERATIONS.getTypes()) {
            KEYS.put(type, BazelQueryHighlightingColors.OPERATION);
        }
        for (IElementType type : BazelQueryTokenSets.FLAGS.getTypes()) {
            KEYS.put(type, BazelQueryHighlightingColors.FLAG);
        }
        for (IElementType type : BazelQueryTokenSets.FLAG_VALS.getTypes()) {
            KEYS.put(type, BazelQueryHighlightingColors.FLAG_VAL);
        }
    }

    private BazelQuerySyntaxHighlighter() {
    }

    @Override
    public Lexer getHighlightingLexer() {
        return new BazelQueryLexer();
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(KEYS.get(tokenType));
    }
}

@ExtensionImpl
class BazelQuerySyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    // consulo: extensions need a public constructor
    public BazelQuerySyntaxHighlighterFactory() {
    }

    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        return BazelQuerySyntaxHighlighter.INSTANCE;
    }

    @Override
    public Language getLanguage() {
        return BazelQueryLanguage.INSTANCE;
    }
}

/**
 * Consulo: upstream registers {@link BazelQuerySyntaxHighlighterFactory} for both BazelQuery and BazelQueryFlags,
 * a Consulo extension is bound to a single language.
 */
@ExtensionImpl
class BazelQueryFlagsSyntaxHighlighterFactory extends BazelQuerySyntaxHighlighterFactory {
    // consulo: extensions need a public constructor
    public BazelQueryFlagsSyntaxHighlighterFactory() {
    }

    @Override
    public Language getLanguage() {
        return BazelQueryFlagsLanguage.INSTANCE;
    }
}
