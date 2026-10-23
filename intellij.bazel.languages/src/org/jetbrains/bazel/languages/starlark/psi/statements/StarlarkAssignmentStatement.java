package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.application.util.function.Processor;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.StarlarkUtils;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jspecify.annotations.Nullable;

public class StarlarkAssignmentStatement extends StarlarkBaseElement implements StarlarkAssigningStatement {
    public StarlarkAssignmentStatement(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitAssignmentStatement(this);
    }

    @Override
    public @Nullable String getName() {
        StarlarkTargetExpression targetExpression = getTargetExpression();
        String name = targetExpression != null ? targetExpression.getName() : null;
        return name != null ? name : super.getName();
    }

    @Override
    public StarlarkTokenType operator() {
        return StarlarkTokenTypes.EQ;
    }

    public boolean check(Processor<StarlarkElement> processor) {
        return processTargets(leftHandSide(), processor);
    }

    public boolean isTopLevel() {
        return getParent() instanceof StarlarkFile;
    }

    private boolean processTargets(@Nullable PsiElement element, Processor<StarlarkElement> processor) {
        if (element == null) {
            return true;
        }
        else if (element instanceof StarlarkTargetExpression targetExpression) {
            return processor.process(targetExpression);
        }
        else if (element instanceof StarlarkParenthesizedExpression parenthesizedExpression) {
            return processTargets(parenthesizedExpression.getTuple(), processor);
        }
        else if (element instanceof StarlarkTupleExpression tupleExpression) {
            return tupleExpression.getTargetExpressions().stream().allMatch(it -> processor.process(it));
        }
        else if (element instanceof StarlarkListLiteralExpression listLiteralExpression) {
            PsiElement child = StarlarkUtils.nearestRelevantBeforeOperator(listLiteralExpression.getFirstChild());
            while (child != null) {
                if (!processTargets(child, processor)) {
                    return false;
                }
                child = StarlarkUtils.nearestRelevantBeforeOperator(child.getNextSibling());
            }
            return true;
        }
        else {
            return true;
        }
    }

    private @Nullable StarlarkTargetExpression getTargetExpression() {
        return findChildByClass(StarlarkTargetExpression.class);
    }
}
