package com.intellij.bazel.python.backend;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.python.lang.PythonBuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PythonImportUtils {
    private PythonImportUtils() {
    }

    /**
     * Converts the {@link PythonBuildTarget#imports()} attribute to paths relative to the root of
     * the repository of {@code buildTarget}. For a target in the main repository, this root is the workspace root.
     */
    public static List<Path> assembleExplicitImportsPaths(BuildTarget buildTarget) {
        if (!(BuildTarget.getId(buildTarget) instanceof ResolvedLabel label)) {
            return List.of();
        }
        PythonBuildTarget ideInfo = PythonBuildTarget.extractPythonBuildTarget(buildTarget);
        if (ideInfo == null) {
            return List.of();
        }
        if (ideInfo.imports().isEmpty()) {
            return List.of();
        }

        Path packagePath = Label.toPath(label.packagePath());
        // In the case of an external repo the build path could be `/BUILD.bazel`
        // which has a basedir of `/`. In this case we translate this to `.` so
        // that it works in the sub file-system.
        Path buildParentPath = packagePath.getNameCount() != 0 ? packagePath : Path.of(".");
        List<Path> result = new ArrayList<>();
        for (String it : ideInfo.imports().asList()) {
            result.add(buildParentPath.resolve(it).normalize());
        }
        return result;
    }

    /**
     * <b>DO NOT</b> use it for workspace model content root calculation -
     * Targets without explicit {@code imports} defined will be based in the project root,
     * which could result in multiple modules containing whole project.
     * <p>
     * For content root calculations use {@code assembleExplicitImportsPaths}
     */
    public static List<ImportsPath> assembleQualifiedNameImportPaths(List<Path> explicitImportsPaths) {
        List<@Nullable Path> paths = explicitImportsPaths.isEmpty() ? Collections.singletonList(null) : new ArrayList<@Nullable Path>(explicitImportsPaths);
        List<ImportsPath> result = new ArrayList<>();
        for (@Nullable Path it : paths) {
            result.add(new ImportsPath(it));
        }
        return result;
    }

    // not ImportPath - it refers to "imports" clause in Python targets
    public record ImportsPath(@Nullable Path importedPath) {
        public @Nullable Path relativizePath(Path rootPath) {
            if (importedPath == null) {
                return rootPath;
            }
            else if (rootPath.getNameCount() <= importedPath.getNameCount() || !rootPath.startsWith(importedPath)) {
                return null; // cannot assemble non-empty relative path
            }
            else {
                return rootPath.subpath(importedPath.getNameCount(), rootPath.getNameCount());
            }
        }
    }
}
