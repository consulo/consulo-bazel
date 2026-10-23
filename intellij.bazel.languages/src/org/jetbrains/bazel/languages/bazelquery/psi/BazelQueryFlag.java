package org.jetbrains.bazel.languages.bazelquery.psi;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BazelQueryFlag extends BazelQueryBaseElement {
    public BazelQueryFlag(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(BazelQueryElementVisitor visitor) {
        visitor.visitFlag(this);
    }

    // consulo: upstream property `name` (getName(): PsiElement?) clashes with PsiElementBase.getName(): String
    public @Nullable PsiElement getNameElement() {
        PsiElement flag = findChildByType(BazelQueryTokenTypes.FLAG);
        return flag != null ? flag : findChildByType(BazelQueryTokenTypes.FLAG_NO_VAL);
    }

    public Collection<PsiReference> getOwnReferences() {
        BazelQueryReferenceSupport support = BazelQueryReferenceSupport.getInstance();
        return support == null ? List.of() : support.getOwnReferences(this);
    }

    // consulo: upstream PsiSymbolReference own references are merged into getReferences()
    @Override
    public PsiReference[] getReferences() {
        List<PsiReference> references = new ArrayList<>();
        PsiReference reference = getReference();
        if (reference != null) {
            references.add(reference);
        }
        references.addAll(getOwnReferences());
        return references.toArray(PsiReference.EMPTY_ARRAY);
    }
}
