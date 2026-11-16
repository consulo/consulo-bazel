package org.jetbrains.bazel.languages.projectview.checker;

import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.projectview.ProjectViewSection;
import org.jetbrains.bazel.languages.projectview.imports.Import;
import org.jetbrains.bazel.languages.projectview.imports.ImportFactory;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

// consulo: Kotlin Sequence<ProjectViewProblem> is a java.util.stream.Stream
@FunctionalInterface
public interface ProjectViewChecker<T> {

    Stream<ProjectViewProblem> check(Project project, T subject);

    // companion object

    ProjectViewChecker<PsiElement> Main = (project, subject) -> {
        if (subject instanceof ProjectViewPsiSection section) {
            return ProjectViewSuppression.withSuppressions(
                ProjectViewCheckers.SECTION_CHECKERS.stream().flatMap(it -> it.check(project, section))
            );
        }
        else if (subject instanceof ProjectViewPsiImportBase importBase) {
            return ProjectViewSuppression.withSuppressions(
                ProjectViewCheckers.IMPORT_CHECKERS.stream().flatMap(it -> it.check(project, importBase))
            );
        }
        else {
            return Stream.empty();
        }
    };

    /**
     * The body of {@link #projectViewChecker}: it reports the problems to {@code sink} (Kotlin yield).
     */
    // consulo: Kotlin `suspend SequenceScope<ProjectViewProblem>.(project, subject) -> Unit`
    @FunctionalInterface
    interface SequenceBlock<T> {
        void run(Project project, T subject, Consumer<ProjectViewProblem> sink);
    }

    // consulo: upstream internal top-level functions of ProjectViewChecker.kt

    // consulo: the block runs when the stream is consumed, like the Kotlin sequence {} builder
    static <T> ProjectViewChecker<T> projectViewChecker(SequenceBlock<T> block) {
        return (project, subject) -> Stream.of(subject).flatMap(it -> {
            List<ProjectViewProblem> problems = new ArrayList<>();
            block.run(project, it, problems::add);
            return problems.stream();
        });
    }

    static <T extends PsiElement> Stream<ProjectViewProblem> check(ProjectViewChecker<T> receiver, T element) {
        return receiver.check(element.getProject(), element);
    }

    static Stream<ProjectViewProblem> checkChildren(ProjectViewChecker<PsiElement> receiver, ProjectViewPsiFile file) {
        return Arrays.stream(file.getChildren())
            .flatMap(it -> check(receiver, it));
    }

    static Stream<ProjectViewProblem> withSourcePath(Stream<ProjectViewProblem> receiver, @Nullable Path sourcePath) {
        return receiver.map(it -> it.sourcePath() == null ? it.withSourcePath(sourcePath) : it);
    }
}

// consulo: upstream private top-level declarations of ProjectViewChecker.kt and the private companion properties
// (an interface cannot have private fields)
final class ProjectViewCheckers {
    private ProjectViewCheckers() {
    }

    private static final ProjectViewChecker<ProjectViewPsiImportBase> UNRESOLVED_IMPORT_CHECKER =
        ProjectViewChecker.projectViewChecker((project, subject, sink) -> {
            VirtualFile root = BazelProjectInfoService.getBazelProjectInfo(project).getProjectRootDir();
            if (root == null) {
                return;
            }
            Path rootPath = root.toNioPath();
            Import anImport = ImportFactory.from(rootPath, subject);
            LocalizeValue message;
            if (anImport instanceof Import.OutsideOfProjectRoot outside) {
                message = BazelProjectViewLocalize.annotatorPathOutsideOfProjectRootError(outside.path(), rootPath);
            }
            else if (anImport instanceof Import.Unresolved unresolved && unresolved.isRequired()) {
                message = BazelProjectViewLocalize.annotatorImportUnresolvedError(unresolved.text());
            }
            else {
                return;
            }
            PsiElement importPath = subject.getImportPath();
            sink.accept(
                new ProjectViewProblem(
                    ProjectViewProblem.Id.UnresolvedImport,
                    message,
                    ProjectViewProblem.Severity.Error,
                    importPath != null ? importPath : subject.getFirstChild()
                )
            );
        });

    private static final ProjectViewChecker<ProjectViewPsiSection> UNSUPPORTED_SECTION_CHECKER =
        ProjectViewChecker.projectViewChecker((project, subject, sink) -> {
            if (ProjectViewSection.findByPsi(subject) == null) {
                ProjectViewProblem problem = new ProjectViewProblem(
                    ProjectViewProblem.Id.UnsupportedSection,
                    BazelProjectViewLocalize.annotatorUnsupportedSectionWarning(ProjectViewPsiSection.sectionName(subject)),
                    ProjectViewProblem.Severity.Warning,
                    subject.getKeyword()
                );
                sink.accept(problem);
            }
        });

    private static final ProjectViewChecker<ProjectViewPsiSection> TYPE_VALUE_CHECKER = (project, subject) -> {
        ProjectViewSection<?> section = ProjectViewSection.findByPsi(subject);
        ProjectViewChecker<String> valueChecker = section != null ? section.getType().getValueChecker() : null;
        if (valueChecker == null) {
            return Stream.empty();
        }
        return subject
            .getItems()
            .stream()
            .flatMap(item -> withElement(valueChecker.check(project, item.getText().trim()), item));
    };

    private static final ProjectViewChecker<ProjectViewPsiSection> SECTION_DEPRECATION_CHECKER =
        ProjectViewChecker.projectViewChecker((project, subject, sink) -> {
            ProjectViewSection<?> section = ProjectViewSection.findByPsi(subject);
            ProjectViewSection.Deprecation deprecation = section != null ? section.getDeprecation() : null;
            if (deprecation == null) {
                return;
            }
            sink.accept(
                new ProjectViewProblem(
                    ProjectViewProblem.Id.Deprecation,
                    deprecation.getMessage().apply(ProjectViewPsiSection.sectionName(subject)),
                    toSeverity(deprecation.getLevel()),
                    deprecation.getQuickFixes(),
                    subject.getKeyword()
                )
            );
        });

    // consulo: companion `sectionCheckers` / `importCheckers`, declared after the checkers they refer to
    // (Java static initialization order)
    static final List<ProjectViewChecker<ProjectViewPsiSection>> SECTION_CHECKERS = List.of(
        UNSUPPORTED_SECTION_CHECKER,
        TYPE_VALUE_CHECKER,
        SECTION_DEPRECATION_CHECKER
    );

    static final List<ProjectViewChecker<ProjectViewPsiImportBase>> IMPORT_CHECKERS = List.of(UNRESOLVED_IMPORT_CHECKER);

    private static ProjectViewProblem.Severity toSeverity(ProjectViewSection.Deprecation.Level receiver) {
        return switch (receiver) {
            case Warning -> ProjectViewProblem.Severity.WeakWarning;
            case Error -> ProjectViewProblem.Severity.Error;
        };
    }

    private static Stream<ProjectViewProblem> withElement(Stream<ProjectViewProblem> receiver, PsiElement element) {
        return receiver.map(it -> it.element() == null ? it.withElement(element) : it);
    }
}
