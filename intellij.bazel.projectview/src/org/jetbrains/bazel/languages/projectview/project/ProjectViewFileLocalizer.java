package org.jetbrains.bazel.languages.projectview.project;

import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.platform.Platform;
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME;
import static org.jetbrains.bazel.commons.constants.Constants.DOT_BAZELBSP_DIR_NAME;
import static org.jetbrains.bazel.commons.constants.Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME;
import static org.jetbrains.bazel.commons.constants.Constants.PROJECT_VIEW_FILE_EXTENSION;

public final class ProjectViewFileLocalizer {
    private static final String PROJECT_VIEW_FILE_SYSTEM_PROPERTY = "bazel.project.view.file.path";

    private ProjectViewFileLocalizer() {
    }

    public static boolean isDefaultProjectViewFile(Path projectIdentityFile, Path rootDir) {
        Path parent = projectIdentityFile.getParent();
        if (parent == null) {
            return false;
        }
        String fileName = name(projectIdentityFile);
        if (parent.equals(rootDir)) {
            return fileName.equals(DEFAULT_PROJECT_VIEW_FILE_NAME) || fileName.equals(LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME);
        }
        else if (name(parent).equals(DOT_BAZELBSP_DIR_NAME)) {
            return fileName.equals(DEFAULT_PROJECT_VIEW_FILE_NAME);
        }
        else {
            return false;
        }
    }

    public static Path pickProjectViewFileForProject(Path projectFile, Path rootDir) {
        Path path = useProjectFileAsProjectView(projectFile);
        if (path == null) {
            path = useProjectViewFileFromEnvVariable();
        }
        if (path == null) {
            path = useProjectViewFileFromRootProjectDirectory(rootDir, DEFAULT_PROJECT_VIEW_FILE_NAME);
        }
        if (path == null) {
            path = useProjectViewFileFromRootProjectDirectory(rootDir, LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME);
        }
        if (path == null) {
            path = useDotBazelBspProjectViewFile(rootDir);
        }
        return path;
    }

    private static @Nullable Path useProjectFileAsProjectView(Path projectFile) {
        if (Files.isRegularFile(projectFile) && extension(projectFile).equals(PROJECT_VIEW_FILE_EXTENSION)) {
            // Either user pointed to a project view file while opening project, or
            // used a "Load Project View Action", so plugin opened project based on a project view file
            return projectFile;
        }
        // Project has been opened using a different Bazel-related file, so plugin needs to
        // find a default project view file to open, or create it.
        return null;
    }

    private static @Nullable Path useProjectViewFileFromEnvVariable() {
        // consulo: System.getProperty -> Platform.current().jvm().getRuntimeProperty, toNioPathOrNull() inlined
        String value = Platform.current().jvm().getRuntimeProperty(PROJECT_VIEW_FILE_SYSTEM_PROPERTY);
        if (value == null) {
            return null;
        }
        try {
            return Path.of(value);
        }
        catch (InvalidPathException e) {
            return null;
        }
    }

    private static @Nullable Path useProjectViewFileFromRootProjectDirectory(Path rootDir, String fileName) {
        Path resolvedFilePath = rootDir.resolve(fileName);
        if (Files.isRegularFile(resolvedFilePath)) {
            return resolvedFilePath;
        }
        return null;
    }

    /**
     * The plugin calls this function before the user trusts the project.
     * Thus a symlink in the project must not move the created file out of the project.
     */
    private static Path useDotBazelBspProjectViewFile(Path rootDir) {
        // consulo: IntelliJ Path.findOrCreateDirectory(relativePath)
        Path dotBazelBspDirectory = rootDir.resolve(DOT_BAZELBSP_DIR_NAME);
        try {
            Files.createDirectories(dotBazelBspDirectory);
            if (!dotBazelBspDirectory.toRealPath().startsWith(rootDir.toRealPath())) {
                throw new IllegalStateException(
                    BazelProjectViewLocalize.projectViewFileOutsideOfProjectRoot(dotBazelBspDirectory, rootDir).get()
                );
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Path projectViewFile = dotBazelBspDirectory.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME);

        if (Files.isRegularFile(projectViewFile)) {
            // Do not overwrite this file if it exists, because user may make his changes there.
            // According to the plugin documentation the idea is that .bazelbsp/.bazelproject is
            // a per-user file that isn't meant to be commited to the repo.
            // https://www.jetbrains.com/help/idea/2026.1/bazel-project-view.html
            return projectViewFile;
        }

        String projectViewTemplate = ProjectViewFileUtils.projectViewTemplate(Objects.requireNonNull(PathUtil.findVirtualFile(rootDir)));
        try {
            // CREATE_NEW fails on any existing entry, also on a dangling symlink
            Files.writeString(
                projectViewFile,
                projectViewTemplate,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );
        }
        catch (FileAlreadyExistsException e) {
            throw new IllegalStateException(BazelProjectViewLocalize.projectViewFileNotRegularFile(projectViewFile).get());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return projectViewFile;
    }

    // consulo: kotlin.io.path.name
    private static String name(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : "";
    }

    // consulo: kotlin.io.path.extension
    private static String extension(Path path) {
        String name = name(path);
        int index = name.lastIndexOf('.');
        return index >= 0 ? name.substring(index + 1) : "";
    }
}
