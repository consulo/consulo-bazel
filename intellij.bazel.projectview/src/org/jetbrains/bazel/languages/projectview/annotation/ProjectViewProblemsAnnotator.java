package org.jetbrains.bazel.languages.projectview.annotation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationBuilder;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.MergeIntoSectionQuickFix;
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.ReplaceWithIndexQuickFix;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jspecify.annotations.Nullable;

// consulo: Consulo registers annotators through AnnotatorFactory; the stateless annotator is its own factory
@ExtensionImpl
class ProjectViewProblemsAnnotator implements Annotator, AnnotatorFactory, DumbAware {
    public ProjectViewProblemsAnnotator() {
    }

    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (!(element instanceof ProjectViewPsiSection section)) {
            return;
        }
        ProjectViewChecker.Sink sink = problem -> {
            holder.newAnnotation(toHighlightSeverity(problem.severity()), problem.message())
                .apply(it -> rangeIfPresent(it, problem.element()))
                .apply(it -> withQuickFixesFrom(it, problem))
                .apply(it -> highlightingTypeFor(it, problem))
                .create();
        };
        ProjectViewChecker.run(section, sink);
    }

    private HighlightSeverity toHighlightSeverity(ProjectViewProblem.Severity receiver) {
        return switch (receiver) {
            case Warning -> HighlightSeverity.WARNING;
            case Error -> HighlightSeverity.ERROR;
            case WeakWarning -> HighlightSeverity.WEAK_WARNING;
        };
    }

    private AnnotationBuilder rangeIfPresent(AnnotationBuilder receiver, @Nullable PsiElement element) {
        if (element == null) {
            return receiver;
        }
        else {
            return receiver.range(element);
        }
    }

    private AnnotationBuilder withQuickFixesFrom(AnnotationBuilder receiver, ProjectViewProblem problem) {
        if (problem.quickFixes().isEmpty()) {
            return receiver;
        }
        AnnotationBuilder builder = receiver;
        for (ProjectViewProblem.QuickFix it : problem.quickFixes()) {
            IntentionAction fix = toIntentionAction(it, problem.element());
            if (fix != null) {
                builder = builder.withFix(fix);
            }
        }
        return builder;
    }

    // consulo: CommonIntentionAction (ModCommand API) -> classic IntentionAction
    private @Nullable IntentionAction toIntentionAction(ProjectViewProblem.QuickFix receiver, @Nullable PsiElement element) {
        ProjectViewPsiSection section = element == null ? null : PsiTreeUtil.getParentOfType(element, ProjectViewPsiSection.class, false);
        if (section == null) {
            return null;
        }
        return switch (receiver) {
            case ProjectViewProblem.QuickFix.MergeIntoSection mergeIntoSection ->
                new MergeIntoSectionQuickFix(section, mergeIntoSection.targetSectionKey());
            case ProjectViewProblem.QuickFix.ReplaceWithIndex replaceWithIndex -> new ReplaceWithIndexQuickFix(section);
        };
    }

    private AnnotationBuilder highlightingTypeFor(AnnotationBuilder receiver, ProjectViewProblem problem) {
        return switch (problem.type()) {
            case Message -> receiver;
            case Deprecation -> receiver.highlightType(ProblemHighlightType.LIKE_DEPRECATED);
        };
    }

    @Override
    public Language getLanguage() {
        return ProjectViewLanguage.INSTANCE;
    }

    @Override
    public @Nullable Annotator createAnnotator() {
        return this;
    }
}
