package org.jetbrains.bazel.languages.bazelquery.psi;

import consulo.language.ast.ASTNode;

public class BazelQueryInteger extends BazelQueryBaseElement {
    public BazelQueryInteger(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(BazelQueryElementVisitor visitor) {
        visitor.visitInteger(this);
    }
}
