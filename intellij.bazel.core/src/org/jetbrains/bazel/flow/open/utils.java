package org.jetbrains.bazel.flow.open;

import consulo.application.Application;
import consulo.bazel.core.BazelProjectStoreService;
import consulo.application.ReadAction;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.UIAccess;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class utils {
    private utils() {
    }

    /**
     * when a file/subdirectory is selected for opening a Bazel project,
     * this method provides information about the real project directory to open
     */
    public static @Nullable Path findProjectFolderFromFile(@Nullable Path path) {
        // consulo: upstream tailrec function, written as a loop
        Path current = path;
        while (true) {
            if (current == null || shouldStopTraversal(current)) {
                return null;
            }
            else if (getWorkspaceFile(current) != null) {
                return current;
            }
            // this is to prevent opening a file that is not an acceptable Bazel config file, #BAZEL-1940
            // TODO(Son): figure out how to write a test for it to avoid regression later
            else if (Files.isRegularFile(current) &&
                !hasNameOf(current, acceptedFileNames()) &&
                !hasExtensionOf(current, Constants.PROJECT_VIEW_FILE_EXTENSION)) {
                return null;
            }
            else {
                current = current.getParent();
            }
        }
    }

    private static String[] acceptedFileNames() {
        return Stream.of(
            Arrays.stream(Constants.WORKSPACE_FILE_NAMES),
            Arrays.stream(Constants.BUILD_FILE_NAMES),
            Stream.of(Constants.MODULE_BAZEL_LOCK_FILE_NAME)
        ).flatMap(it -> it).toArray(String[]::new);
    }

    /**
     * A Bazel project root should never be the filesystem root or the temp directory itself.
     * A workspace file in one of those directories is most probably an accident, so the walk stops before it.
     *
     * It mostly matters for tests, especially when opening a directory from temp dir.
     * It's really hard to guarantee in tests that any workspace file is not in the hierarchy.
     *
     * */
    private static boolean shouldStopTraversal(Path receiver) {
        return receiver.equals(receiver.getRoot()) || REJECTED_DIRECTORIES.get().contains(receiver);
    }

    private static final Supplier<Set<Path>> REJECTED_DIRECTORIES = LazyValue.notNull(() -> {
        String tempProperty = System.getProperty("java.io.tmpdir");
        if (tempProperty == null) {
            return Set.of();
        }
        Path tempPath = Path.of(tempProperty);
        @Nullable Path realTempPath = PathUtil.toRealPathOrNull(tempPath);
        Set<Path> result = new LinkedHashSet<>();
        result.add(tempPath);
        if (realTempPath != null) {
            result.add(realTempPath);
        }
        return result;
    });

    /**
     * The real path of a project root, and a file under it.
     */
    // consulo: upstream data class with a private constructor, so a final class instead of a record
    public static final class ResolvedProjectRoot {
        private final Path myRootDir;
        private final Path myOpenedFile;

        private ResolvedProjectRoot(Path rootDir, Path openedFile) {
            myRootDir = rootDir;
            myOpenedFile = openedFile;
        }

        public Path getRootDir() {
            return myRootDir;
        }

        public Path getOpenedFile() {
            return myOpenedFile;
        }

        /**
         * Finds the project root of {@code openedFile} with {@link #findProjectFolderFromFile}, changes it to its real path,
         * and moves {@code openedFile} onto it. Only the root is resolved.
         * Returns {@code null} when {@code openedFile} is not in a Bazel project.
         */
        public static @Nullable ResolvedProjectRoot from(Path openedFile) {
            Path projectRoot = findProjectFolderFromFile(openedFile);
            if (projectRoot == null) {
                return null;
            }
            Path realProjectRoot = PathUtil.toRealPathOrSelf(projectRoot);
            return new ResolvedProjectRoot(
                /* rootDir = */ realProjectRoot,
                /* openedFile = */ PathUtil.rebase(openedFile, /* from = */ projectRoot, /* to = */ realProjectRoot)
            );
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ResolvedProjectRoot that)) {
                return false;
            }
            return myRootDir.equals(that.myRootDir) && myOpenedFile.equals(that.myOpenedFile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myRootDir, myOpenedFile);
        }

        @Override
        public String toString() {
            return "ResolvedProjectRoot(rootDir=" + myRootDir + ", openedFile=" + myOpenedFile + ")";
        }
    }

    public static @Nullable VirtualFile findProjectFolderFromVFile(@Nullable VirtualFile projectIdentityFile) {
        if (projectIdentityFile == null) {
            return null;
        }
        Path path = toNioPathOrNull(projectIdentityFile);
        if (path == null) {
            return null;
        }
        Path projectFolder = findProjectFolderFromFile(path);
        if (projectFolder == null) {
            return null;
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectFolder);
    }

    public static @Nullable Path getWorkspaceFile(Path receiver) {
        if (!Files.isDirectory(receiver)) {
            return null;
        }

        return Arrays.stream(Constants.WORKSPACE_FILE_NAMES)
            .map(receiver::resolve)
            .filter(Files::isRegularFile)
            .findFirst()
            .orElse(null);
    }

    public static boolean hasNameOf(Path receiver, String... names) {
        return Files.isRegularFile(receiver) &&
            Arrays.asList(names).contains(receiver.getFileName().toString());
    }

    public static boolean hasExtensionOf(Path receiver, String... extensions) {
        return Files.isRegularFile(receiver) &&
            Arrays.asList(extensions).contains(extension(receiver));
    }

    // consulo: kotlin.io.path.extension
    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1);
    }

    public static boolean isBazelWorkspaceFile(VirtualFile receiver) {
        if (receiver.isDirectory()) {
            return false;
        }
        return Arrays.asList(Constants.WORKSPACE_FILE_NAMES).contains(receiver.getName()) ||
            Constants.PROJECT_VIEW_FILE_EXTENSION.equals(receiver.getExtension());
    }

    public static CompletableFuture<?> openProjectViewInEditor(
        Project project,
        VirtualFile file
    ) {
        // README.md should still be the selected tab afterward
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        // consulo: FileEditorOpenOptions(selectAsCurrent = false, requestFocus = false) is internal, the file is opened without focus.
        // The document is loaded here (background thread), so opening the editor on the UI thread does no file IO
        ReadAction.run(() -> FileDocumentManager.getInstance().getDocument(file));
        return project.getUIAccess().giveAsync(() -> {
            fileEditorManager.openFile(file, false);
        });
    }

    /**
     * Closes the current project and reopens it as a Bazel project.
     * Must be called from application-level scope (use {@link BazelApplicationCoroutineScopeService#launch}).
     */
    public static CompletableFuture<?> closeAndReopenAsBazelProject(Project project, Path file) {
        // consulo: Consulo has no public API to open a project with an IntelliJ OpenProjectTask; the Bazel store descriptor
        // (what BazelProjectStorePathCustomizer computes for the file) is written into the project, and the project is reloaded
        BazelProjectStoreDescriptor descriptor = new BazelProjectStorePathCustomizer().getStoreDirectoryPath(file);
        if (descriptor == null) {
            return CompletableFuture.completedFuture(null);
        }
        BazelProjectStoreService.getInstance(project).setDescriptor(descriptor);
        UIAccess uiAccess = Application.get().getLastUIAccess();
        // reloadProject saves the project before closing it
        return uiAccess.giveAsync(() -> {
            ProjectManager.getInstance().reloadProject(project, uiAccess);
        });
    }

    private static @Nullable Path toNioPathOrNull(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
