package org.jetbrains.bazel.commons.symlinks;

import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.jetbrains.bazel.commons.constants.Constants.WORKSPACE_FILE_NAMES;

public final class BazelSymlinksCalculator {
    private BazelSymlinksCalculator() {
    }

    public static Set<Path> calculateBazelSymlinksToExclude(Path workspaceRoot, int bazelSymlinksScanMaxDepth) {
        if (bazelSymlinksScanMaxDepth <= 0) {
            return Set.of();
        }
        // Don't scan non-Bazel projects for symlinks (because it can be quite slow).
        boolean noWorkspaceFile = true;
        for (String workspaceFileName : WORKSPACE_FILE_NAMES) {
            if (Files.exists(workspaceRoot.resolve(workspaceFileName))) {
                noWorkspaceFile = false;
                break;
            }
        }
        if (noWorkspaceFile) {
            return Set.of();
        }

        Set<Path> symlinksToExclude = new HashSet<>();
        visitSymlinksAndJunctions(workspaceRoot, bazelSymlinksScanMaxDepth, it -> {
            if (isBazelSymlink(name(workspaceRoot), it)) {
                symlinksToExclude.add(it);
            }
        });
        return symlinksToExclude;
    }

    private static void visitSymlinksAndJunctions(
        Path receiver,
        int maxDepth,
        Consumer<Path> block
    ) {
        try {
            Files.walkFileTree(receiver, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) {
                    // The walk opens a junction as a directory, which fails when the junction's target is missing.
                    if (!file.equals(receiver) && PathUtil.isWindowsJunction(file)) {
                        block.accept(file);
                    }
                    else {
                        LOG.warn("Failed to visit file " + file, exception);
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    if (directory.equals(receiver) || !PathUtil.isSymbolicLinkOrJunction(attributes)) {
                        return FileVisitResult.CONTINUE;
                    }
                    else {
                        block.accept(directory);
                        // directory + isSymbolicLinkOrJunction means it's a junction, and we need to skip the subtree manually
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (PathUtil.isSymbolicLinkOrJunction(attributes)) {
                        block.accept(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            // consulo: Kotlin visitFileTree() propagates the IOException unchecked
            throw new UncheckedIOException(e);
        }
    }

    public static boolean isBazelSymlink(String workspaceRootName, Path symlink) {
        boolean noSuffixMatches = true;
        for (String it : bazelSymlinkSuffixes(workspaceRootName)) {
            if (endsWith(name(symlink), it, IGNORE_CASE)) {
                noSuffixMatches = false;
                break;
            }
        }
        if (noSuffixMatches) {
            return false;
        }
        Path target = resolveSymlinkTarget(symlink);
        if (target == null && PathUtil.isWindowsJunction(symlink)) {
            // on Windows, we cannot check the junction's target if it does not exist
            LOG.info("Unresolved junction " + symlink + " excluded");
            return true;
        }
        else if (target == null) {
            LOG.info("Symlink " + symlink + " not excluded - cannot resolve the symlink's target");
            return false;
        }
        // Compare with [ignoreCase] and not with Path.equals, which respects the case on macOS
        // although SystemInfoRt reports the macOS file system as case-insensitive.
        else if (anyNameEquals(target, EXEC_ROOT, IGNORE_CASE)) {
            // See https://bazel.build/remote/output-directories
            // This string used to be "execroot/_main", but for projects without Bzlmod the relevant path is actually "execroot/<my-project>"
            return true;
        }
        else {
            LOG.info("Symlink " + symlink + " not excluded - symlink does not point to a Bazel output directory");
            return false;
        }
    }

    public static @Nullable Path resolveSymlinkTarget(Path symlink) {
        Path target = PathUtil.readSymbolicLinkTarget(symlink);
        return target != null ? target : readJunctionTarget(symlink);
    }

    /**
     * Reads the target of a Windows junction.
     * <p>
     * {@link Files#readSymbolicLink} rejects a junction, because a junction carries the `IO_REPARSE_TAG_MOUNT_POINT` reparse tag
     * and not `IO_REPARSE_TAG_SYMLINK`. {@link Path#toRealPath} resolves a junction, but it needs the target to exist.
     */
    private static @Nullable Path readJunctionTarget(Path junction) {
        if (!PathUtil.isWindowsJunction(junction)) {
            return null;
        }
        try {
            return junction.toRealPath();
        }
        catch (IOException e) {
            return null;
        }
    }

    public static boolean isBazelSymlink(String workspaceRootName, VirtualFile file) {
        // consulo: IntelliJ VirtualFile.toNioPathOrNull()
        Path path;
        try {
            path = file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return false;
        }
        return isBazelSymlink(workspaceRootName, path);
    }

    private static List<String> bazelSymlinkSuffixes(String workspaceRootName) {
        return List.of("bin", "out", "testlogs", workspaceRootName);
    }

    // consulo: Kotlin Path.name
    private static String name(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? "" : fileName.toString();
    }

    // consulo: Kotlin String.endsWith(suffix, ignoreCase)
    private static boolean endsWith(String value, String suffix, boolean ignoreCase) {
        return value.regionMatches(ignoreCase, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    // consulo: Kotlin target.any { it.name.equals(EXEC_ROOT, ignoreCase = ignoreCase) }
    private static boolean anyNameEquals(Path path, String expected, boolean ignoreCase) {
        for (Path it : path) {
            String name = name(it);
            if (ignoreCase ? name.equalsIgnoreCase(expected) : name.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private static final String EXEC_ROOT = "execroot";

    private static final boolean IGNORE_CASE = !Platform.current().fs().isCaseSensitive();

    private static final Logger LOG = Logger.getInstance(BazelSymlinksCalculator.class);
}
