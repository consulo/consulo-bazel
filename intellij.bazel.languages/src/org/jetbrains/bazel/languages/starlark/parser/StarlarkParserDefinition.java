package org.jetbrains.bazel.languages.starlark.parser;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.lexer.StarlarkIndentingLexer;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;

@ExtensionImpl
public class StarlarkParserDefinition implements ParserDefinition {
    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new StarlarkIndentingLexer();
    }

    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        return new StarlarkParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public TokenSet getWhitespaceTokens(LanguageVersion languageVersion) {
        return StarlarkTokenSets.WHITESPACE;
    }

    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return StarlarkTokenSets.COMMENT;
    }

    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return StarlarkTokenSets.STRINGS;
    }

    @Override
    public PsiElement createElement(ASTNode node) {
        return StarlarkElementTypes.createElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new StarlarkFile(viewProvider);
    }

    private static final IFileElementType FILE = new IFileElementType(StarlarkLanguage.INSTANCE);
}
