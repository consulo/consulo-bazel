package org.jetbrains.bazel.languages.bazelquery.parser;

import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.parser.PsiBuilder;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryElementTypes;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes;
import org.jspecify.annotations.Nullable;

// consulo: Kotlin `PsiBuilder by builder` delegation is replaced by explicit calls on myBuilder
class ParsingNoFlags {
    private final IElementType myRoot;
    private final PsiBuilder myBuilder;

    private final ParsingUtils myUtils;

    ParsingNoFlags(IElementType root, PsiBuilder builder) {
        myRoot = root;
        myBuilder = builder;
        myUtils = new ParsingUtils(builder);
    }

    public PsiBuilder getBuilder() {
        return myBuilder;
    }

    private static final TokenSet WORD_IN_SQ_QUERY =
        TokenSet.create(
            BazelQueryTokenTypes.UNQUOTED_WORD,
            BazelQueryTokenTypes.DQ_WORD,
            BazelQueryTokenTypes.DQ_UNFINISHED,
            BazelQueryTokenTypes.DQ_EMPTY,
            BazelQueryTokenTypes.ERR_WORD
        );

    private static final TokenSet WORD_IN_DQ_QUERY =
        TokenSet.create(
            BazelQueryTokenTypes.UNQUOTED_WORD,
            BazelQueryTokenTypes.SQ_WORD,
            BazelQueryTokenTypes.SQ_UNFINISHED,
            BazelQueryTokenTypes.SQ_EMPTY,
            BazelQueryTokenTypes.ERR_WORD
        );

    private static final TokenSet WORD_IN_UNQ_QUERY =
        TokenSet.create(
            BazelQueryTokenTypes.UNQUOTED_WORD,
            BazelQueryTokenTypes.SQ_WORD,
            BazelQueryTokenTypes.SQ_UNFINISHED,
            BazelQueryTokenTypes.SQ_EMPTY,
            BazelQueryTokenTypes.DQ_WORD,
            BazelQueryTokenTypes.DQ_UNFINISHED,
            BazelQueryTokenTypes.DQ_EMPTY,
            BazelQueryTokenTypes.ERR_WORD
        );

    private static final TokenSet PATTERN_IN_SQ_QUERY =
        TokenSet.orSet(
            WORD_IN_SQ_QUERY,
            TokenSet.create(BazelQueryTokenTypes.DQ_PATTERN)
        );

    private static final TokenSet PATTERN_IN_DQ_QUERY =
        TokenSet.orSet(
            WORD_IN_DQ_QUERY,
            TokenSet.create(BazelQueryTokenTypes.SQ_PATTERN)
        );

    private static final TokenSet PATTERN_IN_UNQ_QUERY =
        TokenSet.orSet(
            WORD_IN_UNQ_QUERY,
            TokenSet.create(BazelQueryTokenTypes.SQ_PATTERN, BazelQueryTokenTypes.DQ_PATTERN)
        );

    private TokenSet getAvailableWordsSet(IElementType queryQuotes) {
        if (queryQuotes == BazelQueryTokenTypes.WHITE_SPACE) {
            return WORD_IN_UNQ_QUERY;
        }
        else if (queryQuotes == BazelQueryTokenTypes.SINGLE_QUOTE) {
            return WORD_IN_SQ_QUERY;
        }
        else if (queryQuotes == BazelQueryTokenTypes.DOUBLE_QUOTE) {
            return WORD_IN_DQ_QUERY;
        }
        else {
            return TokenSet.EMPTY;
        }
    }

    private TokenSet getAvailablePatternsSet(IElementType queryQuotes) {
        if (queryQuotes == BazelQueryTokenTypes.WHITE_SPACE) {
            return PATTERN_IN_UNQ_QUERY;
        }
        else if (queryQuotes == BazelQueryTokenTypes.SINGLE_QUOTE) {
            return PATTERN_IN_SQ_QUERY;
        }
        else if (queryQuotes == BazelQueryTokenTypes.DOUBLE_QUOTE) {
            return PATTERN_IN_DQ_QUERY;
        }
        else {
            return TokenSet.EMPTY;
        }
    }

