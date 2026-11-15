package org.jetbrains.bazel.workspace.importer;

import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap;
import org.jetbrains.bazel.sync.workspace.snapshot.InMemoryFileToTargetMap;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bazel.workspace.importer.SourceRootBuilder.ResolvedSourceRoot;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bazel reports every source file of a target individually, and too many single-file source roots are expensive
 * for the workspace model. For one target, this class replaces single-file roots with directory roots where it is
 * non-contradictory, i.e., the directory contains neither JVM sources which are foreign to the target
 * nor resources of the target (they get resource roots of their own).
 * <p>
 * - real source files are merged into the deepest directory which contains all of them; if that directory is
 * contradictory or is not strictly below the target's package root (the directory with the BUILD file),
 * the files are split by its subdirectories which are merged separately, so files located directly
 * in the package root or in a contradictory directory are never merged;
 * - a generated source file is merged only into its parent directory;
 * - files shared between several targets are never merged and block merging of their directories;
 * - files which cannot be merged are kept as single-file roots.
 */
public class JavaSourceRootMerger {
    private final FileToTargetMap myFileToTargets;

    private final ConcurrentHashMap<Path, VirtualFile> myVirtualFileCache = new ConcurrentHashMap<>();

    public JavaSourceRootMerger(FileToTargetMap fileToTargets) {
        myFileToTargets = fileToTargets;
    }

    public List<ResolvedSourceRoot> merge(Path baseDirectory, List<ResolvedSourceRoot> sourceRoots) {
        return merge(baseDirectory, sourceRoots, List.of());
    }

    public List<ResolvedSourceRoot> merge(
        Path baseDirectory,
        List<ResolvedSourceRoot> sourceRoots,
        Collection<Path> resourceFiles
    ) {
        if (!BazelFeatureFlags.getMergeSourceRoots()) {
            return sourceRoots;
        }

        List<ResolvedSourceRoot> mergeableSourceRoots = new ArrayList<>();
        List<ResolvedSourceRoot> keptSourceRoots = new ArrayList<>();
        for (ResolvedSourceRoot it : sourceRoots) {
            (isMergeable(it) ? mergeableSourceRoots : keptSourceRoots).add(it);
        }
        Set<VirtualFile> knownFiles = new HashSet<>();
        for (ResolvedSourceRoot it : mergeableSourceRoots) {
            VirtualFile file = findOrRefreshVirtualFile(it.sourcePath());
            if (file != null) {
                knownFiles.add(file);
            }
        }
        UnknownFileFinder finder = new UnknownFileFinder(
            knownFiles,
            new HashSet<>(resourceFiles),
            Constants.JVM_LANGUAGES_EXTENSIONS
        );
        MergeResult result = new MergeResult();

        List<ResolvedSourceRoot> generatedSourceRoots = new ArrayList<>();
        List<ResolvedSourceRoot> realSourceRoots = new ArrayList<>();
        for (ResolvedSourceRoot it : mergeableSourceRoots) {
            (it.generated() ? generatedSourceRoots : realSourceRoots).add(it);
        }
        for (ResolvedSourceRoot sourceRoot : generatedSourceRoots) {
            Path directory = parentOf(sourceRoot.sourcePath());
            if (isValidMergeTarget(directory, finder)) {
                result.merge(directory, List.of(sourceRoot));
            }
            else {
                result.keep(List.of(sourceRoot));
            }
        }

        List<ResolvedSourceRoot> inPackageSourceRoots = new ArrayList<>();
        List<ResolvedSourceRoot> outOfPackageSourceRoots = new ArrayList<>();
        for (ResolvedSourceRoot it : realSourceRoots) {
            (isStrictlyUnder(parentOf(it.sourcePath()), baseDirectory) ? inPackageSourceRoots : outOfPackageSourceRoots).add(it);
        }
        result.keep(outOfPackageSourceRoots);
        mergeIntoDeepestDirectories(inPackageSourceRoots, baseDirectory, finder, result);

        List<ResolvedSourceRoot> merged = new ArrayList<>();
        merged.addAll(result.mergedSourceRoots());
        merged.addAll(result.getNotMerged());
        merged.addAll(keptSourceRoots);
        return merged;
    }

