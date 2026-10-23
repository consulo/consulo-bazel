package org.jetbrains.bazel.languages.projectview;

import org.jetbrains.bazel.commons.ExcludableValue;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

// consulo: upstream Kotlin top-level declarations; extension properties ProjectView.x become static getX(ProjectView)
public final class ProjectViewTargets {
    private ProjectViewTargets() {
    }

    public static final ProjectViewSectionKey<List<ExcludableValue<Path>>> DIRECTORIES_KEY =
        new ProjectViewSectionKey<>("directories", List.of());

    public static List<ExcludableValue<Path>> getDirectories(ProjectView receiver) {
        // Default to workspace root if no directories specified
        List<ExcludableValue<Path>> directories = receiver.getSection(DIRECTORIES_KEY);
        return !directories.isEmpty() ? directories : List.of(ExcludableValue.included(Path.of(".")));
    }

    public static final ProjectViewSectionKey<List<ExcludableValue<Label>>> TARGETS_KEY = new ProjectViewSectionKey<>("targets", List.of());

    public static List<ExcludableValue<Label>> getTargets(ProjectView receiver) {
        if (ProjectViewExtensions.getDeriveTargetsFromDirectories(receiver)) {
            return createTargetsFromDirectories(receiver.getSection(TARGETS_KEY), getDirectories(receiver));
        }
        else {
            return receiver.getSection(TARGETS_KEY);
        }
    }

    private static List<ExcludableValue<Label>> createTargetsFromDirectories(
        List<ExcludableValue<Label>> targets,
        List<ExcludableValue<Path>> dirs
    ) {
        Predicate<List<? extends ExcludableValue<?>>> hasEmptyIncludedAndEmptyExcluded =
            list -> list.stream().noneMatch(it -> it.isIncluded()) && list.stream().noneMatch(it -> !it.isIncluded());

        Predicate<List<? extends ExcludableValue<?>>> hasEmptyIncludedAndNonEmptyExcluded =
            list -> list.stream().noneMatch(it -> it.isIncluded()) && list.stream().anyMatch(it -> !it.isIncluded());

        Function<Path, @Nullable Label> mapDirectoryToTarget = buildDirectoryIdentifier -> {
            if (buildDirectoryIdentifier.isAbsolute()) {
                return null;
            }
            // consulo: Kotlin Path.invariantSeparatorsPathString
            String buildDirectoryPath =
                buildDirectoryIdentifier.toString().replace(buildDirectoryIdentifier.getFileSystem().getSeparator(), "/");
            if (buildDirectoryPath.isEmpty()) {
                return null;
            }

            if (buildDirectoryPath.equals(".")) {
                return Label.parse("//...");
            }
            else {
                return Label.parse("//" + buildDirectoryPath + "/...");
            }
        };

        if (dirs.isEmpty()) {
            return targets;
        }
        else if (hasEmptyIncludedAndEmptyExcluded.test(dirs)) {
            return targets;
        }
        else if (hasEmptyIncludedAndNonEmptyExcluded.test(dirs)) {
            throw new IllegalArgumentException("'directories' section has no included targets.");
        }
        else {
            List<ExcludableValue<Label>> directoriesValues =
                dirs.stream()
                    .filter(it -> it.isIncluded())
                    .map(it -> mapDirectoryToTarget.apply(it.value()))
                    .filter(Objects::nonNull)
                    .map(it -> ExcludableValue.included(it))
                    .toList();
            List<ExcludableValue<Label>> directoriesExcludedValues =
                dirs.stream()
                    .filter(it -> !it.isIncluded())
                    .map(it -> mapDirectoryToTarget.apply(it.value()))
                    .filter(Objects::nonNull)
                    .map(it -> ExcludableValue.excluded(it))
                    .toList();
            List<ExcludableValue<Label>> result = new ArrayList<>(targets);
            result.addAll(directoriesValues);
            result.addAll(directoriesExcludedValues);
            return result;
        }
    }
}
