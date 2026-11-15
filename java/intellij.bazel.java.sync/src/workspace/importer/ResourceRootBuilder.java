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
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
        String bazelProjectName,
        Path workspaceRoot,
        Function<OutputLocation, @Nullable Path> resolveLocation
    ) {
        String rootType = inferRootType(target);
        List<Path> resourceFiles = JvmTargetEntitiesBuilder.resolvePaths(target.getResources(), resolveLocation);
        if (resourceFiles.isEmpty()) {
            return List.of();
        }
        Set<Path> extractedStripPrefix = extractStripPrefixOrNull(target, resolveLocation);
        Set<Path> stripPrefixes = extractedStripPrefix != null ? extractedStripPrefix : defaultStripPrefixes(target, resourceFiles);
        Set<Path> resourcesSet = new LinkedHashSet<>(resourceFiles);
        DirtinessCache dirtinessCache = new DirtinessCache(resourcesSet, bazelProjectName);
        MergeResult result = new MergeResult(List.of(), resourcesSet);
        for (Path prefix : stripPrefixes) {
            result = mergeUsing(result, prefix, dirtinessCache);
        }
        List<Path> leftoverPaths = collapseLeftoversByTopology(
            result.leftovers(),
            result.merged(),
            parent -> collapseCeiling(parent, baseDirectory, workspaceRoot),
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
        private final String myBazelProjectName;
        private final HashMap<VirtualFile, Boolean> myCache = new HashMap<>();

        DirtinessCache(Set<Path> allResourceFiles, String bazelProjectName) {
            myAllResourceFiles = allResourceFiles;
            myBazelProjectName = bazelProjectName;
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
            if (BazelSymlinksCalculator.isBazelSymlink(myBazelProjectName, receiver)) {
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

    private static Set<Path> defaultStripPrefixes(BuildTarget target, List<Path> resources) {
        if (JvmBuildTarget.extractKotlinBuildTarget(target) != null) {
            return defaultStripPrefixesKotlin(resources);
        }
        else if (JvmBuildTarget.extractScalaBuildTarget(target) != null) {
            return defaultStripPrefixesScala(resources);
        }
        else if (JvmBuildTarget.extractJvmBuildTarget(target) != null) {
            return defaultStripPrefixesJava(resources);
        }
        else {
            return Set.of();
        }
    }

    private static Set<Path> defaultStripPrefixesJava(List<Path> resources) {
        Set<Path> result = new LinkedHashSet<>();
        for (Path it : resources) {
            Path prefix = takeSrcResourcesPrefixOrNull(it);
            if (prefix == null) {
                prefix = takeJavaLayoutPrefixOrNull(it);
            }
            if (prefix != null) {
                result.add(prefix);
            }
        }
        return result;
    }

    private static Set<Path> defaultStripPrefixesKotlin(List<Path> resources) {
        Set<Path> result = new LinkedHashSet<>();
        for (Path it : kotlinConventionalSegments) {
            result.addAll(findPrefixesEndingWith(resources, it));
        }
        return result;
    }

    private static Set<Path> defaultStripPrefixesScala(List<Path> resources) {
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
        Set<Path> conventionalPrefixes = new LinkedHashSet<>();
        for (Path it : scalaConventionalSegments) {
            conventionalPrefixes.addAll(findPrefixesEndingWith(resources, it));
        }
        Set<Path> result = new LinkedHashSet<>(externalPrefixes);
        result.addAll(conventionalPrefixes);
        return result;
    }

    private static @Nullable Path takeSrcResourcesPrefixOrNull(Path receiver) {
        for (int i = 0; i < receiver.getNameCount() - 2; i++) {
            if (receiver.getName(i).toString().equals("src") && receiver.getName(i + 2).toString().equals("resources")) {
                return rootSubpath(receiver, i + 3);
            }
        }
        return null;
    }

    private static @Nullable Path takeJavaLayoutPrefixOrNull(Path receiver) {
        for (int segmentIndex = 0; segmentIndex < receiver.getNameCount(); segmentIndex++) {
            String segment = receiver.getName(segmentIndex).toString();
            switch (segment) {
                case "javatests", "testsrc", "java" -> {
                    Path srcJava = takeSrcJavaOrNull(receiver, segmentIndex);
                    return srcJava != null ? srcJava : rootSubpath(receiver, segmentIndex + 1);
                }
                case "src" -> {
                    for (int subSegmentIndex = segmentIndex + 1; subSegmentIndex < receiver.getNameCount() - 1; subSegmentIndex++) {
                        String subSegment = receiver.getName(subSegmentIndex).toString();
                        if (subSegment.equals("src") || subSegment.equals("java") || subSegment.equals("javatests")) {
                            String packageCandidate = receiver.getName(subSegmentIndex + 1).toString();
                            if (javaCommonPackagePrefixes.contains(packageCandidate)) {
                                return rootSubpath(receiver, subSegmentIndex + 1);
                            }
                            Path srcJava = takeSrcJavaOrNull(receiver, segmentIndex);
                            return srcJava != null ? srcJava : rootSubpath(receiver, segmentIndex + 1);
                        }
                    }
                    Path srcJava = takeSrcJavaOrNull(receiver, segmentIndex);
                    return srcJava != null ? srcJava : rootSubpath(receiver, segmentIndex + 1);
                }
                default -> {
                }
            }
        }
        return null;
    }

    private static @Nullable Path takeSrcJavaOrNull(Path receiver, int at) {
        if (receiver.getNameCount() <= at + 2) {
            return null;
        }
        if (!receiver.getName(at).toString().equals("src")) {
            return null;
        }
        if (!receiver.getName(at + 1).toString().equals("main") && !receiver.getName(at + 1).toString().equals("test")) {
            return null;
        }
        if (receiver.getName(at + 2).toString().equals("java")) {
            return rootSubpath(receiver, at + 3);
        }
        return null;
    }

    private static Set<Path> findPrefixesEndingWith(List<Path> receiver, Path path) {
        Set<Path> result = new LinkedHashSet<>();
        for (Path it : receiver) {
            Path prefix = takePrefixEndingWithOrNull(it, path);
            if (prefix != null) {
                result.add(prefix);
            }
        }
        return result;
    }

    private static @Nullable Path takePrefixEndingWithOrNull(Path receiver, Path suffix) {
        if (suffix.isAbsolute()) {
            throw new IllegalArgumentException("Failed requirement.");
        }
        if (suffix.getNameCount() < 1) {
            return null;
        }
        Path first = suffix.getName(0);
        // consulo: Kotlin `Iterable<Path>.indexOf(first)` over the name elements
        int index = -1;
        for (int i = 0; i < receiver.getNameCount(); i++) {
            if (receiver.getName(i).equals(first)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return null;
        }
        for (int i = 1; i < suffix.getNameCount(); i++) {
            if (index + i >= receiver.getNameCount()) {
                return null;
            }
            if (!receiver.getName(index + i).equals(suffix.getName(i))) {
                return null;
            }
        }
        return rootSubpath(receiver, index + suffix.getNameCount());
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

    private static final Set<String> javaCommonPackagePrefixes = Set.of("com", "org", "net");

    private static final List<Path> kotlinConventionalSegments = List.of(
        Path.of("src/main/java"),
        Path.of("src/main/resources"),
        Path.of("src/test/java"),
        Path.of("src/test/resources"),
        Path.of("kotlin")
    );

    private static final List<Path> scalaConventionalSegments = List.of(
        Path.of("resources"),
        Path.of("java")
    );
}
