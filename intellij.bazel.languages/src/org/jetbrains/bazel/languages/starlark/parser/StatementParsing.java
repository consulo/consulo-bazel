package org.jetbrains.bazel.languages.starlark.parser;

import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilder.Marker;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

class StatementParsing extends Parsing {
    StatementParsing(ParsingContext context) {
        super(context);
    }

    public void parseStatement() {
        while (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
            myBuilder.advanceLexer();
        }

        IElementType firstToken = myBuilder.getTokenType();
        if (firstToken == null) {
            return;
        }
        if (firstToken == StarlarkTokenTypes.IF_KEYWORD) {
            parseIfStatement();
        }
        else if (firstToken == StarlarkTokenTypes.FOR_KEYWORD) {
            parseForStatement(myBuilder.mark());
        }
        else if (firstToken == StarlarkTokenTypes.DEF_KEYWORD) {
            getContext().getFunctionParser().parseFunction(myBuilder.mark());
        }
        else {
            parseSimpleStatement();
        }
    }

    public void parseSimpleStatement() {
        IElementType firstToken = myBuilder.getTokenType();
        if (firstToken == null) {
            return;
        }
        if (firstToken == StarlarkTokenTypes.BREAK_KEYWORD) {
            parseKeywordStatement(StarlarkElementTypes.BREAK_STATEMENT);
        }
        else if (firstToken == StarlarkTokenTypes.CONTINUE_KEYWORD) {
            parseKeywordStatement(StarlarkElementTypes.CONTINUE_STATEMENT);
        }
        else if (firstToken == StarlarkTokenTypes.PASS_KEYWORD) {
            parseKeywordStatement(StarlarkElementTypes.PASS_STATEMENT);
        }
        else if (firstToken == StarlarkTokenTypes.RETURN_KEYWORD) {
            parseReturnStatement();
        }
        else if (firstToken == StarlarkTokenTypes.LOAD_KEYWORD) {
            parseLoadStatement();
        }
        else {
            parseExpressionStatement(firstToken);
        }
    }

    private void reportParseStatementError(PsiBuilder builder, IElementType firstToken) {
        if (firstToken == StarlarkTokenTypes.INCONSISTENT_DEDENT) {
            builder.error(StarlarkLocalize.parserUnindentDoesNotMatchAnyOuterIndent());
        }
        else if (firstToken == StarlarkTokenTypes.INDENT) {
            builder.error(StarlarkLocalize.parserUnexpectedIndent());
        }
        else {
            builder.error(StarlarkLocalize.parserStatementExpectedFound0(firstToken.toString()));
        }
    }

    private void checkEndOfStatement() {
        ParsingScope scope = getContext().getScope();
        if (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
            myBuilder.advanceLexer();
            scope.setAfterSemicolon(false);
        }
        else if (atToken(StarlarkTokenTypes.SEMICOLON)) {
            if (!scope.isSuite()) {
                myBuilder.advanceLexer();
                scope.setAfterSemicolon(true);
                if (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
                    myBuilder.advanceLexer();
                    scope.setAfterSemicolon(false);
                }
            }
        }
        else if (!myBuilder.eof()) {
            myBuilder.error(StarlarkLocalize.parserEndOfStatementExpected());
        }
    }

    private void parseKeywordStatement(IElementType statementType) {
        Marker statement = myBuilder.mark();
        myBuilder.advanceLexer();
        checkEndOfStatement();
        statement.done(statementType);
    }

    private void parseReturnStatement() {
        assertCurrentToken(StarlarkTokenTypes.RETURN_KEYWORD);
        Marker returnStatement = myBuilder.mark();
        myBuilder.advanceLexer();
        if (myBuilder.getTokenType() != null && !StarlarkTokenSets.ENDS_OF_STATEMENT.contains(myBuilder.getTokenType())) {
            getContext().getExpressionParser().parseExpression();
        }
        checkEndOfStatement();
        returnStatement.done(StarlarkElementTypes.RETURN_STATEMENT);
    }

