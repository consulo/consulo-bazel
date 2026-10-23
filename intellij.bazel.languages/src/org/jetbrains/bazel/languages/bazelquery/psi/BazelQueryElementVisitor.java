package org.jetbrains.bazel.languages.bazelquery.psi;

import consulo.language.psi.PsiElementVisitor;

public class BazelQueryElementVisitor extends PsiElementVisitor {
    public void visitCommand(BazelQueryCommand node) {
        visitElement(node);
    }

    public void visitWord(BazelQueryWord node) {
        visitElement(node);
    }

    public void visitFlag(BazelQueryFlag node) {
        visitElement(node);
    }

    public void visitFlagVal(BazelQueryFlagVal node) {
        visitElement(node);
    }

    public void visitInteger(BazelQueryInteger node) {
        visitElement(node);
    }
}
