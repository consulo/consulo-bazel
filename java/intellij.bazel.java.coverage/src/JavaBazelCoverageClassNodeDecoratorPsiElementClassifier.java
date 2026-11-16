package org.jetbrains.bazel.java.coverage;

import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.run.coverage.BazelCoverageClassNodeDecoratorPsiElementClassifier;

@ExtensionImpl
public class JavaBazelCoverageClassNodeDecoratorPsiElementClassifier implements BazelCoverageClassNodeDecoratorPsiElementClassifier {
    @Override
    public boolean shouldShowCoverageForElement(PsiElement psiElement) {
        return psiElement instanceof PsiClass;
    }
}
