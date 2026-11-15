// consulo: package org.jetbrains.bazel.workspace is also a package of consulo.bazel.base (JPMS split package)
package org.jetbrains.bazel.workspace.java;

import consulo.bazel.workspace.BazelProjectModelService;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.workspace.importer.CompiledSourceCodeInsideJarExcludeBuilder.CompiledSourceFileIndex;
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity;
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity.CompiledSourceCodeInsideJarExcludeId;
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity.LibraryCompiledSourceCodeInsideJarExcludeEntity;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * There's some cases where we don't want to index certain files inside a jar, either because of performance considerations or to
 * prevent incorrect resolve. CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor adds conditional excludes for such files.
 * <p>
 * 1. Oftentimes, jars can contain resources, e.g., XML/HTML/Javascript/etc.
 * We don't index resources inside jars in the following cases (by using {@link #JVM_EXTENSIONS} as a filter):
 * 1. It is an "internal jar", i.e., an output jar of an internal target. Here "internal" usually refers to
 * the "main workspace" (i.e., a label that starts with "//"), but can also be something like "@community//" in the case of Ultimate.
 * Because internal jars are built from source files that we already have added to the project,
 * resolve should use those local files (be it XML or HTML or whatever) anyway. By skipping those resources during indexing we index less and save time.
 * <p>
 * *Note:* for external libraries there exist valid cases where resources DO need to be indexed inside jars. E.g., pre-built IntelliJ plugins
 * contain xml files that need to be indexed in order for the XML references to resolve properly.
 * 2. In source jars. Source jars are supposed to contain, well, source code, e.g., `.java`/`.kt` files.
 * Still, source jars can contain HTML/JS/other junk that slows down indexing for no good reason.
 * 2. We don't index `.class` and `.java`/`.kt` files inside jars if there's already a source file in the project with the same
 * fully-qualified class name. `CompiledSourceCodeInsideJarExcludeBuilder` collects the source files, and {@link CompiledSourceFileIndex}
 * approximates the package of a file inside a jar by its relative path. E.g., if there's a source file `.../com/example/Example.java`,
 * then we will skip `*.jar!/com/example/Example.class` during indexing.
 * This is done mainly to prevent resolve from navigating into jars instead of source code (see https://youtrack.jetbrains.com/issue/BAZEL-1672),
 * but this also helps with indexing performance.
 * <p>
 * TODO consulo: WorkspaceFileIndexContributor and WorkspaceFileSetRegistrar.registerExclusionCondition are missing. Consulo can exclude
 *  whole roots (DirectoryIndexExcludePolicy) but has no public API to exclude files inside library roots by a condition, so this
 *  contributor is not registered (upstream: workspaceModel.fileIndexContributor). The file sets are computed by
 *  {@link #registerFileSets} from the data published by the importer, the registrar is a plain callback.
 */
public class CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor {
    /**
     * Don't index irrelevant files inside jars that are built from internal targets.
     * We care about generated classes (and respective generated sources), but not about generated resources.
     */
    private static final Set<String> JVM_EXTENSIONS;

    static {
        Set<String> extensions = new LinkedHashSet<>(Set.of("class", "tasty"));
        extensions.addAll(JavaLanguageClass.JAVA.recognizedFilenameExtensions());
        extensions.addAll(JavaLanguageClass.SCALA.recognizedFilenameExtensions());
        extensions.addAll(JavaLanguageClass.KOTLIN.recognizedFilenameExtensions());
        JVM_EXTENSIONS = Set.copyOf(extensions);
    }

    // consulo: upstream `entityClass`
    public Class<LibraryCompiledSourceCodeInsideJarExcludeEntity> getEntityClass() {
        return LibraryCompiledSourceCodeInsideJarExcludeEntity.class;
    }

    private final ConcurrentMap<CompiledSourceCodeInsideJarExcludeId, CompiledSourceFileIndex> mySourceFileIndexes =
        ContainerUtil.createConcurrentWeakMap();

    /**
     * consulo: {@code registrar} replaces {@code WorkspaceFileSetRegistrar.registerExclusionCondition(root, condition, entity)},
     * the {@code storage} lookups go through {@link BazelProjectModelService} and the project library table
     */
    public void registerFileSets(
        LibraryCompiledSourceCodeInsideJarExcludeEntity entity,
        BiConsumer<String, Predicate<VirtualFile>> registrar,
        Project project
    ) {
        Library library = ProjectLibraryTable.getInstance(project).getLibraryByName(entity.libraryId());
        if (library == null) {
            return;
        }
        CompiledSourceCodeInsideJarExcludeEntity compiledSourceCodeInsideJarExcludeEntity =
            BazelProjectModelService.getInstance(project).getProjectData(CompiledSourceCodeInsideJarExcludeEntity.class);
        if (compiledSourceCodeInsideJarExcludeEntity == null ||
            !compiledSourceCodeInsideJarExcludeEntity.getSymbolicId().equals(entity.compiledSourceCodeInsideJarExcludeId())) {
            return;
        }

        CompiledSourceCodeInsideJarExcludeId excludeId = compiledSourceCodeInsideJarExcludeEntity.excludeId();
        CompiledSourceFileIndex sourceFileIndex = mySourceFileIndexes.computeIfAbsent(
            excludeId,
            it -> new CompiledSourceFileIndex(compiledSourceCodeInsideJarExcludeEntity.relativePathsInsideJarToExclude())
        );
        Set<String> librariesFromInternalTargetsUrls = compiledSourceCodeInsideJarExcludeEntity.librariesFromInternalTargetsUrls();
        int entityId = compiledSourceCodeInsideJarExcludeEntity.excludeId().id();
        InternalTargetsJarExclusionCondition internalTargetsExclusionCondition =
            new InternalTargetsJarExclusionCondition(sourceFileIndex, librariesFromInternalTargetsUrls, entityId);

        // consulo: library.roots -> the urls of the binaries and sources roots
        for (String contentRootUrl : library.getUrls(BinariesOrderRootType.ID)) {
            registrar.accept(contentRootUrl, internalTargetsExclusionCondition);
        }
        for (String contentRootUrl : library.getUrls(SourcesOrderRootType.ID)) {
            registrar.accept(contentRootUrl, internalTargetsExclusionCondition);

            // libraryRoot.type == LibraryRootTypeId.SOURCES
            registrar.accept(contentRootUrl, NonJvmExtensionExclusionCondition.INSTANCE);
        }
    }

    private static class InternalTargetsJarExclusionCondition implements Predicate<VirtualFile> {
        private final CompiledSourceFileIndex mySourceFileIndex;
        private final Set<String> myLibrariesFromInternalTargetsUrls;
        private final int myEntityId;

        InternalTargetsJarExclusionCondition(
            CompiledSourceFileIndex sourceFileIndex,
            Set<String> librariesFromInternalTargetsUrls,
            int entityId
        ) {
            mySourceFileIndex = sourceFileIndex;
            myLibrariesFromInternalTargetsUrls = librariesFromInternalTargetsUrls;
            myEntityId = entityId;
        }

        public boolean shouldExclude(VirtualFile file) {
            if (file.isDirectory()) {
                return false;
            }

            VirtualFile rootFile = VirtualFileUtil.getRootFile(file);
            if (!myLibrariesFromInternalTargetsUrls.contains(rootFile.getUrl())) {
                return false;
            }

            if (!hasJvmExtension(file)) {
                return true;
            }

            String relativePath = getRelativePathInsideJar(file, rootFile);
            String relativePathWithoutNestedClass = removeNestedClass(relativePath);
            return mySourceFileIndex.hasSourceFor(relativePathWithoutNestedClass);
        }

        @Override
        public boolean test(VirtualFile file) {
            return shouldExclude(file);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            InternalTargetsJarExclusionCondition that = (InternalTargetsJarExclusionCondition) other;

            return myEntityId == that.myEntityId;
        }

        @Override
        public int hashCode() {
            return myEntityId;
        }
    }

    private static final class NonJvmExtensionExclusionCondition implements Predicate<VirtualFile> {
        static final NonJvmExtensionExclusionCondition INSTANCE = new NonJvmExtensionExclusionCondition();

        private NonJvmExtensionExclusionCondition() {
        }

        public boolean shouldExclude(VirtualFile file) {
            return !file.isDirectory() && !hasJvmExtension(file);
        }

        @Override
        public boolean test(VirtualFile file) {
            return shouldExclude(file);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other == this;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    // consulo: Kotlin `file.extension !in JVM_EXTENSIONS`, a file without an extension is not a JVM file
    private static boolean hasJvmExtension(VirtualFile file) {
        String extension = file.getExtension();
        return extension != null && JVM_EXTENSIONS.contains(extension);
    }

    /**
     * Based on `ArchiveFileSystem#getRelativePath`
     */
    private static String getRelativePathInsideJar(VirtualFile receiver, VirtualFile rootFile) {
        String relativePath = receiver.getPath().substring(rootFile.getPath().length());
        return StringUtil.trimLeading(relativePath, '/');
    }

    /**
     * example/lib.jar!/Example$1.class -> example/lib.jar!/Example.class
     */
    private static String removeNestedClass(String relativePath) {
        int dollarIndex = relativePath.indexOf('$');
        if (dollarIndex == -1) {
            return relativePath;
        }
        int dotIndex = relativePath.indexOf('.', dollarIndex + 1);
        if (dotIndex == -1) {
            return relativePath;
        }
        return relativePath.substring(0, dollarIndex) + relativePath.substring(dotIndex);
    }
}
