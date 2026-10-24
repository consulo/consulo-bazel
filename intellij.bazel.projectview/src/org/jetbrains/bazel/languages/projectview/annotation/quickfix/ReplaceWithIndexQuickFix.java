package org.jetbrains.bazel.languages.projectview.annotation.quickfix;

import consulo.application.dumb.DumbAware;
import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionType;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementFactory;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.INDEX_ALL_FILES_IN_DIRECTORIES_KEY;
import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.INDEX_KEY;

/**
 * Implementation of {@link org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem.QuickFix.ReplaceWithIndex} quick fix.
 * <p>
 * Replaces `index_all_files_in_directories` and `index_additional_files_in_directories` with one `index` section.
 * `index_all_files_in_directories: true` becomes the single line `index: *`.
 * The fix keeps the patterns that {@code org.jetbrains.bazel.languages.projectview.index} derives from the deprecated sections.
 * An existing `index` section with items has priority, so the fix only removes the deprecated sections in that case.
 * An empty `index` section has no effect, so the fix replaces it.
 */
// consulo: the ModCommand API (PsiUpdateModCommandAction) does not exist in Consulo; a classic intention action bound to the
// section (smart pointer), run in a write action
public class ReplaceWithIndexQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement implements DumbAware {

    private final String myTargetSectionName = INDEX_KEY.getName();

    public ReplaceWithIndexQuickFix(
        ProjectViewPsiSection section
    ) {
        super(section);
    }

    // consulo: upstream invoke(context: ActionContext, element: ProjectViewPsiSection, updater: ModPsiUpdater)
    @Override
    public void invoke(
        Project project,
        PsiFile psiFile,
        @Nullable Editor editor,
        PsiElement startElement,
        PsiElement endElement
    ) {
        if (!(startElement instanceof ProjectViewPsiSection element)) {
            return;
        }
        if (!(element.getContainingFile() instanceof ProjectViewPsiFile file)) {
            return;
        }
        ProjectViewPsiSection indexAllFiles = file.getSection(INDEX_ALL_FILES_IN_DIRECTORIES_KEY.getName());
        ProjectViewPsiSection indexAdditionalFiles = file.getSection(INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY.getName());
        ProjectViewPsiSection target = file.getSection(myTargetSectionName);
        ProjectViewPsiSection emptyTarget =
            target != null && target.getItems().stream().noneMatch(it -> !it.getText().isBlank()) ? target : null;
        ProjectViewElementFactory factory = new ProjectViewElementFactory(project);
        ProjectViewPsiSection replacement;
        if (target != null && emptyTarget == null) {
            replacement = null;
        }
        else if (indexAllFiles != null && Boolean.TRUE.equals(ProjectViewSectionType.read(indexAllFiles, ProjectViewSectionType.BOOLEAN))) {
            replacement = factory.createSingleLineSection(myTargetSectionName, "*");
        }
        else {
            List<String> lines = indexAdditionalFiles != null
                ? Utils.contentLines(indexAdditionalFiles).stream().map(PsiElement::getText).toList()
                : null;
            replacement = lines != null && !lines.isEmpty() ? factory.createSection(myTargetSectionName, lines) : null;
        }
        if (replacement != null) {
            Objects.requireNonNull(element.getParent()).addBefore(replacement, element);
        }
        // consulo: Kotlin listOfNotNull(...).distinct()
        Set<ProjectViewPsiSection> sections = new LinkedHashSet<>();
        sections.add(element);
        if (indexAllFiles != null) {
            sections.add(indexAllFiles);
        }
        if (indexAdditionalFiles != null) {
            sections.add(indexAdditionalFiles);
        }
        if (emptyTarget != null) {
            sections.add(emptyTarget);
        }
        sections.forEach(it -> Utils.deleteContent(it));
    }

    // consulo: upstream getPresentation(context, element) returned Presentation.of(text)
    @Override
    public LocalizeValue getText() {
        return BazelProjectViewLocalize.quickfixDeprecatedSectionReplacePresentation(myTargetSectionName);
    }

    // consulo: Consulo intention actions have no family name; kept from upstream
    public LocalizeValue getFamilyName() {
        return BazelProjectViewLocalize.quickfixDeprecatedSectionReplaceDescription();
    }
}
