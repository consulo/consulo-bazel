package org.jetbrains.bazel.languages.bazelrc.parser;

import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;
import consulo.language.parser.PsiBuilder;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcElementTypes;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenSets;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes;
import org.jspecify.annotations.Nullable;

// consulo: Kotlin `PsiBuilder by builder` delegation is replaced by explicit calls on myBuilder
class Parsing {
    private final IElementType myRoot;
    private final PsiBuilder myBuilder;

    Parsing(IElementType root, PsiBuilder builder) {
        myRoot = root;
        myBuilder = builder;
    }

    public PsiBuilder getBuilder() {
        return myBuilder;
    }

    private static final TokenSet COMMAND_PREFIXES = TokenSet.orSet(TokenSet.create(BazelrcTokenTypes.COMMAND), BazelrcTokenSets.QUOTES);
    private static final TokenSet FLAG_PREFIXES = TokenSet.create(BazelrcTokenTypes.FLAG, BazelrcTokenTypes.EQ, BazelrcTokenTypes.VALUE);
    private static final TokenSet VALUE_TOKENS =
        TokenSet.create(BazelrcTokenTypes.VALUE, BazelrcTokenTypes.SINGLE_QUOTE, BazelrcTokenTypes.DOUBLE_QUOTE);
    private static final TokenSet BACK_TOKENS = TokenSet.create(TokenType.WHITE_SPACE, BazelrcTokenTypes.COMMENT);

    public ASTNode parseFile() {
        PsiBuilder.Marker file = myBuilder.mark();
        while (!myBuilder.eof()) {
            if (atToken(BazelrcTokenTypes.IMPORT)) {
                parseImport();
            }
            else if (atAnyToken(COMMAND_PREFIXES)) {
                parseLine();
            }
            else {
                advanceError(BazelLanguagesLocalize.bazelrcLanguageParserExpectedImportOrCommand());
            }
        }

        file.done(myRoot);
        return myBuilder.getTreeBuilt();
    }

    private void parseImport() {
        PsiBuilder.Marker importMarker = myBuilder.mark();

        expectToken(BazelrcTokenTypes.IMPORT, BazelLanguagesLocalize.bazelrcLanguageParserExpectedImport());

        if (!matchToken(BazelrcTokenTypes.VALUE)) {
            myBuilder.error(BazelLanguagesLocalize.bazelrcLanguageParserMissingImportPath());
        }

        while (atToken(BazelrcTokenTypes.VALUE)) {
            advanceError(BazelLanguagesLocalize.bazelrcLanguageParserUnexpectedImportPath());
        }

        importMarker.done(BazelrcElementTypes.IMPORT);
    }

    private void parseLine() {
        if (!atAnyToken(COMMAND_PREFIXES)) {
            advanceError(BazelLanguagesLocalize.bazelrcLanguageParserExpectedNewCommandLine());
        }

        PsiBuilder.Marker lineMarker = myBuilder.mark();
        parseCommand();

        while (atAnyToken(FLAG_PREFIXES)) {
            parseFlag();
        }

        lineMarker.done(BazelrcElementTypes.LINE);
    }

    private void parseCommand() {
        @Nullable IElementType quote = null;
        if (atAnyToken(BazelrcTokenSets.QUOTES)) {
            quote = myBuilder.getTokenType();
            myBuilder.advanceLexer();
        }

        expectToken(BazelrcTokenTypes.COMMAND, BazelLanguagesLocalize.bazelrcLanguageParserExpectedCommand());

        if (matchToken(BazelrcTokenTypes.COLON)) {
            matchToken(BazelrcTokenTypes.CONFIG);
        }

        // check to see if we crossed a nl
        if (quote != null && !pastNewLine()) {
            matchToken(quote);
        }
    }

    private void parseFlag() {
        PsiBuilder.Marker flag = myBuilder.mark();

        matchToken(BazelrcTokenTypes.FLAG);
        matchToken(BazelrcTokenTypes.EQ);
        matchToken(BazelrcTokenTypes.VALUE);
        while (atAnyToken(VALUE_TOKENS) && !pastNewLine()) {
            myBuilder.advanceLexer();
        }

        flag.done(BazelrcElementTypes.FLAG);
    }

    private boolean pastNewLine() {
        int tokenPos = -1;
        int endOffset = myBuilder.getCurrentOffset();

        @Nullable IElementType backToken = myBuilder.rawLookup(tokenPos);
        while (BACK_TOKENS.contains(backToken)) {
            int startOffset = myBuilder.rawTokenTypeStart(tokenPos);
            if (myBuilder.getOriginalText().subSequence(startOffset, endOffset).toString().contains("\n")) {
                return true;
            }
            endOffset = startOffset;
            backToken = myBuilder.rawLookup(--tokenPos);
        }

        return false;
    }

    private boolean atToken(@Nullable IElementType tokenType) {
        return myBuilder.getTokenType() == tokenType;
    }

    private boolean atAnyToken(TokenSet tokenTypes) {
        return tokenTypes.contains(myBuilder.getTokenType());
    }

    private boolean matchToken(IElementType tokenType) {
        if (atToken(tokenType)) {
            myBuilder.advanceLexer();
            return true;
        }
        return false;
    }

    private boolean expectToken(IElementType tokenType, LocalizeValue error) {
        if (atToken(tokenType)) {
            myBuilder.advanceLexer();
            return true;
        }
        else {
            advanceError(error);
            return false;
        }
    }

    private boolean matchAnyToken(TokenSet tokens) {
        if (atAnyToken(tokens)) {
            myBuilder.advanceLexer();
            return true;
        }
        return false;
    }

    private void advanceError(LocalizeValue message) {
        PsiBuilder.Marker err = myBuilder.mark();
        myBuilder.advanceLexer();
        err.error(message);
    }
}
