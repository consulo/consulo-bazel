package org.jetbrains.bazel.languages.projectview.highlighting;

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
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewLexer;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class ProjectViewSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final ProjectViewSyntaxHighlighter INSTANCE = new ProjectViewSyntaxHighlighter();

    private ProjectViewSyntaxHighlighter() {
    }

    private static final Map<IElementType, TextAttributesKey> KEYS =
        Map.of(
            ProjectViewTokenType.COLON, ProjectViewHighlightingColors.COLON,
            ProjectViewTokenType.COMMENT, ProjectViewHighlightingColors.COMMENT,
            ProjectViewTokenType.IDENTIFIER, ProjectViewHighlightingColors.IDENTIFIER,
            ProjectViewTokenType.SECTION_KEYWORD, ProjectViewHighlightingColors.KEYWORD,
            ProjectViewTokenType.IMPORT_KEYWORD, ProjectViewHighlightingColors.KEYWORD,
            ProjectViewTokenType.TRY_IMPORT_KEYWORD, ProjectViewHighlightingColors.KEYWORD
        );

    @Override
    public Lexer getHighlightingLexer() {
        return new ProjectViewLexer();
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(KEYS.get(tokenType));
    }
}

@ExtensionImpl
class ProjectViewSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    // consulo: extensions need a public constructor
    public ProjectViewSyntaxHighlighterFactory() {
    }

    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        return ProjectViewSyntaxHighlighter.INSTANCE;
    }

    @Override
    public Language getLanguage() {
        return ProjectViewLanguage.INSTANCE;
    }
}
