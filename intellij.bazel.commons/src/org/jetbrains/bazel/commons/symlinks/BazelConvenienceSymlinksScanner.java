package org.jetbrains.bazel.commons.symlinks;

import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.config.BazelFeatureFlags;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.jetbrains.bazel.commons.constants.Constants.WORKSPACE_FILE_NAMES;

public class BazelConvenienceSymlinksScanner {
    private static final Logger log = Logger.getInstance(BazelConvenienceSymlinksScanner.class);

    private final Path myWorkspaceRoot;
    private final int myMaxDepth;

    private final boolean myIgnoreCase = !Platform.current().fs().isCaseSensitive();

    private final List<String> myBazelSymlinkSuffixes;

    public BazelConvenienceSymlinksScanner(Path workspaceRoot, int maxDepth) {
        myWorkspaceRoot = workspaceRoot;
        myMaxDepth = maxDepth;
        myBazelSymlinkSuffixes = List.of("bin", "out", "testlogs", name(workspaceRoot));
    }

    // consulo: overload for the upstream default argument (maxDepth = BazelFeatureFlags.symlinkScanMaxDepth)
    public BazelConvenienceSymlinksScanner(Path workspaceRoot) {
        this(workspaceRoot, BazelFeatureFlags.getSymlinkScanMaxDepth());
    }

    public Path getWorkspaceRoot() {
        return myWorkspaceRoot;
    }

    /**
     * Scans for all Bazel convenience symlinks in the {@code workspaceRoot} as deep as {@code maxDepth}.
     */
    public Set<Path> scan() {
        if (myMaxDepth <= 0) {
            return Set.of();
        }
        // Don't scan non-Bazel projects for symlinks (because it can be quite slow).
        boolean noWorkspaceFile = true;
        for (String workspaceFileName : WORKSPACE_FILE_NAMES) {
            if (Files.exists(myWorkspaceRoot.resolve(workspaceFileName))) {
                noWorkspaceFile = false;
                break;
            }
        }
        if (noWorkspaceFile) {
            return Set.of();
        }
        Set<Path> symlinks = new HashSet<>();
        visitSymlinksAndJunctions(myWorkspaceRoot, myMaxDepth, it -> {
            if (isBazelSymlink(it)) {
                symlinks.add(it);
            }
        });
        return symlinks;
    }

    /**
     * Whether {@code path} lies under a Bazel convenience symlink, looking as deep as {@link #scan} does.
     */
    public boolean isUnderBazelSymlink(Path path) {
        return ancestorsWithinScanDepth(path)
            .anyMatch(it -> PathUtil.isSymbolicLinkOrJunction(it) && isBazelSymlink(it));
    }

    public boolean isBazelSymlink(VirtualFile file) {
        // consulo: IntelliJ VirtualFile.toNioPathOrNull()
        Path path;
        try {
            path = file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return false;
        }
        return isBazelSymlink(path);
    }

    /**
     * Whether {@code path} is a Bazel convenience symlink. It checks the name and the target only.
     * Unlike {@link #scan} and {@link #isUnderBazelSymlink}, it does not limit the depth.
     */
    public boolean isBazelSymlink(Path path) {
        boolean noSuffixMatches = true;
        for (String it : myBazelSymlinkSuffixes) {
            if (endsWith(name(path), it, myIgnoreCase)) {
                noSuffixMatches = false;
                break;
            }
        }
        if (noSuffixMatches) {
            return false;
        }
        Path target = resolveSymlinkTarget(path);
        if (target == null && PathUtil.isWindowsJunction(path)) {
            // on Windows, we cannot check the junction's target if it does not exist
            log.info("Unresolved junction " + path + " excluded");
            return true;
        }
        else if (target == null) {
            log.info("Symlink " + path + " not excluded - cannot resolve the symlink's target");
            return false;
        }
        // Compare with [ignoreCase] and not with Path.equals, which respects the case on macOS
        // although SystemInfoRt reports the macOS file system as case-insensitive.
        else if (anyNameEquals(target, "execroot", myIgnoreCase)) {
            // See https://bazel.build/remote/output-directories
            // This string used to be "execroot/_main", but for projects without Bzlmod the relevant path is actually "execroot/<my-project>"
            return true;
        }
        else {
            log.info("Symlink " + path + " not excluded - symlink does not point to a Bazel output directory");
            return false;
        }
    }

    /**
     * The ancestors of this path below the workspace root, from the shallowest one, at most {@code maxDepth} of them.
     */
    private Stream<Path> ancestorsWithinScanDepth(Path receiver) {
        if (!receiver.startsWith(myWorkspaceRoot) || receiver.getNameCount() == myWorkspaceRoot.getNameCount()) {
            return Stream.empty();
        }
        Path relativePath = receiver.subpath(myWorkspaceRoot.getNameCount(), receiver.getNameCount());
        return IntStream.rangeClosed(1, Math.min(relativePath.getNameCount(), myMaxDepth))
            .mapToObj(it -> myWorkspaceRoot.resolve(relativePath.subpath(0, it)));
    }

    public static @Nullable Path resolveSymlinkTarget(Path symlink) {
        Path target = PathUtil.readSymbolicLinkTarget(symlink);
        return target != null ? target : readJunctionTarget(symlink);
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
                        log.warn("Failed to visit file " + file, exception);
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

    /**
     * Reads the target of a Windows junction.
     * <p>
     * {@link Files#readSymbolicLink} rejects a junction, because a junction carries the `IO_REPARSE_TAG_MOUNT_POINT` reparse tag
     * and not `IO_REPARSE_TAG_SYMLINK`. {@link Path#toRealPath} resolves a junction, but it needs the target to exist.
     */
    private static @Nullable Path readJunctionTarget(Path receiver) {
        return PathUtil.isWindowsJunction(receiver) ? PathUtil.toRealPathOrNull(receiver) : null;
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

    // consulo: Kotlin target.any { it.name.equals(name, ignoreCase = ignoreCase) }
    private static boolean anyNameEquals(Path path, String expected, boolean ignoreCase) {
        for (Path it : path) {
            String name = name(it);
            if (ignoreCase ? name.equalsIgnoreCase(expected) : name.equals(expected)) {
                return true;
            }
        }
        return false;
    }
}
