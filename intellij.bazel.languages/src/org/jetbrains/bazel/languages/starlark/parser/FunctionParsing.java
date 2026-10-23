package org.jetbrains.bazel.languages.starlark.parser;

import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder.Marker;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;

import java.util.Arrays;
import java.util.List;

class FunctionParsing extends Parsing {
    FunctionParsing(ParsingContext context) {
        super(context);
    }

    public void parseFunction(Marker functionMarker) {
        assertCurrentToken(StarlarkTokenTypes.DEF_KEYWORD);
        myBuilder.advanceLexer();
        parseIdentifierOrSkip();
        parseParameterList();
        checkMatches(StarlarkTokenTypes.COLON, StarlarkLocalize.parserExpectedColon());
        getContext().pushScope(getContext().getScope().withFunction());
        getContext().getStatementParser().parseSuite(functionMarker, StarlarkElementTypes.FUNCTION_DECLARATION);
        getContext().popScope();
    }

    private void parseParameterList() {
        if (!atToken(StarlarkTokenTypes.LPAR)) {
            myBuilder.error(StarlarkLocalize.parserExpectedLpar());
            myBuilder.mark().done(StarlarkElementTypes.PARAMETER_LIST); // To have non-empty parameters list at all the time.
            return;
        }
        parseParameterListContents(StarlarkTokenTypes.RPAR, true);
    }

    public void parseParameterListContents(IElementType endToken, boolean advanceLexer) {
        Marker parameterList = myBuilder.mark();
        if (advanceLexer) {
            myBuilder.advanceLexer();
        }
        boolean first = true;
        boolean afterStarParameter = false;
        while (!atToken(endToken)) {
            if (first) {
                first = false;
            }
            else {
                if (atToken(StarlarkTokenTypes.COMMA)) {
                    myBuilder.advanceLexer();
                }
                else {
                    myBuilder.error(StarlarkLocalize.parserExpectedCommaLparRpar());
                    break;
                }
            }
            boolean isStarParameter = atAnyOfTokens(List.of(StarlarkTokenTypes.MULT, StarlarkTokenTypes.EXP));
            if (!parseParameter(endToken)) {
                if (afterStarParameter) {
                    myBuilder.error(StarlarkLocalize.parserExpectedExpression());
                }
                break;
            }
            if (isStarParameter) {
                afterStarParameter = true;
            }
        }
        if (atToken(endToken) && endToken == StarlarkTokenTypes.RPAR) {
            myBuilder.advanceLexer();
        }
        parameterList.done(StarlarkElementTypes.PARAMETER_LIST);
        if (atToken(endToken) && endToken == StarlarkTokenTypes.COLON) {
            myBuilder.advanceLexer();
        }
    }

    private boolean parseParameter(IElementType endToken) {
        Marker parameter = myBuilder.mark();
        StarlarkElementType parameterType = StarlarkElementTypes.MANDATORY_PARAMETER;
        if (atToken(StarlarkTokenTypes.MULT)) {
            myBuilder.advanceLexer();
            if (atToken(StarlarkTokenTypes.COMMA)) {
                parameter.done(StarlarkElementTypes.KEYWORD_ONLY_BOUNDARY);
                return true;
            }
            parameterType = StarlarkElementTypes.VARIADIC_PARAMETER;
        }
        else if (atToken(StarlarkTokenTypes.EXP)) {
            myBuilder.advanceLexer();
            parameterType = StarlarkElementTypes.KEYWORD_VARIADIC_PARAMETER;
        }
        if (matchToken(StarlarkTokenTypes.IDENTIFIER)) {
            if (parameterType == StarlarkElementTypes.MANDATORY_PARAMETER && matchToken(StarlarkTokenTypes.EQ)) {
                if (!getContext().getExpressionParser().parseSingleExpression(false)) {
                    Marker invalidElements = myBuilder.mark();
                    while (!atAnyOfTokens(Arrays.asList(endToken, StarlarkTokenTypes.LINE_BREAK, StarlarkTokenTypes.COMMA, null))) {
                        nextToken();
                    }
                    invalidElements.error(StarlarkLocalize.parserExpectedExpression());
                }
                parameter.done(StarlarkElementTypes.OPTIONAL_PARAMETER);
                return true;
            }
            parameter.done(parameterType);
            return true;
        }
        else {
            parameter.rollbackTo();
            if (atToken(endToken)) {
                return false;
            }
            Marker invalidElements = myBuilder.mark();
            while (!atAnyOfTokens(Arrays.asList(endToken, StarlarkTokenTypes.LINE_BREAK, StarlarkTokenTypes.COMMA, null))) {
                nextToken();
            }
            invalidElements.error(StarlarkLocalize.parserExpectedFormalParamName());
            return atToken(endToken) || atToken(StarlarkTokenTypes.COMMA);
        }
    }
}
