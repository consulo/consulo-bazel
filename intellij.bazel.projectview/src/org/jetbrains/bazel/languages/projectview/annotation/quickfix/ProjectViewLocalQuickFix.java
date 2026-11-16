package org.jetbrains.bazel.languages.projectview.annotation.quickfix;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.FileModifier;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionWrapper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.annotation.suppress.ProjectViewSuppressActions.ProjectViewActionWithSuppressOptions;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A quick fix of a project view problem for the editor and for Inspect Code.
 * <p>
 * Inspect Code uses a fix of an annotation only when the fix is a {@link LocalQuickFix}.
 * {@code ModCommandAction.asIntention} returns an intention that is not a {@link LocalQuickFix}, so Inspect Code would show no fix.
 * This class is an {@link IntentionAction} for the editor and a {@link LocalQuickFix} for Inspect Code. It delegates both to the
 * intention.
 */
// consulo: the ModCommand API does not exist in Consulo; the action is a classic IntentionAction. Consulo has no intention preview
// (generatePreview) and no asIntention()/asModCommandAction(); the wrapped fixes are DumbAware (upstream isDumbAware delegates)
public class ProjectViewLocalQuickFix implements ProjectViewActionWithSuppressOptions, LocalQuickFix, DumbAware {

    private final List<? extends IntentionAction> mySuppressActions;

    private final IntentionAction myFix;

    private final IntentionWrapper myBatchFix;

    public ProjectViewLocalQuickFix(
        IntentionAction action,
        List<? extends IntentionAction> suppressActions
    ) {
        mySuppressActions = suppressActions;
        myFix = action;
        myBatchFix = new IntentionWrapper(myFix);
    }

    // consulo: overload for the upstream default argument (suppressActions = emptyList())
    public ProjectViewLocalQuickFix(IntentionAction action) {
        this(action, List.of());
    }

    @Override
    public List<? extends IntentionAction> getSuppressActions() {
        return mySuppressActions;
    }

    @Override
    public LocalizeValue getText() {
        return myFix.getText();
    }

    // consulo: upstream getFamilyName(); the Consulo LocalQuickFix name
    @Override
    public LocalizeValue getName() {
        return myFix.getText();
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile psiFile) {
        return myFix.isAvailable(project, editor, psiFile);
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile psiFile) {
        myFix.invoke(project, editor, psiFile);
    }

    @Override
    public boolean startInWriteAction() {
        return myFix.startInWriteAction();
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        myBatchFix.applyFix(project, descriptor);
    }

    @Override
    public @Nullable PsiElement getElementToMakeWritable(PsiFile currentFile) {
        return myFix.getElementToMakeWritable(currentFile);
    }

    @Override
    public @Nullable FileModifier getFileModifierForPreview(PsiFile target) {
        return myFix.getFileModifierForPreview(target);
    }
}
