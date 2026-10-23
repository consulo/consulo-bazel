package org.jetbrains.bazel.languages.bazelquery.parser;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.parser.PsiBuilder;
import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

class ParsingUtils {
    private final PsiBuilder myBuilder;

    ParsingUtils(PsiBuilder builder) {
        myBuilder = builder;
    }

    public boolean atToken(@Nullable IElementType tokenType) {
        return myBuilder.getTokenType() == tokenType;
    }

    public boolean atAnyToken(TokenSet tokenTypes) {
        return tokenTypes.contains(myBuilder.getTokenType());
    }

    public boolean matchToken(IElementType tokenType) {
        if (atToken(tokenType)) {
            myBuilder.advanceLexer();
            return true;
        }
        return false;
    }

    public boolean expectToken(IElementType tokenType) {
        if (atToken(tokenType)) {
            myBuilder.advanceLexer();
            return true;
        }
        advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingToken(tokenType));
        return false;
    }

    public boolean matchAnyToken(TokenSet tokens) {
        if (atAnyToken(tokens)) {
            myBuilder.advanceLexer();
            return true;
        }
        return false;
    }

    public void advanceError(LocalizeValue message) {
        PsiBuilder.Marker err = myBuilder.mark();
        myBuilder.advanceLexer();
        err.error(message);
    }
}