    // consulo: a source file always has a parent directory (upstream Kotlin `parent` is a platform type)
    private static Path parentOf(Path file) {
        return Objects.requireNonNull(file.getParent());
    }

    private boolean isMergeable(ResolvedSourceRoot receiver) {
        return Files.isRegularFile(receiver.sourcePath()) &&
            Constants.JVM_LANGUAGES_EXTENSIONS.contains(BuilderUtils.extension(receiver.sourcePath())) &&
            !isSharedBetweenSeveralTargets(receiver.sourcePath());
    }

    /**
     * Merges {@code sourceRoots} into their deepest common directory if it is valid. Otherwise, splits them by the subdirectories
     * of that directory and merges each group separately (each split goes strictly deeper);
     * files located directly in the common directory are kept as is.
     */
    private void mergeIntoDeepestDirectories(
        List<ResolvedSourceRoot> sourceRoots,
        Path baseDirectory,
        UnknownFileFinder finder,
        MergeResult result
    ) {
        if (sourceRoots.isEmpty()) {
            return;
        }
        // all source roots are strictly under the base directory, so it is their common ancestor at least
        Path commonDirectory = parentOf(sourceRoots.get(0).sourcePath());
        for (int i = 1; i < sourceRoots.size(); i++) {
            Path ancestor = PathUtil.calculateCommonAncestor(commonDirectory, parentOf(sourceRoots.get(i).sourcePath()));
            commonDirectory = ancestor != null ? ancestor : baseDirectory;
        }
        if (isStrictlyUnder(commonDirectory, baseDirectory) && isValidMergeTarget(commonDirectory, finder)) {
            result.merge(commonDirectory, sourceRoots);
            return;
        }
        Map<@Nullable Path, List<ResolvedSourceRoot>> bySubdirectory = new LinkedHashMap<>();
        for (ResolvedSourceRoot sourceRoot : sourceRoots) {
            Path parent = parentOf(sourceRoot.sourcePath());
            Path key = parent.equals(commonDirectory) ? null : commonDirectory.resolve(commonDirectory.relativize(parent).getName(0));
            bySubdirectory.computeIfAbsent(key, it -> new ArrayList<>()).add(sourceRoot);
        }
        for (Map.Entry<@Nullable Path, List<ResolvedSourceRoot>> entry : bySubdirectory.entrySet()) {
            if (entry.getKey() == null) {
                result.keep(entry.getValue());
            }
            else {
                mergeIntoDeepestDirectories(entry.getValue(), baseDirectory, finder, result);
            }
        }
    }

    private static boolean isStrictlyUnder(Path receiver, Path directory) {
        return !receiver.equals(directory) && receiver.startsWith(directory);
    }

    private boolean isValidMergeTarget(Path receiver, UnknownFileFinder finder) {
        VirtualFile directory = findOrRefreshVirtualFile(receiver);
        if (directory == null) {
            return false;
        }
        return !finder.containsUnknownMemo(directory);
    }

    /**
     * A shared file can't be merged: other targets would create overlapping source roots for it, and IDEA only considers
     * the innermost source root, which causes red code, e.g., on https://github.com/bazelbuild/bazel.
     */
    private boolean isSharedBetweenSeveralTargets(Path receiver) {
        return InMemoryFileToTargetMap.get(myFileToTargets, receiver).stream()
            .map(DependencyBuilder::stripAspects)
            .distinct()
            .count() > 1;
    }

    private static final Comparator<Map.Entry<String, Integer>> rootCardinalityComparator =
        Comparator.<Map.Entry<String, Integer>, Integer>comparing(Map.Entry::getValue)
            .thenComparing(it -> it.getKey().equals(SourceRootBuilder.JAVA_TEST_SOURCE_ROOT_TYPE));

