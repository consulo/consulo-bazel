package org.jetbrains.bazel.ui.gutters;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.action.PsiLocation;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

public class BazelRunLocation extends PsiLocation<PsiElement> {
    private final BuildTargetInfo myTarget;
    /**
     * If provided, this can be, for example, a test method in PSI, from which a test filter can be derived.
     */
    private final PsiLocation<?> myOriginalLocation;

    public BazelRunLocation(
        BuildTargetInfo target,
        PsiLocation<?> originalLocation
    ) {
        super(originalLocation.getProject(), null, new PsiBazelTarget(originalLocation.getProject(), target));
        myTarget = target;
        myOriginalLocation = originalLocation;
    }

    /**
     * No other information except the target itself is available, e.g., from Bazel target tool window.
     */
    @RequiredReadAction
    public BazelRunLocation(Project project, BuildTargetInfo target) {
        this(target, new PsiLocation<>(new PsiBazelTarget(project, target)));
    }

    public BuildTargetInfo getTarget() {
        return myTarget;
    }

    public PsiLocation<?> getOriginalLocation() {
        return myOriginalLocation;
    }
}
