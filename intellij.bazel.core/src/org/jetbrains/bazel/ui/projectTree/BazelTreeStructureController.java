package org.jetbrains.bazel.ui.projectTree;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.workspace.BazelProjectDirectoriesUtils;
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.EXCLUDED;
import static org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.ROOT;
import static org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.UNIMPORTED;

// consulo: the IntelliJ workspace model is not used: BazelProjectDirectoriesEntity is project data of BazelProjectModelService,
// and the CachedValue over the entity storage is a CachedValue that depends on the project roots
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelTreeStructureController {
    private final Project myProject;

    @Inject
    public BazelTreeStructureController(Project project) {
        myProject = project;
    }

    public boolean shouldShowNode(BazelTreeNodeType bazelTreeNodeType) {
        return measure("BazelTreeStructureController.shouldShowNode." + bazelTreeNodeType, () -> {
            BazelTreeStructureContext context = getOrComputeBazelTreeStructureContext();
            return switch (bazelTreeNodeType) {
                case ROOT -> true;
                case EXCLUDED -> context.projectImported() && nodeHasAnyDirectories(context, EXCLUDED);
                case UNIMPORTED -> context.projectImported() && nodeHasAnyDirectories(context, UNIMPORTED);
            };
        });
    }

    private boolean nodeHasAnyDirectories(BazelTreeStructureContext context, BazelTreeNodeType bazelTreeNodeType) {
        VirtualFile rootProjectDir = context.rootProjectDir();
        if (rootProjectDir == null) {
            return false;
        }
        List<VirtualFile> directories = ReadAction.compute(() ->
            Arrays.stream(rootProjectDir.getChildren()).filter(VirtualFile::isDirectory).toList()
        );
        return directories.stream().anyMatch(it -> shouldShowDirectoryUnderTreeNode(context, it, bazelTreeNodeType));
    }

    public boolean shouldShowUnderTreeNode(VirtualFile file, BazelTreeNodeType bazelTreeNodeType) {
        return measure("BazelTreeStructureController.shouldShowUnderTreeNode." + bazelTreeNodeType, () -> {
            BazelTreeStructureContext context = getOrComputeBazelTreeStructureContext();
            if (!context.projectImported()) {
                // When project isn't imported, we don't have information to determine the correct node,
                // so we put everything under the root node
                return bazelTreeNodeType == ROOT;
            }

            if (file.isDirectory()) {
                return shouldShowDirectoryUnderTreeNode(context, file, bazelTreeNodeType);
            }
            else {
                return shouldShowFileUnderTreeNode(context, file, bazelTreeNodeType);
            }
        });
    }

    private boolean shouldShowFileUnderTreeNode(BazelTreeStructureContext context, VirtualFile file, BazelTreeNodeType bazelTreeNodeType) {
        VirtualFile parent = file.getParent();
        if (parent == null) {
            return false;
        }
        if (parent.equals(context.rootProjectDir())) {
            // Files under the project root directory should show ONLY under the root node
            return bazelTreeNodeType == ROOT;
        }

        if (bazelTreeNodeType != ROOT) {
            if (shouldShowDirectoryUnderTreeNode(context, parent, ROOT)) {
                // If the file is already shown under the root node, don't show it under any other node to avoid duplication
                return false;
            }
        }

        return shouldShowDirectoryUnderTreeNode(context, parent, bazelTreeNodeType);
    }

    private boolean shouldShowDirectoryUnderTreeNode(BazelTreeStructureContext context, VirtualFile directory, BazelTreeNodeType bazelTreeNodeType) {
        if (bazelTreeNodeType == ROOT && context.isDirectoryContainingAnotherIncludedDirectory(directory)) {
            return true;
        }
        if (bazelTreeNodeType == EXCLUDED && context.isDirectoryContainingAnotherExcludedDirectory(directory)) {
            return true;
        }

        return computeNodeTypeForFile(directory) == bazelTreeNodeType;
    }

    /**
     * Inspired by {@code WorkspaceFileIndexDataImpl.getFileInfo}, which we can't use here directly
     * because the whole project root is registered as a workspace content,
     * so it won't give us data about unimported directories.
     */
    private BazelTreeNodeType computeNodeTypeForFile(VirtualFile virtualFile) {
        Set<VirtualFile> includedRoots = BazelProjectDirectoriesUtils.includedRoots(myProject);
        if (includedRoots == null) {
            return ROOT;
        }
        Set<VirtualFile> excludedRoots = BazelProjectDirectoriesUtils.excludedRoots(myProject);
        if (excludedRoots == null) {
            return ROOT;
        }
        VirtualFile current = virtualFile;
        while (current != null) {
            if (includedRoots.contains(current)) {
                return ROOT;
            }
            if (excludedRoots.contains(current)) {
                return EXCLUDED;
            }
            current = current.getParent();
        }
        return UNIMPORTED;
    }

    private BazelTreeStructureContext getOrComputeBazelTreeStructureContext() {
        return CachedValuesManager.getManager(myProject).getCachedValue(
            myProject,
            () -> CachedValueProvider.Result.create(
                BazelTreeStructureContext.buildBazelTreeStructureContext(BazelProjectDirectoriesUtils.bazelProjectDirectoriesEntity(myProject)),
                ProjectRootManager.getInstance(myProject)
            )
        );
    }

    // consulo: telemetry is not ported, measure() only runs the body
    private static <T> T measure(String spanName, Supplier<T> f) {
        return f.get();
    }

    public static BazelTreeStructureController getInstance(Project project) {
        return project.getInstance(BazelTreeStructureController.class);
    }

    private record BazelTreeStructureContext(
        boolean projectImported,
        @Nullable VirtualFile rootProjectDir,
        Set<VirtualFile> directoriesContainingIncludedDirectories,
        Set<VirtualFile> directoriesContainingExcludedDirectories
    ) {
        BazelTreeStructureContext() {
            this(false, null, Set.of(), Set.of());
        }

        boolean isDirectoryContainingAnotherIncludedDirectory(VirtualFile file) {
            return directoriesContainingIncludedDirectories.contains(file);
        }

        boolean isDirectoryContainingAnotherExcludedDirectory(VirtualFile file) {
            return directoriesContainingExcludedDirectories.contains(file);
        }

        static BazelTreeStructureContext buildBazelTreeStructureContext(@Nullable BazelProjectDirectoriesEntity bazelProjectDirectories) {
            if (bazelProjectDirectories == null) {
                return new BazelTreeStructureContext();
            }
            return new BazelTreeStructureContext(
                true,
                VirtualFileManager.getInstance().findFileByUrl(bazelProjectDirectories.projectRoot()),
                directoriesContainingDirectories(bazelProjectDirectories.includedRoots()),
                directoriesContainingDirectories(bazelProjectDirectories.excludedRoots())
            );
        }

        private static Set<VirtualFile> directoriesContainingDirectories(List<String> roots) {
            Set<VirtualFile> result = new LinkedHashSet<>();
            for (String url : roots) {
                VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
                if (file == null) {
                    continue;
                }
                VirtualFile directory = file.isDirectory() ? file : file.getParent();
                if (directory != null) {
                    result.add(directory);
                }
            }

            Deque<VirtualFile> toVisit = new ArrayDeque<>(result);
            while (!toVisit.isEmpty()) {
                VirtualFile dir = toVisit.removeFirst();
                VirtualFile parent = dir.getParent();
                if (parent == null) {
                    continue;
                }
                if (result.contains(parent)) {
                    continue;
                }
                result.add(parent);
                toVisit.add(parent);
            }

            return result;
        }
    }
}
