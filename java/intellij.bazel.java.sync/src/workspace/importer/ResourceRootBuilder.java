package org.jetbrains.bazel.workspace.importer;

import consulo.bazel.workspace.BazelModelBuilder;
import consulo.content.base.GeneratedContentFolderPropertyProvider;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.commons.symlinks.BazelConvenienceSymlinksScanner;
import org.jetbrains.bazel.sync.workspace.languages.jvm.BazelConventionalRoots;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// RC: replaces `ResourcesItemToJavaResourceRootTransformer` + `JavaResourceEntityUpdater`;
// the strip-prefix merging logic is moved as-is, the `ResourceRoot` wrapper is dropped
public final class ResourceRootBuilder {
    private static final Logger log = Logger.getInstance(ResourceRootBuilder.class);

    private ResourceRootBuilder() {
    }

    public record ResolvedResourceRoot(
        Path resourcePath,
        String rootType,
        String relativeOutputPath
    ) {
        public ResolvedResourceRoot(Path resourcePath, String rootType) {
            this(resourcePath, rootType, DEFAULT_RELATIVE_OUTPUT_PATH);
        }
    }

    public static List<ResolvedResourceRoot> resolve(
        BuildTarget target,
        @Nullable Path baseDirectory,
        BazelConvenienceSymlinksScanner symlinksScanner,
        Function<OutputLocation, @Nullable Path> resolveLocation
    ) {
        String rootType = inferRootType(target);
        List<Path> resourceFiles = JvmTargetEntitiesBuilder.resolvePaths(target.getResources(), resolveLocation);
        if (resourceFiles.isEmpty()) {
            return List.of();
        }
        Set<Path> extractedStripPrefix = extractStripPrefixOrNull(target, resolveLocation);
        Set<Path> stripPrefixes = extractedStripPrefix != null
            ? extractedStripPrefix
            : defaultStripPrefixes(target, resourceFiles, resolveLocation, symlinksScanner.getWorkspaceRoot());
        Set<Path> resourcesSet = new LinkedHashSet<>(resourceFiles);
        DirtinessCache dirtinessCache = new DirtinessCache(resourcesSet, symlinksScanner);
        MergeResult result = new MergeResult(List.of(), resourcesSet);
        for (Path prefix : stripPrefixes) {
            result = mergeUsing(result, prefix, dirtinessCache);
        }
        List<Path> leftoverPaths = collapseLeftoversByTopology(
            result.leftovers(),
            result.merged(),
            parent -> collapseCeiling(parent, baseDirectory, symlinksScanner.getWorkspaceRoot()),
            dirtinessCache
        );
        List<Path> all = new ArrayList<>(result.merged());
        all.addAll(leftoverPaths);
        List<ResolvedResourceRoot> roots = new ArrayList<>(all.size());
        for (Path path : all) {
            roots.add(new ResolvedResourceRoot(
                path,
                rootType,
                computeRelativeOutputPath(rootForFqnComputation(path), stripPrefixes)
            ));
        }
        return roots;
    }

    // consulo: a content entry with a resource folder is added to the module root model of {@code storage}
    public static void write(
        List<ResolvedResourceRoot> resources,
        Module parentModuleEntity,
        BazelModelBuilder storage
    ) {
        if (resources.isEmpty()) {
            return;
        }
        ModifiableRootModel model = storage.getRootModel(parentModuleEntity);
        for (ResolvedResourceRoot resource : resources) {
            ContentEntry contentEntry = model.addContentEntry(BuilderUtils.toResolvedVirtualFileUrl(resource.resourcePath()));
            // consulo: upstream excludedPatterns = emptyList()
            ContentFolder folder = contentEntry.addFolder(
                BuilderUtils.toVirtualFileUrl(resource.resourcePath()),
                SourceRootBuilder.toContentFolderType(resource.rootType())
            );
            // consulo: JavaResourceRootPropertiesEntity(generated = DEFAULT_GENERATED, relativeOutputPath)
            if (DEFAULT_GENERATED) {
                folder.setPropertyValue(GeneratedContentFolderPropertyProvider.IS_GENERATED, Boolean.TRUE);
            }
            // TODO consulo: the resource root relativeOutputPath (resource.relativeOutputPath(), a package prefix of the resources)
            //  is not supported by Consulo resource folders
        }
    }

