package org.jetbrains.bazel.languages.starlark.parser;

import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.parser.PsiBuilder.Marker;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

class ExpressionParsing extends Parsing {
    ExpressionParsing(ParsingContext context) {
        super(context);
    }

    private boolean parsePrimaryExpression(boolean isTarget) {
        if (StarlarkTokenSets.PRIMARY_EXPRESSION_STARTERS.contains(myBuilder.getTokenType())) {
            IElementType tokenType = myBuilder.getTokenType();
            if (tokenType == StarlarkTokenTypes.IDENTIFIER) {
                buildTokenElement(getTargetOrReferenceExpression(isTarget), myBuilder);
            }
            else if (tokenType == StarlarkTokenTypes.INT) {
                buildTokenElement(StarlarkElementTypes.INTEGER_LITERAL_EXPRESSION, myBuilder);
            }
            else if (tokenType == StarlarkTokenTypes.FLOAT) {
                buildTokenElement(StarlarkElementTypes.FLOAT_LITERAL_EXPRESSION, myBuilder);
            }
            else if (tokenType == StarlarkTokenTypes.NONE_KEYWORD) {
                buildTokenElement(StarlarkElementTypes.NONE_LITERAL_EXPRESSION, myBuilder);
            }
            else if (tokenType == StarlarkTokenTypes.TRUE_KEYWORD) {
                buildTokenElement(StarlarkElementTypes.TRUE_LITERAL_EXPRESSION, myBuilder);
            }
            else if (tokenType == StarlarkTokenTypes.FALSE_KEYWORD) {
                buildTokenElement(StarlarkElementTypes.FALSE_LITERAL_EXPRESSION, myBuilder);
            }
            else if (tokenType == StarlarkTokenTypes.LPAR) {
                parseParenthesizedExpression(isTarget);
            }
            else if (tokenType == StarlarkTokenTypes.LBRACKET) {
                parseListLiteralExpression(isTarget);
            }
            else if (tokenType == StarlarkTokenTypes.LBRACE) {
                parseDictDisplay();
            }
            else {
                buildTokenElement(StarlarkElementTypes.STRING_LITERAL_EXPRESSION, myBuilder);
            }
            return true;
        }
        else {
            return false;
        }
    }

