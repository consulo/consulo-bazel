package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class StarlarkBinaryExpression extends StarlarkBaseElement {
    public StarlarkBinaryExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitBinaryExpression(this);
    }

    public List<PsiElement> getOperands() {
        return findChildrenByType(StarlarkElementTypes.EXPRESSIONS);
    }

    public @Nullable PsiElement getLeftOperand() {
        List<PsiElement> operands = getOperands();
        return operands.isEmpty() ? null : operands.get(0);
    }

    public @Nullable PsiElement getRightOperand() {
        List<PsiElement> operands = getOperands();
        return operands.size() > 1 ? operands.get(1) : null;
    }

    /**
     * The PsiElement of the operator token that sits between the two operands.
     * For compound operations the first keyword element is returned.
     */
    public @Nullable PsiElement getOperatorElement() {
        return findChildByType(StarlarkTokenSets.BINARY_OPERATIONS);
    }

    /**
     * The IElementType of the operator token that sits between the two operands (e.g. `+`, `-`).
     * For compound operations ("not in" and "is not") the first keyword is returned.
     */
    public @Nullable IElementType getOperator() {
        PsiElement operatorElement = getOperatorElement();
        return operatorElement == null ? null : PsiUtilCore.getElementType(operatorElement);
    }
}
