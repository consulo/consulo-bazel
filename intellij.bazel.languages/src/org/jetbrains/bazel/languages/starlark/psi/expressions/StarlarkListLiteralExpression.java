package org.jetbrains.bazel.languages.starlark.psi.expressions;

import java.util.Objects;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.starlark.StarlarkUtils;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.rename.StarlarkElementGenerator;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class StarlarkListLiteralExpression extends StarlarkBaseElement {
    public StarlarkListLiteralExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitListLiteralExpression(this);
    }

    public @Nullable StarlarkElement getElementAt(int index) {
        List<StarlarkElement> elements = getElements();
        return index >= 0 && index < elements.size() ? elements.get(index) : null;
    }

    public List<StarlarkElement> getElements() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkElement.class);
    }

    public void appendString(String string) {
        appendNode(new StarlarkElementGenerator(this.getProject()).createStringLiteralExpression(string));
    }

    /**
     * modified method from `com.jetbrains.python.psi.impl.PyElementGeneratorImpl.insertItemIntoList`
     */
    public void insertString(String toInsert) {
        ASTNode add = new StarlarkElementGenerator(this.getProject()).createStringLiteralExpression(toInsert);
        ASTNode exprNode = this.getNode();
        ASTNode[] closingTokens = exprNode.getChildren(TokenSet.create(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.LPAR));
        if (closingTokens.length == 0) {
            // we tried our best. let's just insert it at the end
            exprNode.addChild(add);
        }
        else {
            ASTNode next = StarlarkUtils.getNextNonWhitespaceSibling(closingTokens[closingTokens.length - 1]);
            if (next != null) {
                String whitespaceToAdd = next.getTreePrev() instanceof PsiWhiteSpace whiteSpace ? whiteSpace.getText() : null;
                exprNode.addChild(add, next);
                ASTNode comma = createComma();
                exprNode.addChild(comma, next);
                if (whitespaceToAdd != null) {
                    exprNode.addLeaf(StarlarkTokenTypes.SPACE, whitespaceToAdd, next);
                }
            }
            else {
                exprNode.addChild(add);
            }
        }
    }

    private void appendNode(ASTNode node) {
        ASTNode myNode = this.getNode();
        while (isNotActualListElementOrOpeningBracket(Objects.requireNonNull(myNode.getLastChildNode()))) {
            myNode.removeChild(Objects.requireNonNull(myNode.getLastChildNode()));
        }
        if (Objects.requireNonNull(myNode.getLastChildNode()).getElementType() != StarlarkTokenTypes.LBRACKET) {
            myNode.addChild(new StarlarkElementGenerator(this.getProject()).createTokenType(","));
        }

        myNode.addChild(node);
        myNode.addChild(new StarlarkElementGenerator(this.getProject()).createTokenType("]"));
    }

    private ASTNode createComma() {
        return new StarlarkElementGenerator(this.getProject()).createTokenType(",");
    }

    private boolean isNotActualListElementOrOpeningBracket(ASTNode node) {
        return node instanceof PsiWhiteSpace
            || node.getElementType() == StarlarkTokenTypes.COMMA
            || node.getElementType() == StarlarkTokenTypes.RBRACKET;
    }
}
