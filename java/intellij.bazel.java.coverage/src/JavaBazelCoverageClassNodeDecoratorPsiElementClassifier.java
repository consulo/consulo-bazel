package org.jetbrains.bazel.java.coverage;

import com.intellij.java.language.psi.PsiClass;
import consulo.language.psi.PsiElement;

// TODO consulo: org.jetbrains.bazel.run.coverage.BazelCoverageClassNodeDecoratorPsiElementClassifier (misc/intellij.bazel.coverage)
//  is not ported yet; the class does not implement it and is not registered
//  (upstream: <bazelCoverageClassNodeDecoratorPsiElementClassifier>)
public class JavaBazelCoverageClassNodeDecoratorPsiElementClassifier /* : BazelCoverageClassNodeDecoratorPsiElementClassifier */ {
    // @Override
    public boolean shouldShowCoverageForElement(PsiElement psiElement) {
        return psiElement instanceof PsiClass;
    }
}
