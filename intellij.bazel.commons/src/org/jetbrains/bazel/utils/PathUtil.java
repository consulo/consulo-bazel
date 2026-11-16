package org.jetbrains.bazel.utils;

import consulo.platform.Platform;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class PathUtil {
    private PathUtil() {
    }

    public static Stream<Path> allAncestorsSequence(Path receiver) {
        return Stream.iterate(receiver, Objects::nonNull, Path::getParent);
    }

    /**
     * See {@code com.intellij.openapi.vfs.VfsUtilCore.isUnder}
     */
    public static boolean isUnder(Path receiver, Set<Path> ancestors) {
        return allAncestorsSequence(receiver).anyMatch(ancestors::contains);
    }

    /**
     * Checks whether the file attributes represent a symbolic link or a Windows junction.
     * <p>
     * On Windows, Bazel might use junctions as an alternative to convenience symbolic links.
     * They are not recognized by Java API as symlinks.
     */
    public static boolean isSymbolicLinkOrJunction(BasicFileAttributes receiver) {
        return receiver.isSymbolicLink() || isWindowsJunction(receiver);
    }

    /**
     * Checks whether the path is a symbolic link or a Windows junction, see {@link #isSymbolicLinkOrJunction(BasicFileAttributes)}.
     * <p>
     * This property reads the file attributes. The result is `false` when the attributes cannot be read.
     */
    public static boolean isSymbolicLinkOrJunction(Path receiver) {
        try {
            return isSymbolicLinkOrJunction(Files.readAttributes(receiver, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
        }
        catch (IOException e) {
            return false;
        }
    }

    public static boolean isWindowsJunction(BasicFileAttributes receiver) {
        return Platform.current().os().isWindows() && receiver.isDirectory() && receiver.isOther();
    }

    /**
     * Checks whether the path is a Windows junction.
     * <p>
     * This property reads the file attributes, so use {@link #isWindowsJunction(BasicFileAttributes)} when the caller
     * already holds them. The result is `false` when the attributes cannot be read.
     */
    public static boolean isWindowsJunction(Path receiver) {
        try {
            return Platform.current().os().isWindows()
                && isWindowsJunction(Files.readAttributes(receiver, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
        }
        catch (IOException e) {
            return false;
        }
    }

    public static @Nullable Path readSymbolicLinkTarget(Path receiver) {
        try {
            return receiver.resolveSibling(Files.readSymbolicLink(receiver)).normalize();  // readSymbolicLink can be a relative path!
        }
        catch (IOException e) {
            return null;
        }
    }

    public static @Nullable Path toRealPathOrNull(Path receiver) {
        try {
            return receiver.toRealPath();
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     * The real path of this path, or this path when the real path cannot be read, for example when the file does not exist.
     */
    public static Path toRealPathOrSelf(Path receiver) {
        Path realPath = toRealPathOrNull(receiver);
        return realPath != null ? realPath : receiver;
    }

    /**
     * This path with its {@code from} prefix changed to {@code to}, or {@code null} when this path is not under {@code from}.
     * For example, {@code /a/b/c} with {@code from} {@code /a} and {@code to} {@code /x} gives {@code /x/b/c}.
     */
    public static @Nullable Path rebaseOrNull(Path receiver, Path from, Path to) {
        if (!receiver.startsWith(from)) {
            return null;
        }
        else if (from.equals(to)) {
            return receiver;
        }
        else if (receiver.getNameCount() == from.getNameCount()) {
            return to;
        }
        else {
            return to.resolve(receiver.subpath(from.getNameCount(), receiver.getNameCount()));
        }
    }

    /**
     * This path with its {@code from} prefix changed to {@code to}. This path must be under {@code from}, see {@link #rebaseOrNull}.
     */
    public static Path rebase(Path receiver, Path from, Path to) {
        Path result = rebaseOrNull(receiver, from, to);
        if (result == null) {
            throw new IllegalArgumentException(receiver + " is not under " + from);
        }
        return result;
    }

    /**
     * Checks whether the real path of this path is inside the real path of {@code root}.
     * The normalized path must be inside the normalized {@code root} first, or the function returns {@code false} and does not touch
     * the file system.
     * This path does not have to exist, so the function resolves the real path of the nearest existing parent.
     * A dangling symlink counts as existing, so its real path fails and the function returns {@code false}.
     */
    public static boolean isRealPathInside(Path receiver, Path root) {
        if (!receiver.normalize().startsWith(root.normalize())) {
            return false;
        }
        Path existingParent = allAncestorsSequence(receiver).filter(it -> Files.exists(it, LinkOption.NOFOLLOW_LINKS)).findFirst().orElse(null);
        if (existingParent == null) {
            return false;
        }
        Path realParent = toRealPathOrNull(existingParent);
        if (realParent == null) {
            return false;
        }
        Path realRoot = toRealPathOrNull(root);
        if (realRoot == null) {
            return false;
        }
        return rebase(receiver, existingParent, realParent).startsWith(realRoot);
    }

    /**
     * See {@code com.intellij.openapi.vfs.VfsUtilCore.getCommonAncestor}.
     * Input paths must be absolute and normalized.
     */
    public static @Nullable Path calculateCommonAncestor(Path file1, Path file2) {
        if (file1.equals(file2)) {
            return file1;
        }

        int depth1 = file1.getNameCount();
        int depth2 = file2.getNameCount();

        Path parent1 = file1;
        Path parent2 = file2;
        while (depth1 > depth2 && parent1 != null) {
            parent1 = parent1.getParent();
            depth1--;
        }
        while (depth2 > depth1 && parent2 != null) {
            parent2 = parent2.getParent();
            depth2--;
        }
        while (parent1 != null && parent2 != null && !parent1.equals(parent2)) {
            parent1 = parent1.getParent();
            parent2 = parent2.getParent();
        }
        return parent1;
    }

    /**
     * Input paths must be absolute and normalized.
     */
    public static @Nullable Path commonAncestor(Collection<Path> receiver) {
        if (receiver.isEmpty()) {
            return null;
        }
        Iterator<Path> iterator = receiver.iterator();
        Path result = iterator.next();
        while (iterator.hasNext()) {
            Path path = iterator.next();
            result = calculateCommonAncestor(result, path);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    public static List<Path> filterPathsThatDontContainEachOther(Set<Path> receiver) {
        return receiver.stream().filter(path -> !isUnder(Objects.requireNonNull(path.getParent()), receiver)).toList();
    }

    public static @Nullable VirtualFile refreshAndFindVirtualFile(Path receiver) {
        return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(receiver);
    }

    public static @Nullable VirtualFile findVirtualFileLocal(Path receiver) {
        return VirtualFileManager.getInstance().findFileByNioPath(receiver);
    }

    public static @Nullable VirtualFile findVirtualFile(Path receiver) {
        return VirtualFileManager.getInstance().findFileByNioPath(receiver);
    }

    public static @Nullable VirtualFile findCanonicalVirtualFileThatExists(Path receiver) {
        VirtualFile virtualFile = findVirtualFile(receiver);
        VirtualFile canonicalFile = virtualFile != null ? virtualFile.getCanonicalFile() : null;
        return canonicalFile != null && canonicalFile.exists() ? canonicalFile : null;
    }
}
