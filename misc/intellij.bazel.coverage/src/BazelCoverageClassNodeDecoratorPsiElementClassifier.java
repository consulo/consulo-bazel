package org.jetbrains.bazel.run.coverage;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ProjectViewNodeDecorator;
import consulo.ui.ex.tree.PresentationData;
import jakarta.inject.Inject;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelCoverageClassNodeDecoratorPsiElementClassifier {
    boolean shouldShowCoverageForElement(PsiElement psiElement);

    // consulo: upstream companion object `ep`, served by the Consulo extension point
    static ExtensionPoint<BazelCoverageClassNodeDecoratorPsiElementClassifier> getEp() {
        return Application.get().getExtensionPoint(BazelCoverageClassNodeDecoratorPsiElementClassifier.class);
    }
}

// consulo: upstream internal top-level class of BazelCoverageClassNodeDecoratorPsiElementClassifier.kt
@ExtensionImpl
class BazelCoverageClassNodeDecorator implements ProjectViewNodeDecorator {
    private final Project myProject;

    @Inject
    public BazelCoverageClassNodeDecorator(Project project) {
        myProject = project;
    }

    @Override
    @RequiredReadAction
    public void decorate(ProjectViewNode node, PresentationData data) {
        // TODO consulo: CoverageOptionsProvider has no showInProjectView() option, coverage is always shown
        // if (!CoverageOptionsProvider.getInstance(project).showInProjectView()) return

        Object nodeValue = node.getValue();
        PsiElement psiElement;
        if (nodeValue instanceof SmartPsiElementPointer<?> pointer) {
            psiElement = pointer.getElement();
        }
        else if (nodeValue instanceof PsiElement element) {
            psiElement = element;
        }
        else {
            psiElement = null;
        }
        if (psiElement == null) {
            return;
        }

        if (BazelCoverageClassNodeDecoratorPsiElementClassifier.getEp()
            .noneMatchSafe(it -> it.shouldShowCoverageForElement(psiElement))) {
            return;
        }

        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile == null) {
            return;
        }
        CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(myProject);
        CoverageSuitesBundle suite = coverageDataManager.getCurrentSuitesBundle();
        if (suite == null) {
            return;
        }
        if (!(suite.getAnnotator(myProject) instanceof BazelCoverageAnnotator annotator)) {
            return;
        }
        String coverageInformationString = annotator.getFileCoverageInformationString(psiFile, suite, coverageDataManager);
        if (coverageInformationString != null) {
            data.setLocationString(coverageInformationString);
        }
    }
}