    private void parseIfStatement() {
        assertCurrentToken(StarlarkTokenTypes.IF_KEYWORD);
        Marker ifStatement = myBuilder.mark();
        myBuilder.advanceLexer();
        if (!getContext().getExpressionParser().parseTestExpression(false)) {
            myBuilder.error(StarlarkLocalize.parserExpectedExpression());
        }
        parseColonAndSuite();
        while (atToken(StarlarkTokenTypes.ELIF_KEYWORD)) {
            myBuilder.advanceLexer();
            if (!getContext().getExpressionParser().parseTestExpression(false)) {
                myBuilder.error(StarlarkLocalize.parserExpectedExpression());
            }
            parseColonAndSuite();
        }
        if (atToken(StarlarkTokenTypes.ELSE_KEYWORD)) {
            myBuilder.advanceLexer();
            parseColonAndSuite();
        }
        ifStatement.done(StarlarkElementTypes.IF_STATEMENT);
    }

    private boolean expectColon() {
        if (atToken(StarlarkTokenTypes.COLON)) {
            myBuilder.advanceLexer();
            return true;
        }
        else if (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
            myBuilder.error(StarlarkLocalize.parserExpectedColon());
            return true;
        }
        Marker marker = myBuilder.mark();
        while (!atAnyOfTokens(
            Arrays.asList(
                null,
                StarlarkTokenTypes.DEDENT,
                StarlarkTokenTypes.STATEMENT_BREAK,
                StarlarkTokenTypes.COLON
            )
        )) {
            myBuilder.advanceLexer();
        }
        boolean result = matchToken(StarlarkTokenTypes.COLON);
        if (!result && atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
            myBuilder.advanceLexer();
        }
        marker.error(StarlarkLocalize.parserExpectedColon());
        return result;
    }

    private void parseForStatement(Marker endMarker) {
        assertCurrentToken(StarlarkTokenTypes.FOR_KEYWORD);
        myBuilder.advanceLexer();
        getContext().getExpressionParser().parseStarTargets();
        checkMatches(StarlarkTokenTypes.IN_KEYWORD, StarlarkLocalize.parserExpectedIn());
        getContext().getExpressionParser().parseExpression();
        parseColonAndSuite();
        endMarker.done(StarlarkElementTypes.FOR_STATEMENT);
    }

    private void parseColonAndSuite() {
        if (expectColon()) {
            parseSuite(null, null);
        }
        else {
            Marker mark = myBuilder.mark();
            mark.done(StarlarkElementTypes.STATEMENT_LIST);
        }
    }

    public void parseSuite(@Nullable Marker endMarker, @Nullable IElementType elType) {
        if (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
            myBuilder.advanceLexer();
            Marker marker = myBuilder.mark();
            boolean indentFound = atToken(StarlarkTokenTypes.INDENT);
            if (indentFound) {
                myBuilder.advanceLexer();
                while (!myBuilder.eof() && !atToken(StarlarkTokenTypes.DEDENT)) {
                    parseStatement();
                }
            }
            else {
                myBuilder.error(StarlarkLocalize.parserIndentExpected());
            }
            marker.done(StarlarkElementTypes.STATEMENT_LIST);
            if (endMarker != null) {
                endMarker.done(Objects.requireNonNull(elType));
            }
            if (indentFound && !myBuilder.eof()) {
                assert atToken(StarlarkTokenTypes.DEDENT);
                myBuilder.advanceLexer();
            }
        }
        else {
            Marker marker = myBuilder.mark();
            if (myBuilder.eof()) {
                myBuilder.error(StarlarkLocalize.parserExpectedStatement());
            }
            else {
                getContext().pushScope(getContext().getScope().withSuite());
                parseSimpleStatement();
                getContext().popScope();
                while (matchToken(StarlarkTokenTypes.SEMICOLON)) {
                    if (matchToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
                        break;
                    }
                    getContext().pushScope(getContext().getScope().withSuite());
                    parseSimpleStatement();
                    getContext().popScope();
                }
            }
            marker.done(StarlarkElementTypes.STATEMENT_LIST);
            if (endMarker != null) {
                endMarker.done(Objects.requireNonNull(elType));
            }
        }
    }

