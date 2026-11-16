package org.jetbrains.bazel.languages.projectview.imports;

import java.nio.file.Path;

// consulo: upstream sealed class with data class subclasses; a sealed interface keeps the records' equals/hashCode
public sealed interface Import {

    boolean isRequired();

    record Resolved(Path path, boolean isRequired) implements Import {
    }

    record Unresolved(String text, boolean isRequired) implements Import {
    }

    /**
     * An import whose normalized {@code path} is outside of the project root.
     * The plugin does not touch the file system for such a path.
     */
    record OutsideOfProjectRoot(Path path, boolean isRequired) implements Import {
    }
}
