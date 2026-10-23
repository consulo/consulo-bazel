package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class StarlarkCompExpression extends StarlarkBaseElement {
    public StarlarkCompExpression(ASTNode node) {
        super(node);
    }

    public List<StarlarkTargetExpression> getCompVariables() {
        List<StarlarkTargetExpression> result = new ArrayList<>();
        for (StarlarkTupleExpression tuple : getCompVariableTuples()) {
            result.addAll(tuple.getTargetExpressions());
        }
        result.addAll(Arrays.asList(findChildrenByClass(StarlarkTargetExpression.class)));
        return result;
    }

    private List<StarlarkTupleExpression> getCompVariableTuples() {
        List<StarlarkTupleExpression> result = new ArrayList<>();
        for (StarlarkParenthesizedExpression parenthesized : findChildrenByClass(StarlarkParenthesizedExpression.class)) {
            StarlarkTupleExpression tuple = parenthesized.getTuple();
            if (tuple != null) {
                result.add(tuple);
            }
        }
        result.addAll(Arrays.asList(findChildrenByClass(StarlarkTupleExpression.class)));
        return result;
    }
}
