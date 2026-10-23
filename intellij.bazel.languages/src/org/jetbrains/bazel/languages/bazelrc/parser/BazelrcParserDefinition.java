package org.jetbrains.bazel.languages.bazelrc.parser;

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
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcElementTypes;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenSets;
import org.jetbrains.bazel.languages.bazelrc.lexer.BazelrcLexer;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFile;

@ExtensionImpl
public class BazelrcParserDefinition implements ParserDefinition {
    @Override
    public Language getLanguage() {
        return BazelrcLanguage.INSTANCE;
    }

    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new BazelrcLexer();
    }

    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        return (root, builder, version) -> new Parsing(root, builder).parseFile();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public TokenSet getWhitespaceTokens(LanguageVersion languageVersion) {
        return BazelrcTokenSets.WHITE_SPACES;
    }

    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return BazelrcTokenSets.COMMENTS;
    }

    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return TokenSet.EMPTY;
    }

    @Override
    public PsiElement createElement(ASTNode node) {
        return BazelrcElementTypes.createElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new BazelrcFile(viewProvider);
    }

    private static final IFileElementType FILE = new IFileElementType(BazelrcLanguage.INSTANCE);
}
