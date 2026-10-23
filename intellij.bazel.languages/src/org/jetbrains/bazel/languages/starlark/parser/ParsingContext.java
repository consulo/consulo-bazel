package org.jetbrains.bazel.languages.starlark.parser;

import consulo.language.parser.PsiBuilder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

class ParsingContext {
    private final PsiBuilder myBuilder;
    private final ExpressionParsing myExpressionParser;
    private final StatementParsing myStatementParser;
    private final FunctionParsing myFunctionParser;
    private final Deque<ParsingScope> myScopes;

    ParsingContext(PsiBuilder builder) {
        myBuilder = builder;
        myExpressionParser = new ExpressionParsing(this);
        myStatementParser = new StatementParsing(this);
        myFunctionParser = new FunctionParsing(this);
        myScopes = new ArrayDeque<>(List.of(emptyParsingScope()));
    }

    public PsiBuilder getBuilder() {
        return myBuilder;
    }

    public ExpressionParsing getExpressionParser() {
        return myExpressionParser;
    }

    public StatementParsing getStatementParser() {
        return myStatementParser;
    }

    public FunctionParsing getFunctionParser() {
        return myFunctionParser;
    }

    public ParsingScope popScope() {
        ParsingScope prevScope = myScopes.pop();
        resetBuilderCache(prevScope);
        return prevScope;
    }

    public void pushScope(ParsingScope scope) {
        ParsingScope prevScope = getScope();
        myScopes.push(scope);
        resetBuilderCache(prevScope);
    }

    public ParsingScope getScope() {
        return myScopes.peek();
    }

    public ParsingScope emptyParsingScope() {
        return new ParsingScope();
    }

    private void resetBuilderCache(ParsingScope prevScope) {
        if (!getScope().equals(prevScope)) {
            myBuilder.mark().rollbackTo();
        }
    }
}
