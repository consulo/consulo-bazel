package org.jetbrains.bazel.flow.open;

// TODO rewrite with no VirtualFile operations!
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class ProjectViewFileUtils {
    // consulo: upstream private top-level declarations of this file

    private static final String PROJECTVIEW_DEFAULT_TEMPLATE_PATH = "tools/intellij/.managed.bazelproject";

    private static final String INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE =
        """
            # This project view file may be overwritten by the Bazel plugin.
            # To use it as default, place it in the Bazel module or workspace root directory.
            # For more options, see project view file documentation: https://www.jetbrains.com/help/idea/bazel-project-view.html

            derive_targets_from_directories: true
            directories: %s

            # Uncomment this line to make indexing quicker:
            # import_depth: 0
            """;

    private ProjectViewFileUtils() {
    }

    // consulo: Kotlin default arguments format = null, forceDefaultTemplate = false
    public static String projectViewTemplate(VirtualFile projectRootDir) {
        return projectViewTemplate(projectRootDir, null, false);
    }

    public static String projectViewTemplate(
        VirtualFile projectRootDir,
        @Nullable String format,
        boolean forceDefaultTemplate
    ) {
        String rawText;
        if (forceDefaultTemplate) {
            rawText = INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE;
        }
        else {
            // consulo: VirtualFile.resolveFromRootOrRelative(path) -> findFileByRelativePath (the path is relative)
            VirtualFile templateFile = projectRootDir.findFileByRelativePath(PROJECTVIEW_DEFAULT_TEMPLATE_PATH);
            rawText = templateFile != null ? readText(templateFile) : INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE;
        }
        return rawText.formatted(format == null || format.isBlank() ? "." : format);
    }

    // consulo: IntelliJ VirtualFile.readText()
    private static String readText(VirtualFile file) {
        try {
            return VirtualFileUtil.loadText(file);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