    public ASTNode parseFile() {
        PsiBuilder.Marker prompt = myBuilder.mark();

        parseExpr(BazelQueryTokenTypes.WHITE_SPACE);

        while (!myBuilder.eof()) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorUnexpectedToken());
        }

        prompt.done(myRoot);
        return myBuilder.getTreeBuilt();
    }

    // _____parsing queries____

    // expr ::= word
    //       | let name = expr in expr
    //       | (expr)
    //       | expr intersect expr
    //       | expr ^ expr
    //       | expr union expr
    //       | expr + expr
    //       | expr except expr
    //       | expr - expr
    //       | set(word *)
    //       | word '(' int | word | expr ... ')'
    private IElementType parseExpr(IElementType initialQueryQuotes) {
        IElementType queryQuotes = initialQueryQuotes;
        if (myUtils.atAnyToken(getAvailableWordsSet(queryQuotes))) {
            queryQuotes = parseWord(queryQuotes);
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.LET)) {
            myBuilder.advanceLexer();
            if (myUtils.atToken(queryQuotes)) {
                return queryQuotes;
            }
            myUtils.expectToken(BazelQueryTokenTypes.UNQUOTED_WORD);
            if (myUtils.atToken(queryQuotes)) {
                return queryQuotes;
            }
            myUtils.expectToken(BazelQueryTokenTypes.EQUALS);
            if (myUtils.atToken(queryQuotes)) {
                return queryQuotes;
            }
            if (myUtils.atToken(BazelQueryTokenTypes.IN)) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingVariableValue());
            }
            queryQuotes = parseExpr(queryQuotes);
            myUtils.expectToken(BazelQueryTokenTypes.IN);
            if (myUtils.atToken(queryQuotes)) {
                return queryQuotes;
            }
            queryQuotes = parseExpr(queryQuotes);
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.LEFT_PAREN)) {
            myBuilder.advanceLexer();
            queryQuotes = parseExpr(queryQuotes);
            if (myUtils.atToken(queryQuotes)) {
                return queryQuotes;
            }
            myUtils.expectToken(BazelQueryTokenTypes.RIGHT_PAREN);
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.SET)) {
            myBuilder.advanceLexer();
            if (myUtils.atToken(queryQuotes)) {
                return queryQuotes;
            }
            myUtils.expectToken(BazelQueryTokenTypes.LEFT_PAREN);
            if (!myUtils.matchAnyToken(getAvailableWordsSet(queryQuotes))) {
                myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingTarget());
            }
            while (!myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) && !myUtils.atToken(queryQuotes) && !myBuilder.eof()) {
                if (!myUtils.matchAnyToken(getAvailableWordsSet(queryQuotes))) {
                    myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingTarget());
                }
            }
            if (!myUtils.matchToken(
                BazelQueryTokenTypes.RIGHT_PAREN
            )
            ) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingRightParenthesis());
            }
        }
        else if (myUtils.atAnyToken(BazelQueryTokenSets.COMMANDS)) {
            queryQuotes = parseCommand(queryQuotes);
        }
        else if (myUtils.atToken(queryQuotes)) {
            return queryQuotes;
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.UNION) ||
            myUtils.atToken(BazelQueryTokenTypes.EXCEPT) ||
            myUtils.atToken(BazelQueryTokenTypes.INTERSECT)) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorUnexpectedTokenInfixOperator());
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.INTEGER)) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorUnexpectedTokenInteger());
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN)) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorUnexpectedTokenParen());
        }

        if (myUtils.atToken(BazelQueryTokenTypes.INTERSECT)) {
            myBuilder.advanceLexer();
            queryQuotes = parseExpr(queryQuotes);
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.EXCEPT)) {
            myBuilder.advanceLexer();
            queryQuotes = parseExpr(queryQuotes);
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.UNION)) {
            myBuilder.advanceLexer();
            queryQuotes = parseExpr(queryQuotes);
        }

        return queryQuotes;
    }

