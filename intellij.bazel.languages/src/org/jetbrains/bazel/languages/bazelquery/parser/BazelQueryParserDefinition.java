package org.jetbrains.bazel.languages.bazelquery.parser;

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
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryElementTypes;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets;
import org.jetbrains.bazel.languages.bazelquery.lexer.BazelQueryLexer;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFile;

@ExtensionImpl
public class BazelQueryParserDefinition implements ParserDefinition {
    @Override
    public Language getLanguage() {
        return BazelQueryLanguage.INSTANCE;
    }

    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new BazelQueryLexer();
    }

    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        return (root, builder, version) -> new ParsingNoFlags(root, builder).parseFile();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public TokenSet getWhitespaceTokens(LanguageVersion languageVersion) {
        return BazelQueryTokenSets.WHITE_SPACES;
    }

    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return BazelQueryTokenSets.COMMENTS;
    }

    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return TokenSet.EMPTY;
    }

    @Override
    public PsiElement createElement(ASTNode node) {
        return BazelQueryElementTypes.createElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new BazelQueryFile(viewProvider);
    }

    private static final IFileElementType FILE = new IFileElementType(BazelQueryLanguage.INSTANCE);
}
