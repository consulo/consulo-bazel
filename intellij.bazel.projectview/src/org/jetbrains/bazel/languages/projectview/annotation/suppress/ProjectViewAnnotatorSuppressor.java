package org.jetbrains.bazel.languages.projectview.annotation.suppress;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.InspectionSuppressor;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewSuppression;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Gives the suppress actions of project view warnings to Inspect Code.
 * <p>
 * Inspect Code runs the annotator through the platform annotator tool, {@code HighlightVisitorBasedInspection}.
 * That tool has no suppress actions of its own, so the Inspect Code view asks the inspection suppressors of the language for them.
 * The editor does not use this class, because the submenu of each fix holds the suppress actions there.
 * {@link #isSuppressedFor} is always false, because the checker already leaves out the suppressed warnings.
 */
@ExtensionImpl
public class ProjectViewAnnotatorSuppressor implements InspectionSuppressor {
    // consulo: IntelliJ HighlightVisitorBasedInspection.SHORT_NAME; the short name of the Consulo annotator tool
    // (AnnotatorBasedInspectionImpl, not public API)
    private static final String ANNOTATOR_SHORT_NAME = "Annotator";

    public ProjectViewAnnotatorSuppressor() {
    }

    @Override
    public boolean isSuppressedFor(PsiElement element, String toolId) {
        return false;
    }

    @Override
    public SuppressQuickFix[] getSuppressActions(@Nullable PsiElement element, String toolId) {
        if (ANNOTATOR_SHORT_NAME.equals(toolId)) {
            return Arrays.stream(ProjectViewSuppressScope.values())
                .map(ProjectViewBatchSuppressFix::new)
                .toArray(SuppressQuickFix[]::new);
        }
        else {
            return SuppressQuickFix.EMPTY_ARRAY;
        }
    }

    @Override
    public Language getLanguage() {
        return ProjectViewLanguage.INSTANCE;
    }
}

class ProjectViewBatchSuppressFix implements SuppressQuickFix {
    private final ProjectViewSuppressScope myScope;

    ProjectViewBatchSuppressFix(ProjectViewSuppressScope scope) {
        myScope = scope;
    }

    // consulo: upstream getFamilyName() = name; Consulo intention actions have no family name
    public LocalizeValue getFamilyName() {
        return getName();
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeKey.of(BazelProjectViewLocalize.ID, myScope.getBatchKey()).getValue();
    }

    @Override
    public boolean isAvailable(
        Project project,
        PsiElement context
    ) {
        return myScope.containerOf(context) != null && suppressibleProblem(context) != null;
    }

    // consulo: upstream isSuppressAll() = false, Consulo SuppressQuickFix has no isSuppressAll

    // consulo: upstream runs the ModCommand with ModCommandExecutor outside a write action; the edit runs in the write action here
    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        if (element == null) {
            return;
        }
        ProjectViewProblem problem = suppressibleProblem(element);
        if (problem == null) {
            return;
        }
        PsiElement container = myScope.containerOf(element);
        if (container == null) {
            return;
        }
        Runnable command = ProjectViewSuppressionCommand.addProjectViewSuppressionCommand(container, problem.id());
        if (command != null) {
            command.run();
        }
    }

    private static @Nullable ProjectViewProblem suppressibleProblem(PsiElement receiver) {
        // consulo: Kotlin parents(withSelf = true).firstOrNull { it.parent is PsiFile }
        PsiElement topLevel = receiver;
        while (topLevel != null && !(topLevel.getParent() instanceof PsiFile)) {
            topLevel = topLevel.getParent();
        }
        if (topLevel == null) {
            return null;
        }
        return ProjectViewChecker.check(ProjectViewChecker.Main, topLevel)
            .filter(it -> {
                PsiElement element = it.element();
                return ProjectViewSuppression.isSuppressible(it)
                    && element != null
                    && Objects.equals(element.getTextRange(), receiver.getTextRange());
            })
            .findFirst()
            .orElse(null);
    }
}
