package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jspecify.annotations.Nullable;

public interface StarlarkArgumentElement extends StarlarkElement {
    default @Nullable PsiElement getValue() {
        ASTNode childNode = getNode().getLastChildNode();
        while (childNode != null) {
            IElementType type = childNode.getElementType();
            if (StarlarkElementTypes.EXPRESSIONS.contains(type)) {
                return childNode.getPsi();
            }
            if (type == StarlarkTokenTypes.EQ) {
                break;
            }
            childNode = childNode.getTreePrev();
        }
        return null;
    }
}
