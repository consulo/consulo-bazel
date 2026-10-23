package org.jetbrains.bazel.languages.bazelversion.parser;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.ast.ASTFactory;
import consulo.language.lexer.EmptyLexer;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.plain.ast.PlainTextTokenTypes;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.version.LanguageVersion;
import org.jetbrains.bazel.languages.bazelversion.BazelVersionLanguage;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionFile;

/**
 * @see consulo.language.impl.plain.PlainTextParserDefinition
 */
@ExtensionImpl
public class BazelVersionParserDefinition implements ParserDefinition {
    // avoid creating new IFileElementType on each getFileNodeType
    // to ensure IFileElementType is registered only once inside an internal registry
    private final IFileElementType myFile =
        new IFileElementType(BazelVersionLanguage.INSTANCE) {
            @Override
            public ASTNode parseContents(ASTNode chameleon) {
                return ASTFactory.leaf(PlainTextTokenTypes.PLAIN_TEXT, chameleon.getChars());
            }
        };

    public IFileElementType getFile() {
        return myFile;
    }

    @Override
    public Language getLanguage() {
        return BazelVersionLanguage.INSTANCE;
    }

    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new EmptyLexer();
    }

    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return myFile;
    }

    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return TokenSet.EMPTY;
    }

    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return TokenSet.EMPTY;
    }

    @Override
    public PsiElement createElement(ASTNode node) {
        return PsiUtilCore.NULL_PSI_ELEMENT;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new BazelVersionFile(viewProvider);
    }
}