// _____parsing commands____

    private IElementType parseExprArg(IElementType initialQueryQuotes) {
        IElementType queryQuotes = initialQueryQuotes;
        if (myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
            myUtils.atToken(BazelQueryTokenTypes.COMMA) ||
            myBuilder.eof()
        ) {
            myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingExpression());
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.INTEGER)) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingExpressionGotInteger());
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.SQ_PATTERN) ||
            myUtils.atToken(BazelQueryTokenTypes.DQ_PATTERN)
        ) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorIncorrectWord());
        }
        else {
            queryQuotes = parseExpr(queryQuotes);
        }
        return queryQuotes;
    }

    private IElementType parseWordArg(IElementType initialQueryQuotes) {
        IElementType queryQuotes = initialQueryQuotes;
        if (myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
            myUtils.atToken(BazelQueryTokenTypes.COMMA) ||
            myBuilder.eof()
        ) {
            myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingWord());
        }
        else if (!myUtils.atAnyToken(getAvailableWordsSet(queryQuotes))) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingWord());
        }
        else {
            queryQuotes = parseWord(queryQuotes);
        }

        return queryQuotes;
    }

    private IElementType parsePatternArg(IElementType initialQueryQuotes) {
        IElementType queryQuotes = initialQueryQuotes;
        if (myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
            myUtils.atToken(BazelQueryTokenTypes.COMMA) ||
            myBuilder.eof()
        ) {
            myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingWord());
        }
        else if (!myUtils.atAnyToken(getAvailablePatternsSet(queryQuotes)) &&
            !myUtils.atToken(BazelQueryTokenTypes.INTEGER)
        ) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingWord());
        }
        else {
            queryQuotes = parseWord(queryQuotes);
        }

        return queryQuotes;
    }

    private void parseOptionalIntArg(IElementType queryQuotes) {
        if (myUtils.matchToken(BazelQueryTokenTypes.COMMA)) {
            if (myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
                myUtils.atToken(BazelQueryTokenTypes.COMMA) ||
                myBuilder.eof()
            ) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingExpression());
            }
            else if (!myUtils.atToken(BazelQueryTokenTypes.INTEGER)) {
                myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingInteger());
            }
            else {
                parseInteger();
            }
        }
    }

    private IElementType parseUnknownArgs(IElementType initialQueryQuotes) {
        IElementType queryQuotes = initialQueryQuotes;
        if (myUtils.atToken(BazelQueryTokenTypes.INTEGER)) {
            parseInteger();
        }
        else if (!myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN)) {
            parseExpr(queryQuotes);
        }
        while (!myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) && !myUtils.atToken(queryQuotes) && !myBuilder.eof()) {
            myUtils.expectToken(BazelQueryTokenTypes.COMMA);
            if (myUtils.atToken(queryQuotes) ||
                myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
                myBuilder.eof()
            ) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingExpression());
            }
            else if (myUtils.atToken(BazelQueryTokenTypes.INTEGER)) {
                parseInteger();
            }
            else {
                queryQuotes = parseExpr(queryQuotes);
            }
        }
        return queryQuotes;
    }

    private IElementType parseCommand(IElementType initialQueryQuotes) {
        IElementType queryQuotes = initialQueryQuotes;

        PsiBuilder.Marker command = myBuilder.mark();

        @Nullable IElementType commandKeyword = myBuilder.getTokenType();
        myBuilder.advanceLexer();
        if (myUtils.atToken(queryQuotes)) {
            return queryQuotes;
        }
        myUtils.expectToken(BazelQueryTokenTypes.LEFT_PAREN);
        if (myUtils.atToken(queryQuotes)) {
            return queryQuotes;
        }

        if (commandKeyword == BazelQueryTokenTypes.SAME_PKG_DIRECT_RDEPS ||
            commandKeyword == BazelQueryTokenTypes.SIBLINGS ||
            commandKeyword == BazelQueryTokenTypes.TESTS ||
            commandKeyword == BazelQueryTokenTypes.BUILDFILES ||
            commandKeyword == BazelQueryTokenTypes.LOADFILES
        ) {
            queryQuotes = parseExprArg(queryQuotes);
        }
        else if (commandKeyword == BazelQueryTokenTypes.ALLPATHS ||
            commandKeyword == BazelQueryTokenTypes.SOMEPATH ||
            commandKeyword == BazelQueryTokenTypes.VISIBLE
        ) {
            queryQuotes = parseExprArg(queryQuotes);
            if (!myUtils.matchToken(BazelQueryTokenTypes.COMMA)) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingComma());
            }
            queryQuotes = parseExprArg(queryQuotes);
        }
        else if (commandKeyword == BazelQueryTokenTypes.LABELS ||
            commandKeyword == BazelQueryTokenTypes.KIND ||
            commandKeyword == BazelQueryTokenTypes.FILTER
        ) {
            queryQuotes = parsePatternArg(queryQuotes);
            if (!myUtils.matchToken(BazelQueryTokenTypes.COMMA)) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingComma());
            }
            queryQuotes = parseExprArg(queryQuotes);
        }
        else if (commandKeyword == BazelQueryTokenTypes.DEPS ||
            commandKeyword == BazelQueryTokenTypes.ALLRDEPS ||
            commandKeyword == BazelQueryTokenTypes.SOME
        ) {
            queryQuotes = parseExprArg(queryQuotes);
            parseOptionalIntArg(queryQuotes);
        }
        else if (commandKeyword == BazelQueryTokenTypes.RDEPS) {
            queryQuotes = parseExprArg(queryQuotes);
            if (!myUtils.matchToken(BazelQueryTokenTypes.COMMA)) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingComma());
            }
            queryQuotes = parseExprArg(queryQuotes);
            parseOptionalIntArg(queryQuotes);
        }
        else if (commandKeyword == BazelQueryTokenTypes.ATTR) {
            queryQuotes = parseWordArg(queryQuotes);
            if (!myUtils.matchToken(BazelQueryTokenTypes.COMMA)) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingComma());
            }
            queryQuotes = parsePatternArg(queryQuotes);
            if (!myUtils.matchToken(BazelQueryTokenTypes.COMMA)) {
                myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingComma());
            }
            queryQuotes = parseExprArg(queryQuotes);
        }
        else if (commandKeyword == BazelQueryTokenTypes.RBUILDFILES) {
            queryQuotes = parseWordArg(queryQuotes);
            while (myUtils.atToken(BazelQueryTokenTypes.COMMA) && !myBuilder.eof()) {
                if (!myUtils.matchToken(BazelQueryTokenTypes.COMMA)) {
                    myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingComma());
                }
                queryQuotes = parseWordArg(queryQuotes);
            }
        }
        else {
            queryQuotes = parseUnknownArgs(queryQuotes);
        }

        if (myBuilder.eof() || myUtils.atToken(queryQuotes)) {
            myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingRightParenthesis());
        }
        while (!myBuilder.eof() && !myUtils.atToken(queryQuotes) && !myUtils.atToken(BazelQueryTokenTypes.RIGHT_PAREN)) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorUnexpectedToken());
        }
        if (!myUtils.matchToken(BazelQueryTokenTypes.RIGHT_PAREN)) {
            myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingRightParenthesis());
        }
        command.done(BazelQueryElementTypes.COMMAND);

        return queryQuotes;
    }

    // _____parsing words____

    private IElementType parseWord(IElementType initialQueryQuotes) {
        PsiBuilder.Marker word = myBuilder.mark();
        IElementType queryQuotes = initialQueryQuotes;

        if (myUtils.atToken(BazelQueryTokenTypes.SQ_EMPTY) ||
            myUtils.atToken(BazelQueryTokenTypes.DQ_EMPTY)
        ) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorEmptyQuotation());
        }
        else if (myUtils.matchToken(BazelQueryTokenTypes.SQ_UNFINISHED) ||
            myUtils.matchToken(BazelQueryTokenTypes.DQ_UNFINISHED)
        ) {
            myBuilder.error(BazelLanguagesLocalize.bazelqueryErrorMissingQuote());
        }
        else if (myUtils.atToken(BazelQueryTokenTypes.ERR_WORD)) {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorIncorrectWord());
        }
        else {
            if (queryQuotes == BazelQueryTokenTypes.WHITE_SPACE) {
                if (myUtils.atToken(BazelQueryTokenTypes.SQ_WORD)) {
                    queryQuotes = BazelQueryTokenTypes.DOUBLE_QUOTE;
                }
                else if (myUtils.atToken(BazelQueryTokenTypes.DQ_WORD)) {
                    queryQuotes = BazelQueryTokenTypes.SINGLE_QUOTE;
                }
            }
            myBuilder.advanceLexer();
        }
        word.done(BazelQueryElementTypes.WORD);
        return queryQuotes;
    }

    // _____parsing integers____

    private void parseInteger() {
        PsiBuilder.Marker integer = myBuilder.mark();

        if (myUtils.atToken(BazelQueryTokenTypes.INTEGER)) {
            myBuilder.advanceLexer();
        }
        else {
            myUtils.advanceError(BazelLanguagesLocalize.bazelqueryErrorMissingInteger());
        }

        integer.done(BazelQueryElementTypes.INTEGER);
    }
}
