package org.jetbrains.bazel.languages.projectview.checker;

import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.ProjectViewSection;
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionType;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;

import java.util.List;

// consulo: the upstream typealiases are spelled out:
// ProjectViewPsiSectionChecker = ProjectViewChecker<ProjectViewPsiSection>, ProjectViewValueChecker = ProjectViewChecker<String>

@FunctionalInterface
public interface ProjectViewChecker<T> {

    void check(Project project, T subject, Sink sink);

    @FunctionalInterface
    interface Sink {
        void report(ProjectViewProblem problem);
    }

    // companion object

    static void run(ProjectViewPsiSection section, Sink sink) {
        for (ProjectViewChecker<ProjectViewPsiSection> it : ProjectViewCheckers.CHECKERS) {
            it.check(section.getProject(), section, sink);
        }
    }
}

// consulo: upstream private top-level declarations of ProjectViewChecker.kt and the private companion property `checkers`
// (an interface cannot have private fields)
final class ProjectViewCheckers {
    private ProjectViewCheckers() {
    }

    private static final ProjectViewChecker<ProjectViewPsiSection> UNSUPPORTED_SECTION_CHECKER = (project, subject, sink) -> {
        ProjectViewSection<?> section = ProjectViewSection.findByPsi(subject);
        if (section == null) {
            ProjectViewProblem problem = new ProjectViewProblem(
                BazelProjectViewLocalize.annotatorUnsupportedSectionWarning(),
                ProjectViewProblem.Severity.Warning,
                ProjectViewProblem.Type.Message,
                List.of(),
                subject.getKeyword()
            );
            sink.report(problem);
            return;
        }
    };

    private static final ProjectViewChecker<ProjectViewPsiSection> TYPE_VALUE_CHECKER = (project, subject, sink) -> {
        ProjectViewSection<?> section = ProjectViewSection.findByPsi(subject);
        if (section == null) {
            return;
        }
        ProjectViewSectionType<?> type = section.getType();
        for (ProjectViewPsiSectionItem it : subject.getItems()) {
            ProjectViewChecker<String> valueChecker = type.getValueChecker();
            if (valueChecker != null) {
                valueChecker.check(project, it.getText().trim(), withElement(sink, it));
            }
        }
    };

    private static final ProjectViewChecker<ProjectViewPsiSection> SECTION_DEPRECATION_CHECKER = (project, subject, sink) -> {
        ProjectViewSection<?> section = ProjectViewSection.findByPsi(subject);
        if (section == null) {
            return;
        }
        ProjectViewSection.Deprecation deprecation = section.getDeprecation();
        if (deprecation == null) {
            return;
        }
        sink.report(
            new ProjectViewProblem(
                deprecation.getMessage(),
                toSeverity(deprecation.getLevel()),
                ProjectViewProblem.Type.Deprecation,
                deprecation.getQuickFixes(),
                subject.getKeyword()
            )
        );
    };

    // consulo: companion `checkers`, declared after the checkers it refers to (Java static initialization order)
    static final List<ProjectViewChecker<ProjectViewPsiSection>> CHECKERS = List.of(
        UNSUPPORTED_SECTION_CHECKER,
        TYPE_VALUE_CHECKER,
        SECTION_DEPRECATION_CHECKER
    );

    private static ProjectViewProblem.Severity toSeverity(ProjectViewSection.Deprecation.Level receiver) {
        return switch (receiver) {
            case Warning -> ProjectViewProblem.Severity.WeakWarning;
            case Error -> ProjectViewProblem.Severity.Error;
        };
    }

    private static ProjectViewChecker.Sink withElement(ProjectViewChecker.Sink receiver, PsiElement element) {
        ProjectViewChecker.Sink delegate = receiver;
        return it -> {
            ProjectViewProblem problem = it.element() == null
                ? new ProjectViewProblem(it.message(), it.severity(), it.type(), it.quickFixes(), element)
                : it;
            delegate.report(problem);
        };
    }
}
