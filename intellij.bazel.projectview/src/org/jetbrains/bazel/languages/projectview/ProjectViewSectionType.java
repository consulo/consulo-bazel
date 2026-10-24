package org.jetbrains.bazel.languages.projectview;

import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.language.editor.completion.CompletionProvider;
import org.jetbrains.bazel.commons.ExcludableValue;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.bazelrc.flags.Flag;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider;
import org.jetbrains.bazel.languages.projectview.completion.PathCompletionProviders.FiletypeCompletionProvider;
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider;
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import static org.jetbrains.bazel.config.project.getRootDir;

// consulo: the upstream typealias ProjectViewValueChecker is ProjectViewChecker<String>
public interface ProjectViewSectionType<T> {

    @Nullable ProjectViewChecker<String> getValueChecker();

    @Nullable CompletionProvider getCompletionProvider();

    @Nullable T readFrom(List<String> values);

    interface Scalar<T> extends ProjectViewSectionType<T> {

        @Nullable T readFrom(String value);

        @Override
        default @Nullable T readFrom(List<String> values) {
            // consulo: Kotlin singleOrNull()
            return values.size() == 1 ? readFrom(values.get(0)) : null;
        }
    }

    // consulo: upstream companion object members; `int`, `boolean`, `label` and `enum` are renamed (Java keywords)

    Scalar<Integer> INT = scalar(ProjectViewSectionType::toIntOrNull);
    Scalar<Boolean> BOOLEAN = variants(ProjectViewSectionType::toBooleanStrictOrNull, List.of("true", "false"));
    Scalar<Label> LABEL = scalar(Label::parseOrNull, scalarChecker(Label::parseOrNull), new TargetCompletionProvider());

    static Scalar<String> string() {
        return string(null);
    }

    static Scalar<String> string(
        @Nullable CompletionProvider completionProvider
    ) {
        Function<String, @Nullable String> read = it -> it;
        return scalar(
            read,
            scalarChecker(read),
            completionProvider
        );
    }

    static Scalar<Path> path() {
        return path(false);
    }

    static Scalar<Path> path(boolean existing) {
        return path(existing, null);
    }

    static Scalar<Path> path(
        boolean existing,
        @Nullable CompletionProvider completionProvider
    ) {
        return scalar(
            ProjectViewSectionType::toNioPathOrNull,
            pathChecker(ProjectViewSectionType::toNioPathOrNull, existing),
            completionProvider
        );
    }

    static Scalar<Path> file(String extension) {
        return path(true, new FiletypeCompletionProvider(extension));
    }

    static <T extends Enum<T>> Scalar<T> enumType(Class<T> enumClass) {
        return enumType(List.of(enumClass.getEnumConstants()));
    }

    static Scalar<String> flag(String... commands) {
        return scalar(
            it -> it,
            flagChecker(commands),
            new FlagCompletionProvider(commands)
        );
    }

    private static <T extends Enum<T>> Scalar<T> enumType(List<T> entries) {
        return variants(
            value -> entries.stream().filter(it -> it.name().equalsIgnoreCase(value)).findFirst().orElse(null),
            entries.stream().map(it -> it.name().toLowerCase(Locale.ROOT)).toList()
        );
    }

    private static <T> Scalar<T> variants(
        Function<String, @Nullable T> read,
        List<String> variants
    ) {
        return scalar(
            read,
            variantsChecker(read, variants),
            new SimpleCompletionProvider(variants)
        );
    }

    private static <T> Scalar<T> scalar(Function<String, @Nullable T> read) {
        return scalar(read, scalarChecker(read), null);
    }

    private static <T> Scalar<T> scalar(
        Function<String, @Nullable T> read,
        ProjectViewChecker<String> checker,
        @Nullable CompletionProvider completionProvider
    ) {
        return new Scalar<T>() {

            @Override
            public ProjectViewChecker<String> getValueChecker() {
                return checker;
            }

            @Override
            public @Nullable CompletionProvider getCompletionProvider() {
                return completionProvider;
            }

            @Override
            public @Nullable T readFrom(String value) {
                return read.apply(value);
            }
        };
    }

    // consulo: upstream Kotlin top-level (extension) functions of this file

    static boolean isScalar(ProjectViewSectionType<?> receiver) {
        return receiver instanceof ProjectViewSectionType.Scalar<?>;
    }

    static <T> @Nullable T read(ProjectViewPsiSection receiver, ProjectViewSectionType<T> type) {
        return type.readFrom(receiver.getItems().stream().map(it -> it.getText().trim()).toList());
    }

    static <T> ProjectViewSectionType.Scalar<ExcludableValue<T>> excludable(ProjectViewSectionType.Scalar<T> receiver) {
        ProjectViewSectionType.Scalar<T> type = receiver;
        return new ProjectViewSectionType.Scalar<ExcludableValue<T>>() {
            private final ProjectViewChecker<String> myValueChecker = (project, value, sink) -> {
                ProjectViewChecker<String> valueChecker = type.getValueChecker();
                if (valueChecker != null) {
                    valueChecker.check(project, removePrefix(value, "-"), sink);
                }
            };

            @Override
            public ProjectViewChecker<String> getValueChecker() {
                return myValueChecker;
            }

            @Override
            public @Nullable CompletionProvider getCompletionProvider() {
                return type.getCompletionProvider();
            }

            @Override
            public @Nullable ExcludableValue<T> readFrom(String value) {
                if (value.startsWith("-")) {
                    T result = type.readFrom(removePrefix(value, "-"));
                    return result != null ? ExcludableValue.excluded(result) : null;
                }
                else {
                    T result = type.readFrom(value);
                    return result != null ? ExcludableValue.included(result) : null;
                }
            }
        };
    }

