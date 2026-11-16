package org.jetbrains.bazel.languages.projectview.annotation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationBuilder;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.annotation.SuppressableProblemGroup;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SuppressIntentionAction;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.ProjectViewLocalQuickFix;
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.Utils;
import org.jetbrains.bazel.languages.projectview.annotation.suppress.ProjectViewSuppressActions;
import org.jetbrains.bazel.languages.projectview.annotation.suppress.ProjectViewSuppressActions.ProjectViewSuppressOptionsAction;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewSuppression;
import org.jspecify.annotations.Nullable;

import java.util.List;

// consulo: Consulo registers annotators through AnnotatorFactory; the stateless annotator is its own factory
@ExtensionImpl
class ProjectViewProblemsAnnotator implements Annotator, AnnotatorFactory, DumbAware {
    public ProjectViewProblemsAnnotator() {
    }

    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        ProjectViewChecker.check(ProjectViewChecker.Main, element).forEach(problem -> {
            holder.newAnnotation(toHighlightSeverity(problem.severity()), problem.message())
                .apply(it -> rangeIfPresent(it, problem.element()))
                .apply(it -> withQuickFixesFrom(it, problem))
                .apply(it -> highlightingTypeFor(it, problem))
                .create();
        });
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
        List<SuppressIntentionAction> suppressActions = ProjectViewSuppression.isSuppressible(problem)
            ? ProjectViewSuppressActions.suppressActions(problem.id())
            : List.of();
        List<IntentionAction> fixesActions;
        if (problem.quickFixes().isEmpty() && !suppressActions.isEmpty()) {
            fixesActions = List.of(new ProjectViewSuppressOptionsAction(problem.id(), suppressActions));
        }
        else {
            fixesActions = Utils.quickFixActions(problem)
                .stream()
                .<IntentionAction>map(it -> new ProjectViewLocalQuickFix(it, suppressActions))
                .toList();
        }
        AnnotationBuilder builder = withSuppressOptions(receiver, suppressActions);
        for (IntentionAction entry : fixesActions) {
            builder = universalIfQuickFix(builder.newFix(entry), entry)
                .registerFix();
        }
        return builder;
    }

    /**
     * The display name of the key is the title of the submenu that holds the suppress actions.
     * An annotator cannot give a fix its own title, so without this key the title is the name of the annotator tool, "Annotator".
     */
    // consulo: upstream sets the key ProjectViewActionWithSuppressOptions.KEY on each fix, and the platform takes the options of the
    // fix (IntentionActionWithOptions) as its submenu. A Consulo annotation fix takes its submenu from the suppress actions of a
    // SuppressableProblemGroup of the annotation, and only with the default annotator key, so the problem group gives them here
    private AnnotationBuilder withSuppressOptions(AnnotationBuilder receiver, List<SuppressIntentionAction> suppressActions) {
        if (suppressActions.isEmpty()) {
            return receiver;
        }
        SuppressIntentionAction[] actions = suppressActions.toArray(SuppressIntentionAction[]::new);
        return receiver.problemGroup(new SuppressableProblemGroup() {
            @Override
            public SuppressIntentionAction[] getSuppressActions(@Nullable PsiElement element) {
                return actions;
            }

            @Override
            public @Nullable String getProblemName() {
                return null;
            }
        });
    }

    private AnnotationBuilder.FixBuilder universalIfQuickFix(AnnotationBuilder.FixBuilder receiver, IntentionAction entry) {
        if (entry instanceof LocalQuickFix) {
            return receiver.universal();
        }
        else {
            return receiver;
        }
    }

    private AnnotationBuilder highlightingTypeFor(AnnotationBuilder receiver, ProjectViewProblem problem) {
        if (problem.id().equals(ProjectViewProblem.Id.Deprecation)) {
            return receiver.highlightType(ProblemHighlightType.LIKE_DEPRECATED);
        }
        else {
            return receiver;
        }
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
