package org.jetbrains.bazel.languages.bazelrc.psi;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes;
import org.jspecify.annotations.Nullable;

public class BazelrcImport extends BazelrcBaseElement {
    public BazelrcImport(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(BazelrcElementVisitor visitor) {
        visitor.visitImport(this);
    }

    @Override
    public @Nullable PsiReference getReference() {
        BazelrcReferenceSupport support = BazelrcReferenceSupport.getInstance();
        return support == null ? null : support.getReference(this);
    }

    @Override
    public PsiReference[] getReferences() {
        BazelrcReferenceSupport support = BazelrcReferenceSupport.getInstance();
        return support == null ? PsiReference.EMPTY_ARRAY : support.getReferences(this);
    }

    public @Nullable PsiElement getImportPath() {
        return findChildByType(BazelrcTokenTypes.VALUE);
    }

    public boolean isOptional() {
        PsiElement importElement = findChildByType(BazelrcTokenTypes.IMPORT);
        return importElement != null && importElement.getText().startsWith("try-");
    }
}
