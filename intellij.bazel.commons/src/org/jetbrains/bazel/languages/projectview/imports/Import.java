package org.jetbrains.bazel.languages.projectview.imports;

import consulo.build.ui.FilePosition;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

// consulo: upstream sealed class with data class subclasses; a sealed interface keeps the records' equals/hashCode
public sealed interface Import {

    boolean isRequired();

    record Resolved(Path path, boolean isRequired) implements Import {
    }

    record Unresolved(String text, @Nullable FilePosition position, boolean isRequired) implements Import {
    }
}
