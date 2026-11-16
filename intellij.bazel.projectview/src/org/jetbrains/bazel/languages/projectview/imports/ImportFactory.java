package org.jetbrains.bazel.languages.projectview.imports;

import org.jetbrains.bazel.languages.projectview.imports.Import.OutsideOfProjectRoot;
import org.jetbrains.bazel.languages.projectview.imports.Import.Resolved;
import org.jetbrains.bazel.languages.projectview.imports.Import.Unresolved;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportItem;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

// consulo: upstream Import.kt of intellij.bazel.projectview. intellij.bazel.commons already declares
// org.jetbrains.bazel.languages.projectview.imports.Import, so this file is named after the class it declares.
// ImportFactory is used from org.jetbrains.bazel.languages.projectview, so it is public.
public final class ImportFactory {
    private ImportFactory() {
    }

    /**
     * Resolves the import path of {@code element} against {@code root}.
     * The normalized path must be inside {@code root}, or the function returns {@link OutsideOfProjectRoot} and does not touch the file
     * system.
     */
    public static Import from(
        Path root,
        ProjectViewPsiImportBase element
    ) {
        ProjectViewPsiImportItem importPathElement = element.getImportPath();
        String text = importPathElement != null ? importPathElement.getText().trim() : "";
        boolean required = element instanceof ProjectViewPsiImport;
        if (text.isEmpty()) {
            return new Unresolved(text, required);
        }
        Path path;
        try {
            path = root.resolve(text).normalize();
        }
        catch (InvalidPathException ignored) {
            return new Unresolved(text, required);
        }
        if (!path.startsWith(root.normalize())) {
            return new OutsideOfProjectRoot(path, required);
        }
        else if (Files.isRegularFile(path)) {
            return new Resolved(path, required);
        }
        else {
            return new Unresolved(text, required);
        }
    }
}
