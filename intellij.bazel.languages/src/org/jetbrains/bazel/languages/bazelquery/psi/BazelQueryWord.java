package org.jetbrains.bazel.languages.bazelquery.psi;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets;

public class BazelQueryWord extends BazelQueryBaseElement {
    public BazelQueryWord(ASTNode node) {
        super(node);
        PsiElement word = this.findChildByType(BazelQueryTokenSets.WORDS);
        myWord = word != null ? word.getText() : "";
    }

    @Override
    protected void acceptVisitor(BazelQueryElementVisitor visitor) {
        visitor.visitWord(this);
    }

    private final String myWord;

    public String getWord() {
        return myWord;
    }
}
