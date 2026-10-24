package org.jetbrains.bazel.languages.projectview.imports;

import consulo.build.ui.FilePosition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.languages.projectview.imports.Import.Resolved;
import org.jetbrains.bazel.languages.projectview.imports.Import.Unresolved;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportItem;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

// consulo: upstream Import.kt of intellij.bazel.projectview. intellij.bazel.commons already declares
// org.jetbrains.bazel.languages.projectview.imports.Import, so this file is named after the class it declares.
// ImportFactory is used from org.jetbrains.bazel.languages.projectview, so it is public.
public final class ImportFactory {
    private ImportFactory() {
    }

    public static Import from(
        Path root,
        ProjectViewPsiImportBase element,
        @Nullable Path sourcePath
    ) {
        ProjectViewPsiImportItem importPathElement = element.getImportPath();
        String path = importPathElement != null ? importPathElement.getText().trim() : "";
        Path file = resolveImport(root, path);
        boolean required = element instanceof ProjectViewPsiImport;
        if (file == null) {
            return new Unresolved(path, importPathElement != null ? filePositionOrNull(importPathElement, sourcePath) : null, required);
        }
        else {
            return new Resolved(file, required);
        }
    }

    private static @Nullable Path resolveImport(Path receiver, String importPath) {
        if (importPath.isEmpty()) {
            return null;
        }
        Path path;
        try {
            path = receiver.resolve(importPath);
        }
        catch (InvalidPathException ignored) {
            return null;
        }
        Path normalized = path.normalize();
        return Files.isRegularFile(normalized) ? normalized : null;
    }

    private static @Nullable FilePosition filePositionOrNull(PsiElement receiver, @Nullable Path path) {
        if (path == null) {
            return null;
        }
        PsiFile containingFile = receiver.getContainingFile();
        if (containingFile == null) {
            return null;
        }
        String text = containingFile.getText();
        // consulo: Consulo FilePosition takes a java.io.File
        LineColumn start = offsetToLineColumn(text, receiver.getTextRange().getStartOffset());
        if (start == null) {
            return new FilePosition(path.toFile(), 0, 0);
        }
        LineColumn end = offsetToLineColumn(text, receiver.getTextRange().getEndOffset());
        if (end == null) {
            return new FilePosition(path.toFile(), 0, 0);
        }
        return new FilePosition(path.toFile(), start.line(), start.column(), end.line(), end.column());
    }

    // consulo: IntelliJ StringUtil.offsetToLineColumn and LineColumn do not exist in Consulo
    private record LineColumn(int line, int column) {
    }

    private static @Nullable LineColumn offsetToLineColumn(String text, int offset) {
        int line = StringUtil.offsetToLineNumber(text, offset);
        if (line < 0) {
            return null;
        }
        int lineStartOffset = StringUtil.lineColToOffset(text, line, 0);
        return new LineColumn(line, offset - lineStartOffset);
    }

    // consulo: upstream top-level extension function Import.resolvedPathOrNull()
    public static @Nullable Path resolvedPathOrNull(Import receiver) {
        return receiver instanceof Resolved resolved ? resolved.path() : null;
    }
}