    private void parseListLiteralExpression(boolean isTarget) {
        assertCurrentToken(StarlarkTokenTypes.LBRACKET);
        Marker expr = myBuilder.mark();
        myBuilder.advanceLexer();
        if (atToken(StarlarkTokenTypes.RBRACKET)) {
            myBuilder.advanceLexer();
            expr.done(StarlarkElementTypes.LIST_LITERAL_EXPRESSION);
            return;
        }
        if (!parseSingleExpression(isTarget)) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
        }
        if (atToken(StarlarkTokenTypes.FOR_KEYWORD)) {
            parseComprehension(expr, StarlarkTokenTypes.RBRACKET, StarlarkElementTypes.LIST_COMP_EXPRESSION);
        }
        else {
            while (!atToken(StarlarkTokenTypes.RBRACKET)) {
                if (!matchToken(StarlarkTokenTypes.COMMA)) {
                    myBuilder.error(StarlarkLocalize.parserRbracketOrCommaExpected());
                }
                if (atToken(StarlarkTokenTypes.RBRACKET)) {
                    break;
                }
                if (!parseSingleExpression(isTarget)) {
                    myBuilder.error(StarlarkLocalize.parserExpectedExprOrCommaOrBracket());
                    break;
                }
            }
            checkMatches(StarlarkTokenTypes.RBRACKET, StarlarkLocalize.parserExpectedRbracket());
            expr.done(StarlarkElementTypes.LIST_LITERAL_EXPRESSION);
        }
    }

    private void parseComprehension(
        Marker expr,
        @Nullable IElementType endToken,
        IElementType exprType
    ) {
        assertCurrentToken(StarlarkTokenTypes.FOR_KEYWORD);
        while (true) {
            myBuilder.advanceLexer();
            parseStarTargets();
            parseComprehensionRange(exprType == StarlarkElementTypes.GENERATOR_EXPRESSION);
            while (atToken(StarlarkTokenTypes.IF_KEYWORD)) {
                myBuilder.advanceLexer();
                if (!parseOldTestExpression(false)) {
                    myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                }
            }
            if (atToken(StarlarkTokenTypes.FOR_KEYWORD)) {
                continue;
            }
            if (endToken == null || matchToken(endToken)) {
                break;
            }
            myBuilder.error(StarlarkLocalize.parserExpectedForOrBracket());
            break;
        }
        expr.done(exprType);
    }

    public void parseStarTargets() {
        Marker expr = myBuilder.mark();
        if (!parseStarExpression(true)) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            expr.drop();
            return;
        }
        if (atToken(StarlarkTokenTypes.COMMA)) {
            while (atToken(StarlarkTokenTypes.COMMA)) {
                myBuilder.advanceLexer();
                Marker expr2 = myBuilder.mark();
                if (!parseStarExpression(true)) {
                    myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                    expr2.rollbackTo();
                    break;
                }
                expr2.drop();
            }
            expr.done(StarlarkElementTypes.TUPLE_EXPRESSION);
        }
        else {
            expr.drop();
        }
    }

    private void parseComprehensionRange(boolean generatorExpression) {
        checkMatches(StarlarkTokenTypes.IN_KEYWORD, StarlarkLocalize.parserExpectedIn());
        boolean result;
        if (generatorExpression) {
            result = parseORTestExpression(false);
        }
        else {
            result = parseTupleExpression(false, true);
        }
        if (!result) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
        }
    }

    private void parseDictDisplay() {
        assertCurrentToken(StarlarkTokenTypes.LBRACE);
        Marker expr = myBuilder.mark();
        myBuilder.advanceLexer();
        if (matchToken(StarlarkTokenTypes.RBRACE)) {
            expr.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION);
            return;
        }
        if (atToken(StarlarkTokenTypes.EXP)) {
            if (!parseDoubleStarExpression()) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                expr.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION);
                return;
            }
            parseDictLiteralContentTail(expr);
            return;
        }
        parseDictTail(expr, false);
    }

    private void parseDictTail(Marker expr, boolean isDict) {
        Marker firstExprMarker = myBuilder.mark();
        if (isDict && !parseSingleExpression(false) || !isDict && !parseSingleExpression(false)) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            firstExprMarker.drop();
            expr.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION);
            return;
        }
        if (matchToken(StarlarkTokenTypes.COLON)) {
            if (!isDict) {
                firstExprMarker.rollbackTo();
                parseDictTail(expr, true);
                return;
            }
            parseDictLiteralTail(expr, firstExprMarker);
        }
        else {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            firstExprMarker.drop();
            expr.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION);
        }
    }

    private void parseDictLiteralTail(Marker startMarker, Marker firstKeyValueMarker) {
        if (!parseSingleExpression(false)) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            firstKeyValueMarker.done(StarlarkElementTypes.KEY_VALUE_EXPRESSION);
            if (atToken(StarlarkTokenTypes.RBRACE)) {
                myBuilder.advanceLexer();
            }
            startMarker.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION);
            return;
        }
        firstKeyValueMarker.done(StarlarkElementTypes.KEY_VALUE_EXPRESSION);
        if (atToken(StarlarkTokenTypes.FOR_KEYWORD)) {
            parseComprehension(startMarker, StarlarkTokenTypes.RBRACE, StarlarkElementTypes.DICT_COMP_EXPRESSION);
        }
        else {
            parseDictLiteralContentTail(startMarker);
        }
    }

    private void parseDictLiteralContentTail(Marker startMarker) {
        while (!atToken(StarlarkTokenTypes.RBRACE)) {
            checkMatches(StarlarkTokenTypes.COMMA, StarlarkLocalize.parserExpectedComma());
            if (atToken(StarlarkTokenTypes.EXP)) {
                if (!parseDoubleStarExpression()) {
                    break;
                }
            }
            else {
                if (!parseKeyValueExpression()) {
                    break;
                }
            }
        }
        checkMatches(StarlarkTokenTypes.RBRACE, StarlarkLocalize.parserExpectedRbrace());
        startMarker.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION);
    }

    private boolean parseKeyValueExpression() {
        Marker marker = myBuilder.mark();
        if (!parseSingleExpression(false)) {
            marker.drop();
            return false;
        }
        checkMatches(StarlarkTokenTypes.COLON, StarlarkLocalize.parserExpectedColon());
        if (!parseSingleExpression(false)) {
            myBuilder.error(StarlarkLocalize.parserValueExpressionExpected());
            marker.drop();
            return false;
        }
        marker.done(StarlarkElementTypes.KEY_VALUE_EXPRESSION);
        return true;
    }

    private void parseParenthesizedExpression(boolean isTarget) {
        assertCurrentToken(StarlarkTokenTypes.LPAR);
        Marker expr = myBuilder.mark();
        myBuilder.advanceLexer();
        if (atToken(StarlarkTokenTypes.RPAR)) {
            myBuilder.advanceLexer();
            expr.done(StarlarkElementTypes.TUPLE_EXPRESSION);
        }
        else {
            parseTupleExpression(isTarget, false);
            if (atToken(StarlarkTokenTypes.FOR_KEYWORD)) {
                parseComprehension(expr, StarlarkTokenTypes.RPAR, StarlarkElementTypes.GENERATOR_EXPRESSION);
            }
            else {
                Marker err = myBuilder.mark();
                boolean empty = true;
                while (!myBuilder.eof() &&
                    !atAnyOfTokens(
                        List.of(StarlarkTokenTypes.RPAR, StarlarkTokenTypes.LINE_BREAK, StarlarkTokenTypes.STATEMENT_BREAK)
                    )
                ) {
                    myBuilder.advanceLexer();
                    empty = false;
                }
                if (!empty) {
                    err.error(StarlarkLocalize.parserUnexpectedExpressionSyntax());
                }
                else {
                    err.drop();
                }
                checkMatches(StarlarkTokenTypes.RPAR, StarlarkLocalize.parserExpectedRpar());
                expr.done(StarlarkElementTypes.PARENTHESIZED_EXPRESSION);
            }
        }
    }

    private boolean parseMemberExpression(boolean isTarget) {
        // in sequence a.b.... .c all members but last are always references, and the last may be target.
        boolean recastFirstIdentifier = false;
        boolean recastQualifier = false;
        do {
            boolean firstIdentifierIsTarget = isTarget && !recastFirstIdentifier;
            Marker expr = myBuilder.mark();
            if (!parsePrimaryExpression(firstIdentifierIsTarget)) {
                expr.drop();
                return false;
            }
            while (true) {
                IElementType tokenType = myBuilder.getTokenType();
                if (tokenType == StarlarkTokenTypes.DOT) {
                    if (firstIdentifierIsTarget) {
                        recastFirstIdentifier = true;
                        expr.rollbackTo();
                        break;
                    }
                    myBuilder.advanceLexer();
                    checkMatches(StarlarkTokenTypes.IDENTIFIER, StarlarkLocalize.parserExpectedName());
                    if (isTarget &&
                        !recastQualifier &&
                        !atAnyOfTokens(
                            List.of(StarlarkTokenTypes.DOT, StarlarkTokenTypes.LPAR, StarlarkTokenTypes.LBRACKET)
                        )
                    ) {
                        expr.done(StarlarkElementTypes.TARGET_EXPRESSION);
                    }
                    else {
                        expr.done(StarlarkElementTypes.REFERENCE_EXPRESSION);
                    }
                    expr = expr.precede();
                }
                else if (tokenType == StarlarkTokenTypes.LPAR) {
                    parseArgumentList();
                    expr.done(StarlarkElementTypes.CALL_EXPRESSION);
                    expr = expr.precede();
                }
                else if (tokenType == StarlarkTokenTypes.LBRACKET) {
                    myBuilder.advanceLexer();
                    parseSliceOrSubscriptionExpression(expr, false);
                    if (isTarget && !recastQualifier) {
                        recastFirstIdentifier = true; // subscription is always a reference
                        recastQualifier = true; // recast non-first qualifiers too
                        expr.rollbackTo();
                        break;
                    }
                    expr = expr.precede();
                }
                else {
                    expr.drop();
                    break;
                }
                recastFirstIdentifier = false; // it is true only after a break; normal flow always unsets it.
                // recastQualifier is untouched, it remembers whether qualifiers were already recast
            }
        }
        while (recastFirstIdentifier);
        return true;
    }

    private void parseSliceOrSubscriptionExpression(Marker expr, boolean isSlice) {
        Marker sliceOrTupleStart = myBuilder.mark();
        Marker sliceItemStart = myBuilder.mark();
        if (atToken(StarlarkTokenTypes.COLON)) {
            sliceOrTupleStart.drop();
            Marker sliceMarker = myBuilder.mark();
            sliceMarker.done(StarlarkElementTypes.EMPTY_EXPRESSION);
            parseSliceEnd(expr, sliceItemStart);
        }
        else {
            boolean hadExpression = parseSingleExpression(false);
            if (atToken(StarlarkTokenTypes.COLON)) {
                if (!isSlice) {
                    sliceOrTupleStart.rollbackTo();
                    parseSliceOrSubscriptionExpression(expr, true);
                    return;
                }
                sliceOrTupleStart.drop();
                parseSliceEnd(expr, sliceItemStart);
            }
            else if (atToken(StarlarkTokenTypes.COMMA)) {
                sliceItemStart.done(StarlarkElementTypes.SLICE_ITEM);
                if (!parseSliceListTail(expr, sliceOrTupleStart)) {
                    sliceOrTupleStart.rollbackTo();
                    if (!parseTupleExpression(false, false)) {
                        myBuilder.error(StarlarkLocalize.parserTupleExpressionExpected());
                    }
                    checkMatches(StarlarkTokenTypes.RBRACKET, StarlarkLocalize.parserExpectedRbracket());
                    expr.done(StarlarkElementTypes.SUBSCRIPTION_EXPRESSION);
                }
            }
            else {
                if (!hadExpression) {
                    myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                }
                sliceOrTupleStart.drop();
                sliceItemStart.drop();
                checkMatches(StarlarkTokenTypes.RBRACKET, StarlarkLocalize.parserExpectedRbracket());
                expr.done(StarlarkElementTypes.SUBSCRIPTION_EXPRESSION);
            }
        }
    }

    private void parseSliceEnd(Marker exprStart, Marker sliceItemStart) {
        myBuilder.advanceLexer();
        if (atToken(StarlarkTokenTypes.RBRACKET)) {
            Marker sliceMarker = myBuilder.mark();
            sliceMarker.done(StarlarkElementTypes.EMPTY_EXPRESSION);
            sliceItemStart.done(StarlarkElementTypes.SLICE_ITEM);
            nextToken();
            exprStart.done(StarlarkElementTypes.SLICE_EXPRESSION);
            return;
        }
        else {
            if (atToken(StarlarkTokenTypes.COLON)) {
                Marker sliceMarker = myBuilder.mark();
                sliceMarker.done(StarlarkElementTypes.EMPTY_EXPRESSION);
            }
            else {
                parseSingleExpression(false);
            }
            if (!BRACKET_COLON_COMMA.contains(myBuilder.getTokenType())) {
                myBuilder.error(StarlarkLocalize.parserExpectedColonOrRbracket());
            }
            if (matchToken(StarlarkTokenTypes.COLON)) {
                parseSingleExpression(false);
            }
            sliceItemStart.done(StarlarkElementTypes.SLICE_ITEM);
            if (!BRACKET_OR_COMMA.contains(myBuilder.getTokenType())) {
                myBuilder.error(StarlarkLocalize.parserRbracketOrCommaExpected());
            }
        }
        parseSliceListTail(exprStart, null);
    }

    private boolean parseSliceListTail(Marker exprStart, @Nullable Marker sliceOrTupleStart) {
        boolean inSlice = sliceOrTupleStart == null;
        while (atToken(StarlarkTokenTypes.COMMA)) {
            nextToken();
            Marker sliceItemStart = myBuilder.mark();
            parseTestExpression(false);
            if (matchToken(StarlarkTokenTypes.COLON)) {
                inSlice = true;
                parseTestExpression(false);
                if (matchToken(StarlarkTokenTypes.COLON)) {
                    parseTestExpression(false);
                }
            }
            sliceItemStart.done(StarlarkElementTypes.SLICE_ITEM);
            if (!BRACKET_OR_COMMA.contains(myBuilder.getTokenType())) {
                myBuilder.error(StarlarkLocalize.parserRbracketOrCommaExpected());
                break;
            }
        }
        checkMatches(StarlarkTokenTypes.RBRACKET, StarlarkLocalize.parserExpectedRbracket());
        if (inSlice) {
            if (sliceOrTupleStart != null) {
                sliceOrTupleStart.drop();
            }
            exprStart.done(StarlarkElementTypes.SLICE_EXPRESSION);
        }
        return inSlice;
    }

    private void parseArgumentList() {
        assertCurrentToken(StarlarkTokenTypes.LPAR);
        Marker argumentList = myBuilder.mark();
        myBuilder.advanceLexer();
        int argumentCount = 0;
        while (!atToken(StarlarkTokenTypes.RPAR)) {
            argumentCount++;
            if (argumentCount > 1) {
                if (matchToken(StarlarkTokenTypes.COMMA)) {
                    if (atToken(StarlarkTokenTypes.RPAR)) {
                        break;
                    }
                }
                else {
                    myBuilder.error(StarlarkLocalize.parserExpectedCommaOrRpar());
                    break;
                }
            }
            if (atToken(StarlarkTokenTypes.MULT) || atToken(StarlarkTokenTypes.EXP)) {
                Marker starArgumentMarker = myBuilder.mark();
                myBuilder.advanceLexer();
                if (!parseSingleExpression(false)) {
                    myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                }
                starArgumentMarker.done(StarlarkElementTypes.STAR_ARGUMENT_EXPRESSION);
            }
            else {
                if (isIdentifierLike(myBuilder)) {
                    Marker namedArgumentMarker = myBuilder.mark();
                    advanceIdentifierLike(myBuilder);
                    if (atToken(StarlarkTokenTypes.EQ)) {
                        myBuilder.advanceLexer();
                        if (!parseSingleExpression(false)) {
                            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                        }
                        namedArgumentMarker.done(StarlarkElementTypes.NAMED_ARGUMENT_EXPRESSION);
                        continue;
                    }
                    namedArgumentMarker.rollbackTo();
                }
                Marker argumentMarker = myBuilder.mark();
                if (!parseSingleExpression(false)) {
                    myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                    argumentMarker.rollbackTo();
                    break;
                }
                argumentMarker.done(StarlarkElementTypes.ARGUMENT_EXPRESSION);
            }
        }
        checkMatches(StarlarkTokenTypes.RPAR, StarlarkLocalize.parserExpectedRpar());
        argumentList.done(StarlarkElementTypes.ARGUMENT_LIST);
    }

    public boolean parseExpressionOptional() {
        return parseTupleExpression(false, false);
    }

    public void parseExpression() {
        if (!parseExpressionOptional()) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
        }
    }

    public void parseExpression(boolean isTarget) {
        if (!parseTupleExpression(isTarget, false)) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
        }
    }

    private boolean parseTupleExpression(boolean isTarget, boolean oldTest) {
        Marker expr = myBuilder.mark();
        boolean exprParseResult = oldTest ? parseOldTestExpression(true) : parseTestExpression(isTarget);
        if (!exprParseResult) {
            expr.drop();
            return false;
        }
        if (atToken(StarlarkTokenTypes.COMMA)) {
            while (atToken(StarlarkTokenTypes.COMMA)) {
                myBuilder.advanceLexer();
                Marker expr2 = myBuilder.mark();
                exprParseResult = oldTest ? parseOldTestExpression(true) : parseTestExpression(isTarget);
                if (!exprParseResult) {
                    expr2.rollbackTo();
                    break;
                }
                expr2.drop();
            }
            expr.done(StarlarkElementTypes.TUPLE_EXPRESSION);
        }
        else {
            expr.drop();
        }
        return true;
    }

    public boolean parseSingleExpression(boolean isTarget) {
        return parseTestExpression(isTarget);
    }

    public boolean parseTestExpression(boolean isTarget) {
        if (atToken(StarlarkTokenTypes.LAMBDA_KEYWORD)) {
            return parseLambdaExpression(false);
        }
        Marker condExpr = myBuilder.mark();
        if (!parseORTestExpression(isTarget)) {
            condExpr.drop();
            return false;
        }
        if (atToken(StarlarkTokenTypes.IF_KEYWORD)) {
            Marker conditionMarker = myBuilder.mark();
            myBuilder.advanceLexer();
            if (!parseORTestExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            else {
                if (!atToken(StarlarkTokenTypes.ELSE_KEYWORD)) {
                    if (atToken(StarlarkTokenTypes.COLON)) { // it's regular if statement. Bracket wasn't closed or new line was lost
                        conditionMarker.rollbackTo();
                        condExpr.drop();
                        return true;
                    }
                    else {
                        myBuilder.error(StarlarkLocalize.parserExpectedElse());
                    }
                }
                else {
                    myBuilder.advanceLexer();
                    if (!parseTestExpression(isTarget)) {
                        myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                    }
                }
            }
            conditionMarker.drop();
            condExpr.done(StarlarkElementTypes.CONDITIONAL_EXPRESSION);
        }
        else {
            condExpr.drop();
        }
        return true;
    }

    private boolean parseOldTestExpression(boolean oldTest) {
        if (atToken(StarlarkTokenTypes.LAMBDA_KEYWORD)) {
            return parseLambdaExpression(oldTest);
        }
        else {
            return parseORTestExpression(false);
        }
    }

    private boolean parseLambdaExpression(boolean oldTest) {
        Marker expr = myBuilder.mark();
        myBuilder.advanceLexer();
        getContext().getFunctionParser().parseParameterListContents(StarlarkTokenTypes.COLON, false);
        boolean parseExpressionResult =
            oldTest ? parseOldTestExpression(true) : parseSingleExpression(false);
        if (!parseExpressionResult) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
        }
        expr.done(StarlarkElementTypes.LAMBDA_EXPRESSION);
        return true;
    }

    private boolean parseORTestExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseANDTestExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (atToken(StarlarkTokenTypes.OR_KEYWORD)) {
            myBuilder.advanceLexer();
            if (!parseANDTestExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseANDTestExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseComparisonExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (atToken(StarlarkTokenTypes.AND_KEYWORD)) {
            myBuilder.advanceLexer();
            if (!parseComparisonExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseComparisonExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseStarExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (StarlarkTokenSets.COMPARISON_OPERATIONS.contains(myBuilder.getTokenType())) {
            if (atToken(StarlarkTokenTypes.NOT_KEYWORD)) {
                Marker notMarker = myBuilder.mark();
                myBuilder.advanceLexer();
                if (!atToken(StarlarkTokenTypes.IN_KEYWORD)) {
                    notMarker.rollbackTo();
                    break;
                }
                notMarker.drop();
                myBuilder.advanceLexer();
            }
            else if (atToken(StarlarkTokenTypes.IS_KEYWORD)) {
                myBuilder.advanceLexer();
                if (atToken(StarlarkTokenTypes.NOT_KEYWORD)) {
                    myBuilder.advanceLexer();
                }
            }
            else {
                myBuilder.advanceLexer();
            }
            if (!parseBitwiseORExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseStarExpression(boolean isTarget) {
        if (atToken(StarlarkTokenTypes.MULT)) {
            Marker starExpr = myBuilder.mark();
            nextToken();
            if (!parseBitwiseORExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                starExpr.drop();
                return false;
            }
            starExpr.done(StarlarkElementTypes.STAR_EXPRESSION);
            return true;
        }
        return parseBitwiseORExpression(isTarget);
    }

    private boolean parseDoubleStarExpression() {
        if (atToken(StarlarkTokenTypes.EXP)) {
            Marker starExpr = myBuilder.mark();
            nextToken();
            if (!parseBitwiseORExpression(false)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                starExpr.drop();
                return false;
            }
            starExpr.done(StarlarkElementTypes.DOUBLE_STAR_EXPRESSION);
            return true;
        }
        return parseBitwiseORExpression(false);
    }

    private boolean parseBitwiseORExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseBitwiseXORExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (atToken(StarlarkTokenTypes.OR)) {
            myBuilder.advanceLexer();
            if (!parseBitwiseXORExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseBitwiseXORExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseBitwiseANDExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (atToken(StarlarkTokenTypes.XOR)) {
            myBuilder.advanceLexer();
            if (!parseBitwiseANDExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseBitwiseANDExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseShiftExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (atToken(StarlarkTokenTypes.AND)) {
            myBuilder.advanceLexer();
            if (!parseShiftExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseShiftExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseAdditiveExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (StarlarkTokenSets.SHIFT_OPERATIONS.contains(myBuilder.getTokenType())) {
            myBuilder.advanceLexer();
            if (!parseAdditiveExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseAdditiveExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseMultiplicativeExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (StarlarkTokenSets.ADDITIVE_OPERATIONS.contains(myBuilder.getTokenType())) {
            myBuilder.advanceLexer();
            if (!parseMultiplicativeExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseMultiplicativeExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseUnaryExpression(isTarget)) {
            expr.drop();
            return false;
        }
        while (StarlarkTokenSets.MULTIPLICATIVE_OPERATIONS.contains(myBuilder.getTokenType())) {
            myBuilder.advanceLexer();
            if (!parseUnaryExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private boolean parseUnaryExpression(boolean isTarget) {
        if (StarlarkTokenSets.UNARY_OPERATIONS.contains(myBuilder.getTokenType())) {
            Marker expr = myBuilder.mark();
            myBuilder.advanceLexer();
            if (!parseUnaryExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.PREFIX_EXPRESSION);
            return true;
        }
        else {
            return parsePowerExpression(isTarget);
        }
    }

    private boolean parsePowerExpression(boolean isTarget) {
        Marker expr = myBuilder.mark();
        if (!parseMemberExpression(isTarget)) {
            expr.drop();
            return false;
        }
        if (atToken(StarlarkTokenTypes.EXP)) {
            myBuilder.advanceLexer();
            if (!parseUnaryExpression(isTarget)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            expr.done(StarlarkElementTypes.BINARY_EXPRESSION);
        }
        else {
            expr.drop();
        }
        return true;
    }

    private StarlarkElementType getTargetOrReferenceExpression(boolean isTarget) {
        if (isTarget && !Objects.equals(myBuilder.getTokenText(), "_")) {
            return StarlarkElementTypes.TARGET_EXPRESSION;
        }
        else if (Objects.equals(myBuilder.getTokenText(), "glob")) {
            return StarlarkElementTypes.GLOB_EXPRESSION;
        }
        else {
            return StarlarkElementTypes.REFERENCE_EXPRESSION;
        }
    }

    private static final TokenSet BRACKET_OR_COMMA = TokenSet.create(StarlarkTokenTypes.RBRACKET, StarlarkTokenTypes.COMMA);
    private static final TokenSet BRACKET_COLON_COMMA =
        TokenSet.create(StarlarkTokenTypes.RBRACKET, StarlarkTokenTypes.COLON, StarlarkTokenTypes.COMMA);
}
