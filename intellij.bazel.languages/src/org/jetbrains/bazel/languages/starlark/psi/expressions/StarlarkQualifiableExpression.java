package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jspecify.annotations.Nullable;

public interface StarlarkQualifiableExpression extends PsiElement {
    @Nullable String getName();

    /**
     * Check if the expression is qualified: of the form "a.b".
     */
    default boolean isQualified() {
        return getNode().findChildByType(StarlarkTokenTypes.DOT) != null;
    }

    /**
     * If the expression is qualified (of the form "a.b"), return the part the qualifier applies to ("a") as PsiElement.
     */
    default @Nullable PsiElement getQualifierExpression() {
        if (!isQualified()) {
            return null;
        }
        ASTNode firstChildNode = getNode().getFirstChildNode();
        return firstChildNode == null ? null : firstChildNode.getPsi();
    }

    /**
     * Check if the element is a simple (nonqualified) expression.
     */
    static boolean isSimpleNameExpression(PsiElement receiver) {
        return receiver instanceof StarlarkQualifiableExpression qualifiableExpression && !qualifiableExpression.isQualified();
    }

    /**
     * If the element is a simple (nonqualified) expression, return its name. Otherwise, return null.
     */
    static @Nullable String getSimpleNameOrNull(PsiElement receiver) {
        if (receiver instanceof StarlarkQualifiableExpression qualifiableExpression && !qualifiableExpression.isQualified()) {
            return qualifiableExpression.getName();
        }
        return null;
    }
}