    private static String inferRootType(BuildTarget receiver) {
        if (WorkspaceTarget.isTestTarget(receiver)) {
            return SourceRootBuilder.JAVA_TEST_RESOURCE_ROOT_TYPE;
        }
        else {
            return SourceRootBuilder.JAVA_RESOURCE_ROOT_TYPE;
        }
    }

    /**
     * The reference point for computing `relativeOutputPath`. For a directory resource root the
     * directory itself is the reference; for a file-level root the file's parent is, so a file at
     * `kotlin/messages/XXX.properties` and a directory root at `kotlin/messages/` produce the same
     * relative output path when both sit inside a strip prefix `kotlin/`.
     */
    private static Path rootForFqnComputation(Path path) {
        if (Files.isDirectory(path)) {
            return path;
        }
        Path parent = path.getParent();
        return parent != null ? parent : path;
    }

    /**
     * A strip prefix is the directory which Bazel maps to the classpath root. When a resource root can't be the strip prefix
     * itself (e.g., the strip prefix also contains sources) and sits strictly inside it, expose the path from the strip prefix
     * as the resource's package prefix, so `getResource` / `@PropertyKey` resolve the same path as at runtime.
     */
    private static String computeRelativeOutputPath(Path referencePath, Set<Path> stripPrefixes) {
        Path enclosing = null;
        for (Path it : stripPrefixes) {
            if (!referencePath.equals(it) && referencePath.startsWith(it)) {
                if (enclosing == null || it.getNameCount() > enclosing.getNameCount()) {
                    enclosing = it;
                }
            }
        }
        if (enclosing == null) {
            return DEFAULT_RELATIVE_OUTPUT_PATH;
        }
        Path relative = enclosing.relativize(referencePath);
        return IntStream.range(0, relative.getNameCount())
            .mapToObj(it -> relative.getName(it).toString())
            .collect(Collectors.joining("/"));
    }

    private static List<Path> collapseLeftoversByTopology(
        Set<Path> leftovers,
        List<Path> alreadyMerged,
        Function<Path, @Nullable Path> ceilingFor,
        DirtinessCache dirtinessCache
    ) {
        LinkedHashSet<Path> collapsed = new LinkedHashSet<>();
        Map<@Nullable Path, List<Path>> byParent = new LinkedHashMap<>();
        for (Path it : leftovers) {
            byParent.computeIfAbsent(it.getParent(), ignored -> new ArrayList<>()).add(it);
        }
        for (Map.Entry<@Nullable Path, List<Path>> entry : byParent.entrySet()) {
            Path parent = entry.getKey();
            List<Path> group = entry.getValue();
            Path collapseTarget;
            if (parent == null) {
                collapseTarget = null;
            }
            else {
                Path ceiling = ceilingFor.apply(parent);
                Path aggressive = ceiling != null ? findHighestCollapseAncestor(parent, ceiling, alreadyMerged, dirtinessCache) : null;
                if (aggressive != null) {
                    collapseTarget = aggressive;
                }
                else {
                    collapseTarget = canCollapseStrictly(parent, byParent, alreadyMerged, dirtinessCache) ? parent : null;
                }
            }
            if (collapseTarget != null) {
                collapsed.add(collapseTarget);
            }
            else {
                collapsed.addAll(group);
            }
        }
        return new ArrayList<>(collapsed);
    }

    private static boolean canCollapseStrictly(
        Path parent,
        Map<@Nullable Path, List<Path>> byParent,
        List<Path> alreadyMerged,
        DirtinessCache dirtinessCache
    ) {
        // any other leftover group sitting in a subdirectory of parent would have its classpath
        // name change if we collapsed here, so refuse. Cheap O(P) over the byParent keyset
        // instead of O(N) over allResourceFiles.
        boolean hasNestedLeftoverGroup = byParent.keySet().stream().anyMatch(otherParent ->
            otherParent != null && !otherParent.equals(parent) && otherParent.startsWith(parent)
        );
        return !hasNestedLeftoverGroup && canCollapseToPath(parent, alreadyMerged, dirtinessCache);
    }

