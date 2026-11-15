package org.jetbrains.bazel.workspace.importer;

import consulo.bazel.workspace.BazelModelBuilder;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.GeneratedContentFolderPropertyProvider;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.ProductionResourceContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.language.content.TestResourceContentFolderTypeProvider;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.ModifiableRootModel;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmPackageNameParser;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget;
import org.jetbrains.bazel.workspace.ProjectViewGlobSet;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

// RC: replaces `SourcesItemToJavaSourceRootTransformer` + `SourceItemToSourceRootTransformer` +
// `JavaSourceEntityUpdater` + the source-side of `ContentRootEntityUpdater`;
// the `JavaSourceRoot` / `ContentRoot` / `GenericSourceRoot` wrappers are dropped
public final class SourceRootBuilder {
    // consulo: upstream SourceRootTypeId("java-source") etc. -> the id of the Consulo ContentFolderTypeProvider
    // (ContentFolderTypeProvider.byId), which is what WorkspaceModelTargetSourceRootTypeId stores
    public static final String JAVA_SOURCE_ROOT_TYPE = "PRODUCTION";

    public static final String JAVA_TEST_SOURCE_ROOT_TYPE = "TEST";

    public static final String JAVA_RESOURCE_ROOT_TYPE = "PRODUCTION_RESOURCE";

    public static final String JAVA_TEST_RESOURCE_ROOT_TYPE = "TEST_RESOURCE";

    private SourceRootBuilder() {
    }

    public record ResolvedSourceRoot(
        Path sourcePath,
        boolean generated,
        String rootType
    ) {
    }

    public static List<ResolvedSourceRoot> resolve(
        BuildTarget target,
        ProjectViewGlobSet testSourcesGlob,
        Function<OutputLocation, @Nullable Path> resolveLocation
    ) {
        // KSP is not a compiler plugin, it uses kotlin analysis API to analyze
        // user code and expose results using KSP API then generate code (that's why it's separate bazel actions),
        // the issue is that rules_kotlin treat KSP outputs in the same way as normal compiler artifacts.
        // Correct way of representing ksp sources is to put then side-by-side to real sources
        // that allow our plugin to correctly handle things like two-way references, or access to internal members.
        KotlinBuildTarget kotlinBuildTarget = BuildTarget.extractData(target, KotlinBuildTarget.class);
        OutputLocationCollection kspSrcJars = kotlinBuildTarget != null ? kotlinBuildTarget.kspSourceJars() : OutputLocationCollection.EMPTY;

        List<ResolvedSourceRoot> result = new ArrayList<>();
        target.getSources().getOutputLocations().forEach(location -> {
            Path path = resolveLocation.apply(location);
            if (path != null) {
                result.add(convert(path, OutputLocation.isGenerated(location), target, testSourcesGlob));
            }
        });
        for (Path it : JvmTargetEntitiesBuilder.resolvePaths(kspSrcJars, resolveLocation)) {
            result.add(convert(it, true, target, testSourcesGlob));
        }
        return result;
    }

    // consulo: upstream local `fun Path.convert(generated: Boolean)` inside resolve()
    private static ResolvedSourceRoot convert(Path receiver, boolean generated, BuildTarget target, ProjectViewGlobSet testSourcesGlob) {
        String rootType;
        if (WorkspaceTarget.isTestTarget(target)) {
            rootType = JAVA_TEST_SOURCE_ROOT_TYPE;
        }
        else if (testSourcesGlob.matches(receiver)) {
            rootType = JAVA_TEST_SOURCE_ROOT_TYPE;
        }
        else {
            rootType = JAVA_SOURCE_ROOT_TYPE;
        }
        return new ResolvedSourceRoot(receiver, generated, rootType);
    }

    /**
     * writes a (ContentRootEntity, SourceRootEntity, JavaSourceRootPropertiesEntity) triple per source root.
     * <p>
     * if {@code singleContentRoot} is true (the hirschgarten workspacemodel-module special case), a single content
     * root is created at the parent directory of the first source root and shared by all source roots.
     * <p>
     * consulo: a content entry with source folders is added to the module root model of {@code builder}
     */
    public static void write(
        List<ResolvedSourceRoot> sourceRoots,
        Module parentModuleEntity,
        Path projectBasePath,
        BazelModelBuilder storage
    ) {
        if (sourceRoots.isEmpty()) {
            return;
        }

        Map<Path, List<ResolvedSourceRoot>> groups = new LinkedHashMap<>();
        Set<Path> packageRoots = new HashSet<>();
        for (ResolvedSourceRoot original : sourceRoots) {
            // consulo: see toPackageRoot; a package root is its own content root and is written once
            ResolvedSourceRoot root = toPackageRoot(original);
            if (root != original) {
                if (packageRoots.add(root.sourcePath())) {
                    groups.computeIfAbsent(root.sourcePath(), it -> new ArrayList<>()).add(root);
                }
                continue;
            }
            Path key;
            if (BazelFeatureFlags.getMergeSourceRoots() || // sources were merged earlier, now we just need to create a source root for each path
                Objects.equals(root.sourcePath().getParent(), projectBasePath)) { // don't create a content root for project root dir to avoid excessive indexing
                key = root.sourcePath();
            }
            else {
                key = root.sourcePath().getParent();
            }
            groups.computeIfAbsent(key, it -> new ArrayList<>()).add(root);
        }

        ModifiableRootModel model = storage.getRootModel(parentModuleEntity);
        for (Map.Entry<Path, List<ResolvedSourceRoot>> entry : groups.entrySet()) {
            Path commonParentDir = entry.getKey();
            // consulo: a content entry must contain its folders, so a source jar which is its own content root
            // gets the jar url (upstream uses the file url of the jar)
            String contentRootUrl = "jar".equals(BuilderUtils.extension(commonParentDir))
                ? BuilderUtils.toResolvedVirtualFileUrl(BuilderUtils.toJarUrlString(commonParentDir))
                : BuilderUtils.toResolvedVirtualFileUrl(commonParentDir);
            ContentEntry contentEntry = model.addContentEntry(contentRootUrl);
            // consulo: upstream excludedPatterns = emptyList()
            for (ResolvedSourceRoot it : entry.getValue()) {
                sourceRootEntity(it, contentEntry);
            }
        }
    }

