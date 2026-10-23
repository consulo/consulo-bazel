package org.jetbrains.bazel.languages.starlark.psi;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.rename.StarlarkElementGenerator;
import org.jspecify.annotations.Nullable;

public abstract class StarlarkNamedElement extends StarlarkBaseElement implements PsiNameIdentifierOwner {
    public StarlarkNamedElement(ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable String getName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier.getText() : null;
    }

    @Override
    public PsiElement setName(String name) {
        PsiElement nameIdentifier = getNameIdentifier();
        ASTNode oldNode = nameIdentifier != null ? nameIdentifier.getNode() : null;
        if (oldNode == null) {
            return this;
        }
        ASTNode newNode = new StarlarkElementGenerator(getProject()).createNameIdentifier(name);
        getNode().replaceChild(oldNode, newNode);
        return this;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return findChildByType(StarlarkTokenTypes.IDENTIFIER);
    }

    @Override
    public int getTextOffset() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier.getTextRange().getStartOffset() : super.getTextOffset();
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getName() + "')";
    }
}
