package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StarlarkDictLiteralExpression extends StarlarkBaseElement {
    public StarlarkDictLiteralExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitDictLiteralExpression(this);
    }

    public @Nullable List<PsiElement> getKeys() {
        List<PsiElement> keys = new ArrayList<>();
        PsiElement current = getFirstChild();
        while (current != null) {
            if (current instanceof StarlarkKeyValueExpression) {
                ASTNode keyNode = current.getNode().findChildByType(StarlarkElementTypes.EXPRESSIONS);
                PsiElement key = keyNode == null ? null : keyNode.getPsi();
                if (key == null) {
                    return null;
                }
                keys.add(key);
            }
            else if (current instanceof StarlarkBaseElement) {
                return null;
            }
            current = current.getNextSibling();
        }
        return keys;
    }
}
