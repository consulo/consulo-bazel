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
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget;
import org.jetbrains.bazel.workspace.ProjectViewGlobSet;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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
        for (ResolvedSourceRoot root : sourceRoots) {
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