    /**
     * Aggressive mode: walk up from the leftover's immediate parent toward the ceiling and pick the
     * highest path that still satisfies cleanliness (see DirtinessCache) / no-overlap / no-source-nesting.
     */
    private static @Nullable Path findHighestCollapseAncestor(
        Path immediateParent,
        Path ceiling,
        List<Path> alreadyMerged,
        DirtinessCache dirtinessCache
    ) {
        if (!immediateParent.startsWith(ceiling)) {
            return null;
        }
        Path best = null;
        Path candidate = immediateParent;
        while (candidate != null && candidate.startsWith(ceiling)) {
            if (!canCollapseToPath(candidate, alreadyMerged, dirtinessCache)) {
                break;
            }
            best = candidate;
            if (candidate.equals(ceiling)) {
                break;
            }
            candidate = candidate.getParent();
        }
        return best;
    }

    private static boolean canCollapseToPath(
        Path parent,
        List<Path> alreadyMerged,
        DirtinessCache dirtinessCache
    ) {
        // no source-content-root rejection: a resource root nested inside a source content root is
        // fine because we set `relativeOutputPath` on the resulting root so its files keep the same
        // FQN they would have had through the enclosing strip prefix.
        boolean overlapsMerged = alreadyMerged.stream().anyMatch(it -> it.startsWith(parent) || parent.startsWith(it));
        return !overlapsMerged && !dirtinessCache.isDirty(parent);
    }

    private static class DirtinessCache {
        private final Set<Path> myAllResourceFiles;
        private final BazelConvenienceSymlinksScanner mySymlinksScanner;
        private final HashMap<VirtualFile, Boolean> myCache = new HashMap<>();

        DirtinessCache(Set<Path> allResourceFiles, BazelConvenienceSymlinksScanner symlinksScanner) {
            myAllResourceFiles = allResourceFiles;
            mySymlinksScanner = symlinksScanner;
        }

        boolean isDirty(Path path) {
            VirtualFile file = PathUtil.findVirtualFile(path);
            if (file == null) {
                file = PathUtil.refreshAndFindVirtualFile(path);
            }
            if (file == null) {
                log.warn("Resource path does not exist, skipping dirtiness check: " + path);
                return false;
            }
            return isDirty(file);
        }

        private boolean isDirty(VirtualFile file) {
            Boolean cached = myCache.get(file);
            if (cached != null) {
                return cached;
            }
            // consulo: getOrPut computes the value before storing it
            boolean value = containsExtraFilesIgnoringBazelSymlinks(file);
            myCache.put(file, value);
            return value;
        }

        private boolean containsExtraFilesIgnoringBazelSymlinks(VirtualFile receiver) {
            if (!receiver.isDirectory()) {
                return !myAllResourceFiles.contains(receiver.toNioPath());
            }
            VirtualFile[] children = receiver.getChildren();
            if (children == null) {
                return false;
            }
            // symlinked directories are never walked
            for (VirtualFile child : children) {
                boolean dirty;
                if (child.is(VFileProperty.SYMLINK)) {
                    dirty = isDirtySymlink(child);
                }
                else if (child.isDirectory()) {
                    dirty = isDirty(child); // recursion goes through the memoizing cache
                }
                else {
                    dirty = !myAllResourceFiles.contains(child.toNioPath());
                }
                if (dirty) {
                    return true;
                }
            }
            return false;
        }

        private boolean isDirtySymlink(VirtualFile receiver) {
            if (mySymlinksScanner.isBazelSymlink(receiver)) {
                return false;
            }
            else if (receiver.isDirectory()) {
                return true; // symlinked directory: foreign content, dirty without descending
            }
            else {
                return !myAllResourceFiles.contains(receiver.toNioPath()); // symlink to a file (or broken): treat like a regular file entry
            }
        }
    }

    // here we have two distinct cases:
    //  - candidate is located under OWNING target package, then use it as celling
    //  - candidate is outside OWNING target package, then target is probably a filegroup
    //    in this case treat closest bazel package as collapse celling, generally safe
    // by widening scope of resource file merging we prevent creating single file resource content roots per
    // resource file which with big enough resource file set can cause OOM
    private static @Nullable Path collapseCeiling(Path parent, @Nullable Path baseDirectory, Path workspaceRoot) {
        if (baseDirectory != null && parent.startsWith(baseDirectory)) {
            return baseDirectory;
        }
        else {
            return findClosestOwningPackage(parent, workspaceRoot);
        }
    }

    private static @Nullable Path findClosestOwningPackage(Path path, Path workspaceRoot) {
        Path next = path;
        while (next != null && next.startsWith(workspaceRoot)) {
            Path candidate = next;

            // hit celling, bail
            if (candidate.equals(workspaceRoot)) {
                return null;
            }

            // nearest package found
            for (String it : Constants.BUILD_FILE_NAMES) {
                if (Files.isRegularFile(candidate.resolve(it))) {
                    return candidate;
                }
            }

            next = candidate.getParent();
        }
        return null;
    }

