package org.jetbrains.bazel.languages.projectview.parser;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementType;
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementTypes;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewLexer;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;

@ExtensionImpl
public class ProjectViewParserDefinition implements ParserDefinition {
    @Override
    public Language getLanguage() {
        return ProjectViewLanguage.INSTANCE;
    }

    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new ProjectViewLexer();
    }

    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        return (root, builder, version) -> {
            PsiBuilder.Marker rootMarker = builder.mark();
            new ProjectViewParser(builder).parseFile();
            rootMarker.done(root);
            return builder.getTreeBuilt();
        };
    }

    @Override
    public IFileElementType getFileNodeType() {
        return ProjectViewElementTypes.FILE;
    }

    @Override
    public TokenSet getWhitespaceTokens(LanguageVersion languageVersion) {
        return TokenSet.create(ProjectViewTokenType.WHITESPACE);
    }

    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return TokenSet.create(ProjectViewTokenType.COMMENT);
    }

    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return TokenSet.EMPTY;
    }

    @Override
    public PsiElement createElement(ASTNode node) {
        IElementType type = node.getElementType();
        if (type instanceof ProjectViewElementType projectViewElementType) {
            return projectViewElementType.createElement(node);
        }
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new ProjectViewPsiFile(viewProvider);
    }
}
