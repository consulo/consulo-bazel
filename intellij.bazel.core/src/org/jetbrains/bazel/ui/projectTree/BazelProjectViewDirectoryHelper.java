package org.jetbrains.bazel.ui.projectTree;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.BaseProjectViewDirectoryHelper;
import consulo.project.ui.view.tree.PsiFileSystemItemFilter;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.workspace.packageMarker.concatenatePackages;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// consulo: IntelliJ's ProjectViewDirectoryHelper is a project service with overridable methods; Consulo's counterpart
// BaseProjectViewDirectoryHelper is a static utility, so the class is standalone and delegates to it. The IntelliJ
// workspace model entities at a file are replaced by the classic source roots.
public class BazelProjectViewDirectoryHelper {
    private final Project myProject;
    private final ProjectFileIndex myFileIndex;

    public BazelProjectViewDirectoryHelper(Project project) {
        myProject = project;
        myFileIndex = ProjectFileIndex.getInstance(project);
    }

    @RequiredReadAction
    public @Nullable String getPackageName(PsiDirectory directory) {
        VirtualFile file = directory.getVirtualFile();
        VirtualFile root = myFileIndex.getSourceRootForFile(file);
        if (root == null) {
            return null;
        }

        // consulo: upstream reads the package prefix of the java source root properties of the SourceRootEntity; Consulo
        // core source roots have no package prefix
        if (!ProjectRootsUtil.isSourceRoot(root, myProject)) {
            return null;
        }
        String packagePrefix = "";
        String relativeName = VirtualFileUtil.getRelativePath(file, root, '.');
        if (relativeName == null) {
            return null;
        }
        return concatenatePackages.concatenatePackages(packagePrefix, relativeName);
    }

    @RequiredReadAction
    public boolean isSourceRoot(PsiDirectory directory) {
        return ProjectRootsUtil.isSourceRoot(directory);
    }

    /**
     * Returns the subdirectory into which a middle {@code directory} outside source roots collapses, or `null`.
     * The heuristic uses only the directory structure: {@code directory} must be inside a Bazel package,
     * and the subdirectory must be the only visible child of {@code directory}.
     * Symlinked and excluded directories never collapse, e.g., the Bazel convenience symlinks to the output trees.
     */
    @RequiredReadAction
    public @Nullable PsiDirectory findCollapsibleSubdirectory(PsiDirectory directory, @Nullable PsiFileSystemItemFilter filter) {
        if (!isCollapsible(directory.getVirtualFile()) || !isInsideBazelPackage(directory.getVirtualFile())) {
            return null;
        }
        PsiDirectory single = null;
        for (PsiElement child : directory.getChildren()) {
            if (!(child instanceof PsiFileSystemItem item) || isFileIgnored(item) ||
                filter != null && !filter.shouldShow(item)) {
                continue;
            }
            if (!(child instanceof PsiDirectory childDirectory) || single != null) {
                return null;
            }
            single = childDirectory;
        }
        return single != null && isCollapsible(single.getVirtualFile()) ? single : null;
    }

    // consulo: upstream FileTypeRegistry.isFileIgnored(child.virtualFile); a PSI item without a virtual file is not ignored
    private static boolean isFileIgnored(PsiFileSystemItem item) {
        VirtualFile file = item.getVirtualFile();
        return file != null && FileTypeRegistry.getInstance().isFileIgnored(file);
    }

    private boolean isCollapsible(VirtualFile receiver) {
        return !receiver.is(VFileProperty.SYMLINK) && !myFileIndex.isExcluded(receiver);
    }

    /**
     * A directory is inside a Bazel package if it or one of its ancestors in the project has a BUILD file.
     */
    private boolean isInsideBazelPackage(VirtualFile directory) {
        VirtualFile rootDirectory = org.jetbrains.bazel.config.project.getRootDir(myProject);
        VirtualFile current = directory;
        while (current != null && VirtualFileUtil.isAncestor(rootDirectory, current, false)) {
            for (String buildFileName : Constants.BUILD_FILE_NAMES) {
                VirtualFile child = current.findChild(buildFileName);
                if (child != null && !child.isDirectory()) {
                    return true;
                }
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * consulo: upstream inherited {@code ProjectViewDirectoryHelper.getDirectoryChildren(directory, settings, withSubDirectories, filter)};
     * Consulo's helper has no filter parameter, so the filter is applied to the children here
     */
    @RequiredReadAction
    public Collection<AbstractTreeNode> getDirectoryChildren(
        PsiDirectory directory,
        ViewSettings settings,
        boolean withSubDirectories,
        @Nullable PsiFileSystemItemFilter filter
    ) {
        Collection<AbstractTreeNode> children = BaseProjectViewDirectoryHelper.getDirectoryChildren(directory, settings, withSubDirectories);
        if (filter == null) {
            return children;
        }
        List<AbstractTreeNode> result = new ArrayList<>(children.size());
        for (AbstractTreeNode child : children) {
            if (child.getValue() instanceof PsiFileSystemItem item && !filter.shouldShow(item)) {
                continue;
            }
            result.add(child);
        }
        return result;
    }

    @RequiredReadAction
    public boolean skipDirectory(PsiDirectory directory) {
        return getPackageName(directory) == null && BaseProjectViewDirectoryHelper.skipDirectory(directory);
    }

    @RequiredReadAction
    public boolean isEmptyMiddleDirectory(PsiDirectory directory, boolean strictlyEmpty, @Nullable PsiFileSystemItemFilter filter) {
        if (getPackageName(directory) == null) {
            // consulo: Consulo's helper has no filter parameter
            return BaseProjectViewDirectoryHelper.isEmptyMiddleDirectory(directory, strictlyEmpty);
        }
        int subdirectories = 0;
        for (PsiElement child : directory.getChildren()) {
            if (!(child instanceof PsiFileSystemItem item) || isFileIgnored(item) ||
                filter != null && !filter.shouldShow(item)) {
                continue;
            }
            if (!(child instanceof PsiDirectory childDirectory) || skipDirectory(childDirectory)) {
                return false;
            }
            subdirectories++;
            if (strictlyEmpty && subdirectories > 1) {
                return false;
            }
        }
        return subdirectories > 0;
    }
}