    private static MergeResult mergeUsing(MergeResult receiver, Path stripPrefix, DirtinessCache dirtinessCache) {
        Set<Path> stripPrefixAncestors = Set.of(stripPrefix);
        Set<Path> newLeftovers = new LinkedHashSet<>();
        for (Path it : receiver.leftovers()) {
            if (!PathUtil.isUnder(it, stripPrefixAncestors)) {
                newLeftovers.add(it);
            }
        }
        if (receiver.leftovers().size() == newLeftovers.size()) {
            return receiver;
        }
        if (dirtinessCache.isDirty(stripPrefix)) {
            return receiver;
        }
        List<Path> merged = new ArrayList<>(receiver.merged());
        merged.add(stripPrefix);
        return new MergeResult(merged, newLeftovers);
    }

    private static @Nullable Set<Path> extractStripPrefixOrNull(BuildTarget target, Function<OutputLocation, @Nullable Path> resolveLocation) {
        JvmBuildTarget jvmBuildTarget = JvmBuildTarget.extractJvmBuildTarget(target);
        OutputLocation stripPrefix = jvmBuildTarget != null ? jvmBuildTarget.resolvedResourceStripPrefix() : null;
        Path resolved = stripPrefix != null ? resolveLocation.apply(stripPrefix) : null;
        return resolved != null ? Set.of(resolved) : null;
    }

    private static Set<Path> defaultStripPrefixes(
        BuildTarget target,
        List<Path> resources,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        Path workspaceRoot
    ) {
        if (JvmBuildTarget.extractKotlinBuildTarget(target) != null) {
            return defaultStripPrefixesKotlin(target, resolveLocation, workspaceRoot);
        }
        else if (JvmBuildTarget.extractScalaBuildTarget(target) != null) {
            return defaultStripPrefixesScala(target, resources, resolveLocation, workspaceRoot);
        }
        else if (JvmBuildTarget.extractJvmBuildTarget(target) != null) {
            return defaultStripPrefixesJava(target, resolveLocation, workspaceRoot);
        }
        else {
            return Set.of();
        }
    }

    /**
     * Returns the strip prefixes that `rules_java` uses when `resource_strip_prefix` is not set.
     * Each prefix is the resource root that {@link BazelConventionalRoots#javaResourceRootIndex} finds.
     */
    private static Set<Path> defaultStripPrefixesJava(
        BuildTarget target,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        Path workspaceRoot
    ) {
        return stripPrefixes(target, resolveLocation, workspaceRoot, BazelConventionalRoots::javaResourceRootIndex);
    }

    /**
     * Returns the strip prefixes that `rules_kotlin` uses when `resource_strip_prefix` is not set.
     * Each prefix ends with the first marker of {@link #KOTLIN_RESOURCE_MARKERS} that the path contains.
     */
    private static Set<Path> defaultStripPrefixesKotlin(
        BuildTarget target,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        Path workspaceRoot
    ) {
        return stripPrefixes(target, resolveLocation, workspaceRoot, it -> markerEndIndex(it, KOTLIN_RESOURCE_MARKERS));
    }

    /**
     * Returns the strip prefixes that `rules_scala` uses when `resource_strip_prefix` is not set.
     * Each prefix ends with the first marker of {@link #SCALA_RESOURCE_MARKERS} that the path contains.
     * A resource of an external repository also gets the root of that repository as a prefix.
     */
    private static Set<Path> defaultStripPrefixesScala(
        BuildTarget target,
        List<Path> resources,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        Path workspaceRoot
    ) {
        Set<Path> externalPrefixes = new LinkedHashSet<>();
        for (Path resource : resources) {
            int index = -1;
            for (int i = 0; i < resource.getNameCount(); i++) {
                if (resource.getName(i).toString().equals("external")) {
                    index = i;
                    break;
                }
            }
            int next = index + 1;
            if (index == -1) {
                continue;
            }
            if (next >= resource.getNameCount()) {
                continue;
            }
            externalPrefixes.add(rootSubpath(resource, next + 1));
        }
        Set<Path> conventionalPrefixes =
            stripPrefixes(target, resolveLocation, workspaceRoot, it -> markerEndIndex(it, SCALA_RESOURCE_MARKERS));
        Set<Path> result = new LinkedHashSet<>(externalPrefixes);
        result.addAll(conventionalPrefixes);
        return result;
    }

