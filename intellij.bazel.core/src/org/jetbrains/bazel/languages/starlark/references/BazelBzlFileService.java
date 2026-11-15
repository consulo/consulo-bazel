package org.jetbrains.bazel.languages.starlark.references;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.Canonical;
import org.jetbrains.bazel.label.Label.Package;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.label.Label.SingleTarget;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingProvider;
import org.jetbrains.bazel.sync.SyncCache;
import org.jetbrains.bazel.workspace.BazelProjectDirectoriesUtils;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class BazelBzlFileService {
    private BazelBzlFileService() {
    }

    // consulo: Kotlin read-only collections are covariant, Java needs the unchecked cast
    @SuppressWarnings("unchecked")
    public static Map<String, List<Label>> getCanonicalRepoNameToBzlFiles(Project project) {
        return (Map<String, List<Label>>) (Map<String, ?>) SyncCache.getInstance(project).get(CANONICAL_REPO_NAME_TO_BZL_FILES_VALUE);
    }

    private static final SyncCache.SyncCacheComputable<Map<String, List<ResolvedLabel>>> CANONICAL_REPO_NAME_TO_BZL_FILES_VALUE =
        project -> calculateApparentRepoNameToFiles(project);

    private static Map<String, List<ResolvedLabel>> calculateApparentRepoNameToFiles(Project project) {
        Map<String, List<ResolvedLabel>> newMap = new HashMap<>();

        Map<String, Path> canonicalRepoNameToPath = BazelRepoMappingProvider.getCanonicalRepoNameToPath(project);
        Set<VirtualFile> canonicalRepoPaths = new HashSet<>();
        for (Path it : canonicalRepoNameToPath.values()) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(it);
            if (file != null) {
                canonicalRepoPaths.add(file);
            }
        }

        Set<VirtualFile> excludedRoots = BazelProjectDirectoriesUtils.excludedRoots(project);
        if (excludedRoots == null) {
            return Map.of();
        }

        for (Map.Entry<String, Path> entry : canonicalRepoNameToPath.entrySet()) {
            String canonicalName = entry.getKey();
            Path repoPath = entry.getValue();
            VirtualFile root = VirtualFileManager.getInstance().findFileByNioPath(repoPath);
            if (root == null) {
                continue;
            }
            Map<VirtualFile, @Nullable Path> dirToPackagePath = new HashMap<>();
            VirtualFileUtil.visitChildrenRecursively(
                root,
                new VirtualFileVisitor<Void>() {
                    @Override
                    public Result visitFileEx(VirtualFile file) {
                        if (excludedRoots.contains(file)) {
                            return SKIP_CHILDREN;
                        }
                        if (!file.equals(root) && canonicalRepoPaths.contains(file)) {
                            return SKIP_CHILDREN;
                        }
                        if (file.isDirectory()) {
                            findBuildFilePathForDirectory(file, root, dirToPackagePath);
                            return CONTINUE;
                        }
                        if (file.isDirectory() || !"bzl".equals(file.getExtension())) {
                            return CONTINUE;
                        }

                        Path buildFilePath =
                            findBuildFilePathForDirectory(Objects.requireNonNull(file.getParent()), root, dirToPackagePath);
                        Path packagePath = buildFilePath == null ? null : buildFilePath.getParent();
                        if (packagePath == null) {
                            return CONTINUE;
                        }
                        String packageName = relativeToOrNull(packagePath, repoPath);
                        if (packageName == null) {
                            return CONTINUE;
                        }
                        String targetName = relativeToOrNull(file.toNioPath(), packagePath);
                        if (targetName == null) {
                            return CONTINUE;
                        }

                        ResolvedLabel label =
                            new ResolvedLabel(
                                Canonical.createCanonicalOrMain(canonicalName),
                                new Package(List.of(packageName.split("/", -1))),
                                new SingleTarget(targetName)
                            );
                        List<ResolvedLabel> labels = new ArrayList<>(newMap.getOrDefault(canonicalName, List.of()));
                        labels.add(label);
                        newMap.put(canonicalName, labels);
                        return CONTINUE;
                    }
                }
            );
        }
        return newMap;
    }

    // consulo: Kotlin Path.relativeToOrNull(base)?.invariantSeparatorsPathString
    private static @Nullable String relativeToOrNull(Path receiver, Path base) {
        try {
            return base.relativize(receiver).toString().replace('\\', '/');
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    // consulo: Kotlin default argument cache = mutableMapOf() is always passed by the callers
    private static @Nullable Path findBuildFilePathForDirectory(
        VirtualFile dir,
        VirtualFile repoRoot,
        Map<VirtualFile, @Nullable Path> cache
    ) {
        // consulo: Kotlin MutableMap.getOrPut (a cached null value is recomputed, as in upstream)
        Path cached = cache.get(dir);
        if (cached != null) {
            return cached;
        }
        Path result;
        VirtualFile buildFile = LabelResolveUtils.findBuildFile(dir);
        if (buildFile != null) {
            result = buildFile.toNioPath();
        }
        else {
            VirtualFile parent = dir.getParent();
            if (parent == null || !VirtualFileUtil.isAncestor(repoRoot, parent, false)) {
                result = null;
            }
            else {
                result = findBuildFilePathForDirectory(parent, repoRoot, cache);
            }
        }
        cache.put(dir, result);
        return result;
    }
}
