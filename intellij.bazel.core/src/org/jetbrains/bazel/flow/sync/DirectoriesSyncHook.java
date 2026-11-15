package org.jetbrains.bazel.flow.sync;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.flow.exclude.BazelSymlinkExcludeService;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.sync.ProjectSyncHook;
import org.jetbrains.bazel.workspace.indexing.IndexableContent;
import org.jetbrains.bazel.workspace.indexing.IndexableContentCollector;
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * This sync hook does three important things:
 * 1. Creates the WSM entity
 * 2. Supports the `index:` section, see its documentation in
 *    {@link org.jetbrains.bazel.languages.projectview.ProjectViewSectionProvider}.
 * 3. Loads all non-indexable files that happen to be under `directories:` (and not excluded) into the VFS,
 *    so that "Go to file by name" is quicker, see https://youtrack.jetbrains.com/issue/IJPL-207088
 */
// consulo: the IntelliJ workspace model is not used; virtual file urls are VFS url strings and the entity is project data
// of the BazelModelBuilder
@ExtensionImpl
public class DirectoriesSyncHook implements ProjectSyncHook {

    @Override
    public CompletableFuture<Void> onSync(ProjectSyncHook.ProjectSyncHookEnvironment environment) {
        Project project = environment.project();
        return environment.withSubtask(
            LocalizeValue.of("Collect project directories"),
            taskId -> computeProjectDirectories(environment)
        ).thenCompose(directoryRoots -> {
            List<String> indexPatterns = ProjectViewExtensions.getIndex(environment.server().getProjectView());
            return environment.withSubtask(
                LocalizeValue.of("Collect indexable content"),
                taskId -> CompletableFuture.completedFuture(computeIndexableContent(environment, indexPatterns, directoryRoots))
            ).thenApply(indexableContent -> {
                // consulo: upstream registers the whole project root as workspace content (BazelProjectDirectoriesEntity +
                // its WorkspaceFileIndexContributor). The classic project model has content only in modules: a Bazel-owned
                // root module with the project root as content root (excludes come from BazelProjectDirectoriesExcludePolicy)
                VirtualFile rootDir = org.jetbrains.bazel.config.project.getRootDir(project);
                Module rootModule = environment.diff().getOrCreateModule(rootDir.getName(), rootDir.getPath());
                environment.diff().getRootModel(rootModule).addContentEntry(rootDir);

                environment.diff().putProjectData(
                    BazelProjectDirectoriesEntity.class,
                    new BazelProjectDirectoriesEntity(
                        org.jetbrains.bazel.config.project.getRootDir(project).getUrl(),
                        new ArrayList<>(directoryRoots.included()),
                        new ArrayList<>(directoryRoots.excluded()),
                        indexPatterns,
                        new ArrayList<>(indexableContent.getRecursiveRoots()),
                        new ArrayList<>(indexableContent.getNonRecursiveRoots())
                    )
                );
                return null;
            });
        });
    }

    private record DirectoryRoots(
        Set<String> included,
        Set<String> excluded
    ) {
    }

    private CompletableFuture<DirectoryRoots> computeProjectDirectories(
        ProjectSyncHook.ProjectSyncHookEnvironment environment
    ) {
        return environment
            .server()
            .workspaceDirectories(environment.snapshot().repoMapping(), environment.taskId())
            .thenCompose(directories -> BazelSymlinkExcludeService
                .getInstance(environment.project())
                .scanForBazelSymlinksToExclude(org.jetbrains.bazel.config.project.getRootDir(environment.project()).toNioPath())
                .thenApply(symlinks -> {
                    Set<String> symlinkExcludes = toVirtualUrlSet(symlinks);
                    Set<String> includedRoots = toVirtualUrlSet(directories.includedDirectories());
                    Set<String> excludedRoots = toVirtualUrlSet(directories.excludedDirectories());
                    Set<String> allExcluded = new LinkedHashSet<>(excludedRoots);
                    allExcluded.addAll(symlinkExcludes);
                    return new DirectoryRoots(
                        includedRoots,
                        allExcluded
                    );
                }));
    }

    private IndexableContent computeIndexableContent(
        ProjectSyncHook.ProjectSyncHookEnvironment environment,
        List<String> indexPatterns,
        DirectoryRoots directoryRoots
    ) {
        Set<VirtualFile> contentRoots = new LinkedHashSet<>();
        // consulo: upstream collects the ContentRootEntity urls of the diff storage
        for (ModifiableRootModel rootModel : environment.diff().getRootModels()) {
            for (ContentEntry contentEntry : rootModel.getContentEntries()) {
                VirtualFile file = contentEntry.getFile();
                if (file != null) {
                    contentRoots.add(file);
                }
            }
        }
        IndexableContentCollector collector = new IndexableContentCollector(
            environment.project(),
            indexPatterns,
            toVirtualFiles(directoryRoots.included()),
            toVirtualFiles(directoryRoots.excluded()),
            contentRoots
        );
        return collector.collect();
    }

    private static Set<String> toVirtualUrlSet(Collection<Path> receiver) {
        Set<String> result = new LinkedHashSet<>();
        for (Path path : receiver) {
            result.add(VirtualFileUtil.pathToUrl(path.toString()));
        }
        return result;
    }

    // consulo: upstream VirtualFileUrl::virtualFile
    private static Set<VirtualFile> toVirtualFiles(Collection<String> urls) {
        Set<VirtualFile> result = new LinkedHashSet<>();
        for (String url : urls) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
            if (file != null) {
                result.add(file);
            }
        }
        return result;
    }
}