    /**
     * Returns a strip prefix for each resource of {@code target}.
     * {@code rootIndex} gives the index of the resource root in the segments of a path relative to the root of the repository.
     * Bazel uses this relative path, so a directory above the workspace root does not count.
     */
    private static Set<Path> stripPrefixes(
        BuildTarget target,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        Path workspaceRoot,
        Function<List<String>, @Nullable Integer> rootIndex
    ) {
        Set<Path> result = new HashSet<>();
        target.getResources().getOutputLocations().forEach(location -> {
            Path path = resolveLocation.apply(location);
            if (path == null) {
                return;
            }
            Path repoRelativePath = OutputLocation.toRepoRelativePath(location);
            String relativePath = repoRelativePath != null
                ? invariantSeparatorsPathString(repoRelativePath)
                : relativeToRootOrNull(path, workspaceRoot);
            if (relativePath == null) {
                return;
            }
            // a hard link of a generated file has another path, so the prefix is unknown
            if (!path.endsWith(relativePath)) {
                return;
            }
            List<String> segments = Arrays.stream(relativePath.split("/", -1)).filter(it -> !it.isEmpty()).toList();
            Integer index = rootIndex.apply(segments);
            if (index == null) {
                return;
            }
            result.add(rootSubpath(path, path.getNameCount() - (segments.size() - 1 - index)));
        });
        return result;
    }

    private static @Nullable String relativeToRootOrNull(Path receiver, Path root) {
        return receiver.startsWith(root) && !receiver.equals(root) ? invariantSeparatorsPathString(root.relativize(receiver)) : null;
    }

    /**
     * Returns the index of the last segment of the first marker in {@code markers} that the directories of this path contain.
     * The rules check the markers in their order, and each marker can match at any depth.
     * A marker matches whole segments, so `kotlin` does not match `mykotlinlib`.
     */
    private static @Nullable Integer markerEndIndex(List<String> receiver, List<List<String>> markers) {
        List<String> directories = receiver.subList(0, Math.max(receiver.size() - 1, 0));
        for (List<String> marker : markers) {
            // consulo: Kotlin `directories.windowed(marker.size).indexOf(marker)`
            int start = -1;
            for (int i = 0; i + marker.size() <= directories.size(); i++) {
                if (directories.subList(i, i + marker.size()).equals(marker)) {
                    start = i;
                    break;
                }
            }
            if (start >= 0) {
                return start + marker.size() - 1;
            }
        }
        return null;
    }

    // consulo: Kotlin Path.invariantSeparatorsPathString
    private static String invariantSeparatorsPathString(Path path) {
        String separator = path.getFileSystem().getSeparator();
        return separator.equals("/") ? path.toString() : path.toString().replace(separator, "/");
    }

    private static Path rootSubpath(Path receiver, int until) {
        Path root = receiver.getRoot();
        return root != null ? root.resolve(receiver.subpath(0, until)) : receiver.subpath(0, until);
    }

    private record MergeResult(
        List<Path> merged,
        Set<Path> leftovers
    ) {
    }

    private static final boolean DEFAULT_GENERATED = false;
    private static final String DEFAULT_RELATIVE_OUTPUT_PATH = "";

    /**
     * The markers of `_CONVENTIONAL_RESOURCE_PATHS` in
     * <a href="https://github.com/bazelbuild/rules_kotlin/blob/v2.4.21/kotlin/internal/jvm/compile.bzl">compile.bzl</a> of
     * `rules_kotlin`, in its order.
     */
    private static final List<List<String>> KOTLIN_RESOURCE_MARKERS =
        List.of("src/main/java", "src/main/resources", "src/test/java", "src/test/resources", "kotlin").stream()
            .map(it -> List.of(it.split("/", -1)))
            .toList();

    /**
     * The markers of `_target_path_by_default_prefixes` in
     * <a href="https://github.com/bazelbuild/rules_scala/blob/master/scala/private/resources.bzl">resources.bzl</a> of
     * `rules_scala`, in its order.
     */
    private static final List<List<String>> SCALA_RESOURCE_MARKERS = List.of(List.of("resources"), List.of("java"));
}
