package org.jetbrains.bazel.languages.bazelquery.psi;

import consulo.language.ast.ASTNode;

public class BazelQueryFlagVal extends BazelQueryBaseElement {
    public BazelQueryFlagVal(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(BazelQueryElementVisitor visitor) {
        visitor.visitFlagVal(this);
    }
}