    private void parseLoadStatement() {
        assertCurrentToken(StarlarkTokenTypes.LOAD_KEYWORD);
        Marker loadStatement = myBuilder.mark();
        myBuilder.advanceLexer();
        parseLoadValueList();
        checkEndOfStatement();
        loadStatement.done(StarlarkElementTypes.LOAD_STATEMENT);
    }

    private void parseLoadValueList() {
        if (!atToken(StarlarkTokenTypes.LPAR)) {
            myBuilder.error(StarlarkLocalize.parserExpectedLpar());
            return;
        }
        myBuilder.advanceLexer();
        boolean firstValue = true;
        while (!atToken(StarlarkTokenTypes.RPAR)) {
            if (!firstValue) {
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

            if (firstValue) {
                firstValue = false;
                Marker filenameLoadValueMarker = myBuilder.mark();
                parseLoadValue();
                filenameLoadValueMarker.done(StarlarkElementTypes.FILENAME_LOAD_VALUE);
                continue;
            }
            else if (isIdentifierLike(myBuilder)) {
                Marker namedLoadValueMarker = myBuilder.mark();
                advanceIdentifierLike(myBuilder);
                if (atToken(StarlarkTokenTypes.EQ)) {
                    myBuilder.advanceLexer();
                    parseLoadValue();
                    namedLoadValueMarker.done(StarlarkElementTypes.NAMED_LOAD_VALUE);
                    continue;
                }
                namedLoadValueMarker.rollbackTo();
            }
            Marker loadValueMarker = myBuilder.mark();
            parseLoadValue();
            loadValueMarker.done(StarlarkElementTypes.STRING_LOAD_VALUE);
        }
        checkMatches(StarlarkTokenTypes.RPAR, StarlarkLocalize.parserExpectedRpar());
    }

    private void parseLoadValue() {
        if (atToken(StarlarkTokenTypes.STRING)) {
            buildTokenElement(StarlarkElementTypes.STRING_LITERAL_EXPRESSION, myBuilder);
        }
        else {
            advanceError(myBuilder, StarlarkLocalize.parserExpectedString());
        }
    }

    private void parseExpressionStatement(IElementType firstToken) {
        Marker exprStatement = myBuilder.mark();
        if (getContext().getExpressionParser().parseExpressionOptional()) {
            StarlarkElementType statementType = StarlarkElementTypes.EXPRESSION_STATEMENT;
            if (StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS.contains(myBuilder.getTokenType())) {
                statementType = StarlarkElementTypes.AUG_ASSIGNMENT_STATEMENT;
                myBuilder.advanceLexer();
                if (!getContext().getExpressionParser().parseExpressionOptional()) {
                    myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                }
            }
            else if (atToken(StarlarkTokenTypes.EQ)) {
                exprStatement.rollbackTo();
                exprStatement = myBuilder.mark();
                getContext().getExpressionParser().parseExpression(true);
                assertCurrentToken(StarlarkTokenTypes.EQ);
                statementType = StarlarkElementTypes.ASSIGNMENT_STATEMENT;
                myBuilder.advanceLexer();
                while (true) {
                    Marker maybeExprMarker = myBuilder.mark();
                    if (!getContext().getExpressionParser().parseExpressionOptional()) {
                        maybeExprMarker.drop();
                        myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                        break;
                    }
                    if (atToken(StarlarkTokenTypes.EQ)) {
                        maybeExprMarker.rollbackTo();
                        getContext().getExpressionParser().parseExpression(true);
                        assertCurrentToken(StarlarkTokenTypes.EQ);
                        myBuilder.advanceLexer();
                    }
                    else {
                        maybeExprMarker.drop();
                        break;
                    }
                }
            }
            checkEndOfStatement();
            exprStatement.done(statementType);
            return;
        }
        else {
            exprStatement.drop();
        }
        myBuilder.advanceLexer();
        reportParseStatementError(myBuilder, firstToken);
    }
}
