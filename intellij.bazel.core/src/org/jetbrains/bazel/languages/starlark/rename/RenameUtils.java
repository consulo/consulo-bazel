package org.jetbrains.bazel.languages.starlark.rename;

import consulo.language.ast.ASTNode;
import consulo.project.Project;

public final class RenameUtils {
    private RenameUtils() {
    }

    public static ASTNode createNewName(Project project, String name) {
        return new StarlarkElementGenerator(project).createNameIdentifier(name);
    }
}
