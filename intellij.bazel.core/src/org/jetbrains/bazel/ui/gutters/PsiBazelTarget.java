package org.jetbrains.bazel.ui.gutters;

import consulo.language.impl.psi.FakePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

/**
 * A fake PSI element for cases where we want to get executor actions from Bazel target tool window, for example.
 */
public class PsiBazelTarget extends FakePsiElement {
    private final Project myProject;
    private final BuildTarget myTarget;

    public PsiBazelTarget(Project project, BuildTarget target) {
        myProject = project;
        myTarget = target;
    }

    public BuildTarget getTarget() {
        return myTarget;
    }

    @Override
    public @Nullable PsiElement getParent() {
        return null;
    }

    @Override
    public @Nullable PsiFile getContainingFile() {
        return null;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public PsiManager getManager() {
        return PsiManager.getInstance(myProject);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public @Nullable String getText() {
        return super.getText();
    }
}
