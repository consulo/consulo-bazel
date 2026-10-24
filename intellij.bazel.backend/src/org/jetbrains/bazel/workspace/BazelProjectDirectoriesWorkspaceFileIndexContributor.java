package org.jetbrains.bazel.workspace;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.module.content.layer.DirectoryIndexExcludePolicy;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Registers the file sets of {@link BazelProjectDirectoriesEntity}.
 * <p>
 * The project root is non-indexable content.
 * Thus, all the files under it are part of the project, but the IDE does not index them.
 * Indexing of the files other than the sources can be very slow, see https://youtrack.jetbrains.com/issue/BAZEL-2088.
 * <p>
 * {@link org.jetbrains.bazel.workspace.indexing.IndexableContentCollector} selects the files to index in addition to the target sources.
 * These are the files that match the `index` patterns, the Bazel files, the root workspace files and the project view file.
 * They also include the files from {@link org.jetbrains.bazel.workspace.indexing.IndexableContentContributor}.
 * {@code registerIndexableRecursiveRoots} indexes the directories that the patterns match recursively.
 * {@code registerIndexableNonRecursiveRoots} indexes each of the other selected files.
 * <p>
 * consulo: the IntelliJ workspace file index is not available. The indexable roots are contributed with
 * {@link IndexableSetContributor} and the excluded roots with {@link DirectoryIndexExcludePolicy}
 * ({@link BazelProjectDirectoriesExcludePolicy}).
 */
@ExtensionImpl
public class BazelProjectDirectoriesWorkspaceFileIndexContributor extends IndexableSetContributor {
    @Override
    public Set<VirtualFile> getAdditionalProjectRootsToIndex(Project project) {
        BazelProjectDirectoriesEntity entity = BazelProjectModelService.getInstance(project).getProjectData(BazelProjectDirectoriesEntity.class);
        if (entity == null) {
            return Set.of();
        }
        Set<VirtualFile> roots = new LinkedHashSet<>();
        registerIndexableRecursiveRoots(entity, roots);
        registerIndexableNonRecursiveRoots(entity, roots);
        // TODO consulo: upstream registers the project root as non-indexable content (WorkspaceFileKind.CONTENT_NON_INDEXABLE),
        //  the classic project model has no such kind
        return roots;
    }

    @Override
    public Set<VirtualFile> getAdditionalRootsToIndex() {
        return Set.of();
    }

    private static void registerIndexableRecursiveRoots(BazelProjectDirectoriesEntity entity, Set<VirtualFile> roots) {
        for (String it : entity.indexableRecursiveRoots()) {
            addIfExists(it, roots);
        }
    }

    private static void registerIndexableNonRecursiveRoots(BazelProjectDirectoriesEntity entity, Set<VirtualFile> roots) {
        for (String it : entity.indexableNonRecursiveRoots()) {
            addIfExists(it, roots);
        }
    }

    static void addIfExists(String url, Set<VirtualFile> roots) {
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        if (file != null && file.isValid()) {
            roots.add(file);
        }
    }
}

// consulo: excluded roots of BazelProjectDirectoriesEntity (upstream registerExcludedDirectories)
@ExtensionImpl
class BazelProjectDirectoriesExcludePolicy implements DirectoryIndexExcludePolicy {
    private final Project myProject;
    private volatile @Nullable List<String> myLastExcludedRoots;

    @Inject
    public BazelProjectDirectoriesExcludePolicy(Project project) {
        myProject = project;
    }

    @Override
    public VirtualFile[] getExcludeRootsForProject() {
        BazelProjectDirectoriesEntity entity = BazelProjectModelService.getInstance(myProject).getProjectData(BazelProjectDirectoriesEntity.class);
        if (entity == null) {
            return VirtualFile.EMPTY_ARRAY;
        }
        registerExcludedDirectories(entity);
        Set<VirtualFile> roots = new LinkedHashSet<>();
        for (String it : entity.excludedRoots()) {
            BazelProjectDirectoriesWorkspaceFileIndexContributor.addIfExists(it, roots);
        }
        return roots.toArray(VirtualFile.EMPTY_ARRAY);
    }

    @Override
    public VirtualFilePointer[] getExcludeRootsForModule(ModuleRootLayer rootModel) {
        return VirtualFilePointer.EMPTY_ARRAY;
    }

    private void registerExcludedDirectories(BazelProjectDirectoriesEntity entity) {
        // consulo: upstream excludes the symlinks once per registration of the entity
        if (Objects.equals(myLastExcludedRoots, entity.excludedRoots())) {
            return;
        }
        myLastExcludedRoots = entity.excludedRoots();
        List<Path> paths = new ArrayList<>();
        for (String it : entity.excludedRoots()) {
            paths.add(Path.of(VirtualFileUtil.urlToPath(it)));
        }
        ExcludeSymlinksFromFileWatcher.excludeSymlinksFromFileWatcher(paths);
    }
}