    static <T> ProjectViewSectionType<List<T>> list(ProjectViewSectionType.Scalar<T> receiver) {
        ProjectViewSectionType.Scalar<T> itemType = receiver;
        return new ProjectViewSectionType<List<T>>() {

            private final @Nullable CompletionProvider myCompletionProvider = itemType.getCompletionProvider();

            private final @Nullable ProjectViewChecker<String> myValueChecker = itemType.getValueChecker();

            @Override
            public @Nullable CompletionProvider getCompletionProvider() {
                return myCompletionProvider;
            }

            @Override
            public @Nullable ProjectViewChecker<String> getValueChecker() {
                return myValueChecker;
            }

            @Override
            public List<T> readFrom(List<String> values) {
                return values.stream().map(it -> itemType.readFrom(it)).filter(Objects::nonNull).toList();
            }
        };
    }

    private static <T> ProjectViewChecker<String> scalarChecker(Function<String, @Nullable T> read) {
        return (project, subject, sink) -> {
            if (read.apply(subject) == null) {
                ProjectViewProblem problem = new ProjectViewProblem(
                    BazelProjectViewLocalize.annotatorCannotParseValue(subject),
                    ProjectViewProblem.Severity.Error,
                    ProjectViewProblem.Type.Message,
                    List.of(),
                    null
                );
                sink.report(problem);
            }
        };
    }

    private static ProjectViewChecker<String> pathChecker(Function<String, @Nullable Path> read, boolean existing) {
        return (project, subject, sink) -> {
            Path path = read.apply(subject);
            if (path == null) {
                ProjectViewProblem problem = new ProjectViewProblem(
                    BazelProjectViewLocalize.annotatorInvalidPathError(),
                    ProjectViewProblem.Severity.Error,
                    ProjectViewProblem.Type.Message,
                    List.of(),
                    null
                );
                sink.report(problem);
                return;
            }

            if (!existing) {
                return;
            }

            Path resolvedPath = path.isAbsolute()
                ? path
                : getRootDir(project).toNioPath().resolve(path);
            if (!Files.exists(resolvedPath)) {
                ProjectViewProblem problem = new ProjectViewProblem(
                    BazelProjectViewLocalize.annotatorPathNotExistsError(),
                    ProjectViewProblem.Severity.Warning,
                    ProjectViewProblem.Type.Message,
                    List.of(),
                    null
                );
                sink.report(problem);
            }
        };
    }

    private static <T> ProjectViewChecker<String> variantsChecker(Function<String, @Nullable T> read, List<String> variants) {
        return (project, subject, sink) -> {
            if (read.apply(subject) == null) {
                ProjectViewProblem problem = new ProjectViewProblem(
                    BazelProjectViewLocalize.annotatorUnknownVariantError(subject, String.join(", ", variants)),
                    ProjectViewProblem.Severity.Error,
                    ProjectViewProblem.Type.Message,
                    List.of(),
                    null
                );
                sink.report(problem);
            }
        };
    }

    private static ProjectViewChecker<String> flagChecker(String[] commands) {
        return (project, subject, sink) -> {
            int equalsIndex = subject.indexOf('=');
            Flag flag = Flag.byName(equalsIndex >= 0 ? subject.substring(0, equalsIndex) : subject);
            if (flag == null) {
                ProjectViewProblem problem = new ProjectViewProblem(
                    BazelProjectViewLocalize.annotatorUnknownFlagError(subject),
                    ProjectViewProblem.Severity.Warning,
                    ProjectViewProblem.Type.Message,
                    List.of(),
                    null
                );
                sink.report(problem);
                return;
            }
            if (Arrays.stream(commands).anyMatch(it -> !Arrays.asList(flag.getOption().commands()).contains(it))) {
                ProjectViewProblem problem = new ProjectViewProblem(
                    BazelProjectViewLocalize.annotatorFlagNotAllowedHereError(subject, Arrays.toString(commands)),
                    ProjectViewProblem.Severity.Warning,
                    ProjectViewProblem.Type.Message,
                    List.of(),
                    null
                );
                sink.report(problem);
            }
        };
    }

    // consulo: Kotlin stdlib/IntelliJ helpers used by the upstream code

    // Kotlin String.toIntOrNull()
    private static @Nullable Integer toIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    // Kotlin String.toBooleanStrictOrNull()
    private static @Nullable Boolean toBooleanStrictOrNull(String value) {
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        return null;
    }

    // IntelliJ String.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(String value) {
        try {
            return Path.of(value);
        }
        catch (InvalidPathException e) {
            return null;
        }
    }

    // Kotlin String.removePrefix()
    private static String removePrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
