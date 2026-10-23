package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StarlarkTupleExpression extends StarlarkBaseElement {
    public StarlarkTupleExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitTupleExpression(this);
    }

    public List<StarlarkTargetExpression> getTargetExpressions() {
        List<StarlarkTargetExpression> result = new ArrayList<>(Arrays.asList(findChildrenByClass(StarlarkTargetExpression.class)));
        for (StarlarkTupleExpression tuple : getNestedTuples()) {
            result.addAll(tuple.getTargetExpressions());
        }
        return result;
    }

    private List<StarlarkTupleExpression> getNestedTuples() {
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