    /**
     * consulo: upstream marks a source root which is not a jar with {@code packageMatchesDirectory = false}: Bazel does not require
     * that a directory under a source root corresponds to a JVM package, so IntelliJ takes the package of a file from its
     * {@code package} statement. Consulo has neither that property nor a package prefix, and derives the package from the directory.
     * So a root whose directory ends with the package that its files declare (e.g. {@code src/main/java/com/example/app} with
     * {@code package com.example.app}) is moved up to the package root ({@code src/main/java}). Any other root is kept as is.
     * <p>
     * TODO consulo: a directory whose layout does not match the package of its files can't be expressed, and targets sharing
     *   one package root (e.g. two targets under the same src/main/java) get the same content root
     */
    private static ResolvedSourceRoot toPackageRoot(ResolvedSourceRoot sourceRoot) {
        Path sourcePath = sourceRoot.sourcePath();
        if ("jar".equals(BuilderUtils.extension(sourcePath))) {
            return sourceRoot;
        }
        Path directory;
        Path sourceFile;
        if (Files.isRegularFile(sourcePath)) {
            directory = sourcePath.getParent();
            sourceFile = sourcePath;
        }
        else {
            directory = sourcePath;
            sourceFile = findJvmSourceFile(sourcePath);
        }
        if (directory == null || sourceFile == null) {
            return sourceRoot;
        }
        String packageName = JvmPackageNameParser.findPackage(sourceFile, "scala".equals(BuilderUtils.extension(sourceFile)));
        if (packageName == null || packageName.isEmpty()) {
            return Files.isRegularFile(sourcePath) ? new ResolvedSourceRoot(directory, sourceRoot.generated(), sourceRoot.rootType()) : sourceRoot;
        }
        Path packagePath;
        try {
            packagePath = directory.getFileSystem().getPath(packageName.replace('.', '/'));
        }
        catch (InvalidPathException e) {
            return sourceRoot;
        }
        if (!directory.endsWith(packagePath)) {
            return sourceRoot;
        }
        Path packageRoot = directory;
        for (int i = 0; i < packagePath.getNameCount(); i++) {
            packageRoot = packageRoot.getParent();
            if (packageRoot == null) {
                return sourceRoot;
            }
        }
        return new ResolvedSourceRoot(packageRoot, sourceRoot.generated(), sourceRoot.rootType());
    }

    // consulo: the first JVM source file directly in the directory (a merged root contains the files of one package)
    private static @Nullable Path findJvmSourceFile(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children
                .filter(it -> Files.isRegularFile(it) && Constants.JVM_LANGUAGES_EXTENSIONS.contains(BuilderUtils.extension(it)))
                .sorted()
                .findFirst()
                .orElse(null);
        }
        catch (IOException e) {
            return null;
        }
    }

    private static void sourceRootEntity(ResolvedSourceRoot sourceRoot, ContentEntry contentEntry) {
        ContentFolder folder = contentEntry.addFolder(
            BuilderUtils.toResolvedVirtualFileUrl(BuilderUtils.toJarUrlString(sourceRoot.sourcePath())),
            toContentFolderType(sourceRoot.rootType())
        );
        // consulo: JavaSourceRootPropertiesEntity(generated, packagePrefix = "")
        if (sourceRoot.generated()) {
            folder.setPropertyValue(GeneratedContentFolderPropertyProvider.IS_GENERATED, Boolean.TRUE);
        }
        // Bazel does not require that a directory under a source root corresponds to a JVM package. A source root inside a jar keeps
        // the correspondence, because a jar stores a class file at the path which the package name defines.
        // TODO consulo: JavaSourceRootPropertiesEntity.packageMatchesDirectory (= sourcePath is a jar) has no Consulo counterpart
    }

    // consulo: upstream SourceRootTypeId -> Consulo ContentFolderTypeProvider
    static ContentFolderTypeProvider toContentFolderType(String rootType) {
        return switch (rootType) {
            case JAVA_TEST_SOURCE_ROOT_TYPE -> TestContentFolderTypeProvider.getInstance();
            case JAVA_RESOURCE_ROOT_TYPE -> ProductionResourceContentFolderTypeProvider.getInstance();
            case JAVA_TEST_RESOURCE_ROOT_TYPE -> TestResourceContentFolderTypeProvider.getInstance();
            default -> ProductionContentFolderTypeProvider.getInstance();
        };
    }
}
