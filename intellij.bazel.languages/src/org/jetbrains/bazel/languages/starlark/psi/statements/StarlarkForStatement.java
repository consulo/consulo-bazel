package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class StarlarkForStatement extends StarlarkBaseElement implements StarlarkStatementContainer, StarlarkAssigningStatement {
    public StarlarkForStatement(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitForStatement(this);
    }

    @Override
    public StarlarkTokenType operator() {
        return StarlarkTokenTypes.IN_KEYWORD;
    }

    public List<StarlarkTargetExpression> getLoopVariables() {
        StarlarkTargetExpression loopVariable = getLoopVariable();
        if (loopVariable != null) {
            return List.of(loopVariable);
        }
        StarlarkTupleExpression loopTuple = getLoopTuple();
        if (loopTuple != null) {
            return loopTuple.getTargetExpressions();
        }
        return List.of();
    }

    private @Nullable StarlarkTargetExpression getLoopVariable() {
        return findChildByClass(StarlarkTargetExpression.class);
    }

    private @Nullable StarlarkTupleExpression getLoopTuple() {
        StarlarkTupleExpression tupleExpression = findChildByClass(StarlarkTupleExpression.class);
        if (tupleExpression != null) {
            return tupleExpression;
        }
        StarlarkParenthesizedExpression parenthesizedExpression = findChildByClass(StarlarkParenthesizedExpression.class);
        return parenthesizedExpression != null ? parenthesizedExpression.getTuple() : null;
    }

    @Override
    public List<StarlarkStatementList> getStatementLists() {
        return List.of(findChildrenByClass(StarlarkStatementList.class));
    }
}