    /**
     * Prefer test roots on ties (e.g., if some utility target is in the same package as a java_test).
     */
    private static String majorityRootType(List<String> receiver) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String it : receiver) {
            counts.merge(it, 1, Integer::sum);
        }
        // consulo: Kotlin maxWith returns the first maximal element
        Map.Entry<String, Integer> max = null;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (max == null || rootCardinalityComparator.compare(entry, max) > 0) {
                max = entry;
            }
        }
        return Objects.requireNonNull(max).getKey();
    }

    private @Nullable VirtualFile findOrRefreshVirtualFile(Path receiver) {
        VirtualFile cached = myVirtualFileCache.get(receiver);
        if (cached != null) {
            return cached;
        }
        VirtualFile file = PathUtil.findVirtualFile(receiver);
        if (file == null) {
            file = PathUtil.refreshAndFindVirtualFile(receiver);
        }
        if (file == null) {
            return null;
        }
        myVirtualFileCache.put(receiver, file);
        return file;
    }

    private static class MergeResult {
        // (directory, generated) -> source roots merged into it
        private final LinkedHashMap<MergeKey, List<ResolvedSourceRoot>> myMerged = new LinkedHashMap<>();
        private final List<ResolvedSourceRoot> myNotMerged = new ArrayList<>();

        List<ResolvedSourceRoot> getNotMerged() {
            return myNotMerged;
        }

        void merge(Path directory, List<ResolvedSourceRoot> sourceRoots) {
            myMerged.computeIfAbsent(new MergeKey(directory, sourceRoots.get(0).generated()), it -> new ArrayList<>()).addAll(sourceRoots);
        }

        void keep(List<ResolvedSourceRoot> sourceRoots) {
            myNotMerged.addAll(sourceRoots);
        }

        List<ResolvedSourceRoot> mergedSourceRoots() {
            List<ResolvedSourceRoot> result = new ArrayList<>();
            for (Map.Entry<MergeKey, List<ResolvedSourceRoot>> entry : myMerged.entrySet()) {
                result.add(new ResolvedSourceRoot(
                    entry.getKey().directory(),
                    entry.getKey().generated(),
                    majorityRootType(entry.getValue().stream().map(ResolvedSourceRoot::rootType).toList())
                ));
            }
            return result;
        }
    }

    // consulo: Kotlin Pair<Path, Boolean>
    private record MergeKey(Path directory, boolean generated) {
    }

    /**
     * Memoize file tree traversal, avoids unnecessary recursively traversing file tree,
     * before time complexity was O(N * M * K), where N is amount of source roots, M
     * is average prefix length and K is amount of files in subtree.
     * <p>
     * This simple memoization reduce that to O(N) where N is size
     * of some file subtree (hard to actually define what size subtree)
     * <p>
     * Before optimization issue was especially apparent for LSP where VFS for
     * workspace importers is NOT cached and each VFS IO operation touches real file system.
     */
    private static class UnknownFileFinder {
        private final Set<VirtualFile> myKnownFiles;
        private final Set<Path> myConflictingFiles;
        private final List<String> myRelevantExtensions;
        private final HashMap<VirtualFile, Boolean> myCache = new HashMap<>();

        UnknownFileFinder(Set<VirtualFile> knownFiles, Set<Path> conflictingFiles, List<String> relevantExtensions) {
            myKnownFiles = knownFiles;
            myConflictingFiles = conflictingFiles;
            myRelevantExtensions = relevantExtensions;
        }

        boolean containsUnknownMemo(VirtualFile file) {
            Boolean cached = myCache.get(file);
            if (cached != null) {
                return cached;
            }
            boolean value;
            // symlinks are ignored, so it's safe
            if (file.isDirectory()) {
                value = false;
                VirtualFile[] children = file.getChildren();
                if (children != null) {
                    for (VirtualFile child : children) {
                        if (!child.is(VFileProperty.SYMLINK) && (child.isDirectory() ? containsUnknownMemo(child) : isUnknown(child))) {
                            value = true;
                            break;
                        }
                    }
                }
            }
            else {
                value = isUnknown(file);
            }
            myCache.put(file, value);
            return value;
        }

        private boolean isUnknown(VirtualFile receiver) {
            String extension = receiver.getExtension();
            return extension != null && myRelevantExtensions.contains(extension) && !myKnownFiles.contains(receiver) ||
                !myConflictingFiles.isEmpty() && myConflictingFiles.contains(receiver.toNioPath());
        }
    }
}
