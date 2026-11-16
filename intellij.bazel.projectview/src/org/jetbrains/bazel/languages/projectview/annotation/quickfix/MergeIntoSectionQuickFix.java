package org.jetbrains.bazel.languages.projectview.annotation.quickfix;

import consulo.application.dumb.DumbAware;
import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionKey;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementFactory;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Implementation of {@link org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem.QuickFix.MergeIntoSection} quick fix.
 */
// consulo: the ModCommand API (PsiUpdateModCommandAction) does not exist in Consulo; a classic intention action bound to the
// section (smart pointer), run in a write action
public class MergeIntoSectionQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement implements DumbAware {

    private final String myTargetSectionName;

    public MergeIntoSectionQuickFix(
        ProjectViewPsiSection section,
        ProjectViewSectionKey<?> targetSectionKey
    ) {
        super(section);
        myTargetSectionName = targetSectionKey.getName();
    }

    // consulo: upstream invoke(context: ActionContext, element: ProjectViewPsiSection, updater: ModPsiUpdater)
    @Override
    public void invoke(
        Project project,
        PsiFile file,
        @Nullable Editor editor,
        PsiElement startElement,
        PsiElement endElement
    ) {
        if (!(startElement instanceof ProjectViewPsiSection element)) {
            return;
        }
        if (!(element.getContainingFile() instanceof ProjectViewPsiFile projectViewFile)) {
            return;
        }
        ProjectViewElementFactory factory = new ProjectViewElementFactory(project);
        ProjectViewPsiSection target = projectViewFile.getSection(myTargetSectionName);
        if (target == null) {
            element.getKeyword().replace(factory.createSection(myTargetSectionName).getKeyword());
            return;
        }
        List<PsiElement> content = Utils.contentLines(element);
        if (!content.isEmpty()) {
            ProjectViewPsiSection source =
                factory.createSection(myTargetSectionName, content.stream().map(it -> it.getText().trim()).toList());
            PsiElement sourceColon = source.getColon();
            PsiElement first = sourceColon != null ? sourceColon.getNextSibling() : null;
            if (first == null) {
                return;
            }
            List<PsiElement> targetContent = Utils.contentLines(target);
            PsiElement targetColon = target.getColon();
            PsiElement anchor = !targetContent.isEmpty()
                ? targetContent.getLast()
                : targetColon != null ? targetColon : target.getKeyword();
            target.addRangeAfter(first, Utils.contentLines(source).getLast(), anchor);
        }
        Utils.deleteContent(element, content);
    }

    // consulo: upstream getFamilyName(); Consulo intention actions have no family name, the text is the family name of upstream
    @Override
    public LocalizeValue getText() {
        return BazelProjectViewLocalize.quickfixDeprecatedSectionReplacePresentation(myTargetSectionName);
    }
}
