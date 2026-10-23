package org.jetbrains.bazel.languages.starlark.parser;

import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

class Parsing {
    private final ParsingContext myContext;
    protected final PsiBuilder myBuilder;

    Parsing(ParsingContext context) {
        myContext = context;
        myBuilder = context.getBuilder();
    }

    public ParsingContext getContext() {
        return myContext;
    }

    protected boolean checkMatches(IElementType token, LocalizeValue message) {
        if (atToken(token)) {
            myBuilder.advanceLexer();
            return true;
        }
        myBuilder.error(message);
        return false;
    }

    protected boolean parseIdentifierOrSkip() {
        if (atToken(StarlarkTokenTypes.IDENTIFIER)) {
            myBuilder.advanceLexer();
            return true;
        }
        else {
            PsiBuilder.Marker nameExpected = myBuilder.mark();
            if (!atToken(StarlarkTokenTypes.STATEMENT_BREAK) && !atToken(StarlarkTokenTypes.LPAR)) {
                myBuilder.advanceLexer();
            }
            nameExpected.error(StarlarkLocalize.parserExpectedIdentifier());
            return false;
        }
    }

    protected void assertCurrentToken(StarlarkTokenType tokenType) {
        LOG.assertTrue(atToken(tokenType));
    }

    protected boolean atToken(@Nullable IElementType tokenType) {
        return myBuilder.getTokenType() == tokenType;
    }

    protected boolean atAnyOfTokens(List<@Nullable IElementType> tokenTypes) {
        IElementType currentTokenType = myBuilder.getTokenType();
        for (IElementType tokenType : tokenTypes) {
            if (currentTokenType == tokenType) {
                return true;
            }
        }
        return false;
    }

    protected boolean matchToken(IElementType tokenType) {
        if (atToken(tokenType)) {
            myBuilder.advanceLexer();
            return true;
        }
        return false;
    }

    protected void nextToken() {
        myBuilder.advanceLexer();
    }

    private static final Logger LOG = Logger.getInstance(Parsing.class);

    protected static void advanceIdentifierLike(PsiBuilder builder) {
        if (isReservedKeyword(builder)) {
            String text = builder.getTokenText();
            String tokenText = text != null ? text : "";
            advanceError(builder, StarlarkLocalize.parserReservedKeywordCannotBeUsedAsIdentifier(tokenText));
        }
        else {
            builder.advanceLexer();
        }
    }

    protected static void advanceError(PsiBuilder builder, LocalizeValue message) {
        PsiBuilder.Marker err = builder.mark();
        builder.advanceLexer();
        err.error(message);
    }

    protected static boolean isIdentifierLike(PsiBuilder builder) {
        return builder.getTokenType() == StarlarkTokenTypes.IDENTIFIER || isReservedKeyword(builder);
    }

    private static boolean isReservedKeyword(PsiBuilder builder) {
        return StarlarkTokenSets.RESERVED_KEYWORDS.contains(builder.getTokenType());
    }

    protected static void buildTokenElement(@Nullable IElementType type, PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        advanceIdentifierLike(builder);
        marker.done(Objects.requireNonNull(type));
    }
}
