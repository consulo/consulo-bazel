package org.jetbrains.bazel.languages.bazelrc.psi;

import consulo.language.psi.PsiElementVisitor;

public class BazelrcElementVisitor extends PsiElementVisitor {
    public void visitImport(BazelrcImport node) {
        visitElement(node);
    }

    public void visitLine(BazelrcLine node) {
        visitElement(node);
    }

    public void visitFlag(BazelrcFlag node) {
        visitElement(node);
    }
}
