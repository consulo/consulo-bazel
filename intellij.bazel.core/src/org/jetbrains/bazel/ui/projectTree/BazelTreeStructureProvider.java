package org.jetbrains.bazel.ui.projectTree;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ExternalLibrariesNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ProjectViewModuleGroupNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ProjectViewModuleNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ProjectViewProjectNode;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.module.ModuleManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.project.ui.view.tree.PsiFileNode;
import consulo.project.ui.view.tree.PsiFileSystemItemFilter;
import consulo.project.ui.view.tree.TreeStructureProvider;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@ExtensionImpl
public class BazelTreeStructureProvider implements TreeStructureProvider {
    public BazelTreeStructureProvider() {
    }

    // We want to get rid of all the module (group) nodes from the project view tree;
    // in rare cases with complicated project (modules) structure IJ
    // doesn't know how to render the project tree,
    // what causes flat directory structure.
    // Instead we use the root directory node as the root of the project view tree and ignore modules altogether.
    // This results in displaying the folder structure of the project as it is on the file system.
    @Override
    @RequiredReadAction
    public Collection<AbstractTreeNode> modify(
        AbstractTreeNode parent,
        Collection<AbstractTreeNode> children,
        ViewSettings settings
    ) {
        Project project = parent.getProject();
        if (project == null) {
            return children;
        }
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return children;
        }
        if (parent instanceof ProjectViewProjectNode) {
            return calculateChildrenForProjectNode(project, children, settings);
        }
        else {
            List<AbstractTreeNode> result = new ArrayList<>();
            for (AbstractTreeNode it : removeModuleAndModuleGroupNodes(children)) {
                if (it instanceof PsiDirectoryNode psiDirectoryNode) {
                    result.add(new BazelDirectoryNode(project, psiDirectoryNode.getValue(), settings, psiDirectoryNode.getFilter()));
                }
                else {
                    result.add(it);
                }
            }
            return result;
        }
    }

    @RequiredReadAction
    private Collection<AbstractTreeNode> calculateChildrenForProjectNode(
        Project project,
        Collection<AbstractTreeNode> children,
        ViewSettings settings
    ) {
        PsiDirectory rootDirectory = PsiManager.getInstance(project).findDirectory(org.jetbrains.bazel.config.project.getRootDir(project));
        if (rootDirectory == null) {
            return children; // should never happen
        }
        BazelTreeStructureController bazelTreeStructureController = BazelTreeStructureController.getInstance(project);
        List<AbstractTreeNode> result = new ArrayList<>();
        for (AbstractTreeNode child : children) {
            if (child instanceof ExternalLibrariesNode) {
                result.add(child);
                break;
            }
        }

        if (bazelTreeStructureController.shouldShowNode(BazelTreeNodeType.ROOT)) {
            result.add(new BazelDirectoryNode(project, rootDirectory, settings, item ->
                bazelTreeStructureController.shouldShowUnderTreeNode(
                    Objects.requireNonNull(item.getVirtualFile()),
                    BazelTreeNodeType.ROOT
                )
            ));
        }

        if (bazelTreeStructureController.shouldShowNode(BazelTreeNodeType.EXCLUDED)) {
            result.add(new ExcludedDirectoriesNode(project, rootDirectory, settings, item ->
                bazelTreeStructureController.shouldShowUnderTreeNode(
                    Objects.requireNonNull(item.getVirtualFile()),
                    BazelTreeNodeType.EXCLUDED
                )
            ));
        }

        if (bazelTreeStructureController.shouldShowNode(BazelTreeNodeType.UNIMPORTED)) {
            result.add(new UnimportedDirectoriesNode(project, rootDirectory, settings, item ->
                bazelTreeStructureController.shouldShowUnderTreeNode(
                    Objects.requireNonNull(item.getVirtualFile()),
                    BazelTreeNodeType.UNIMPORTED
                )
            ));
        }
        return result;
    }

    private Collection<AbstractTreeNode> removeModuleAndModuleGroupNodes(Collection<AbstractTreeNode> receiver) {
        List<AbstractTreeNode> result = new ArrayList<>();
        for (AbstractTreeNode it : receiver) {
            if (it instanceof ProjectViewModuleGroupNode) {
                continue;
            }
            if (it instanceof ProjectViewModuleNode) {
                continue;
            }
            result.add(it);
        }
        return result;
    }
}

/**
 * Custom PsiDirectoryNode for Bazel projects to show the project view with full contents during initial sync.
 */
class BazelDirectoryNode extends PsiDirectoryNode {
    private final @Nullable PsiFileSystemItemFilter myFilter;

    BazelDirectoryNode(
        Project project,
        PsiDirectory directory,
        ViewSettings viewSettings,
        @Nullable PsiFileSystemItemFilter filter
    ) {
        super(project, directory, viewSettings, filter);
        myFilter = filter;
    }

    // consulo: upstream default argument filter = null
    BazelDirectoryNode(
        Project project,
        PsiDirectory directory,
        ViewSettings viewSettings
    ) {
        this(project, directory, viewSettings, null);
    }

    // TODO consulo: PsiDirectoryNode.shouldShowModuleName() is missing
    //override fun shouldShowModuleName(): Boolean = false

    @Override
    @RequiredReadAction
    protected void updateImpl(PresentationData data) {
        super.updateImpl(data);
        ViewSettings settings = getSettings();
        if (!settings.isFlattenPackages() && settings.isHideEmptyMiddlePackages()) {
            if (!(getParentValue() instanceof PsiDirectory parentDirectory)) {
                return;
            }
            String name = VirtualFileUtil.getRelativePath(getValue().getVirtualFile(), parentDirectory.getVirtualFile(), '/');
            if (name == null) {
                return;
            }
            data.clearText();
            data.setPresentableText(name);
        }
    }

    @RequiredReadAction
    private @Nullable Collection<AbstractTreeNode> calculateCustomChildrenNodes(
        PsiDirectory receiver,
        Project project,
        ViewSettings settings
    ) {
        if (shouldNotCalculateCustomNodes(project)) {
            return null;
        }
        else {
            List<AbstractTreeNode> result = new ArrayList<>();
            for (PsiElement psiChild : receiver.getChildren()) {
                if (psiChild instanceof PsiFileSystemItem psiFileSystemItem
                    && myFilter != null
                    && !myFilter.shouldShow(psiFileSystemItem)) {
                    continue;
                }

                if (psiChild instanceof PsiDirectory psiDirectory) {
                    result.add(new BazelDirectoryNode(project, psiDirectory, settings));
                }
                else if (psiChild instanceof PsiFile psiFile) {
                    result.add(new PsiFileNode(project, psiFile, settings));
                }
            }
            return result;
        }
    }

    /**
     * The project view tree relies on the existence of the "fake module".
     * See https://youtrack.jetbrains.com/issue/IJPL-15946/Platform-solution-for-the-initial-state-of-the-project-model-on-the-first-open
     * However, we remove the fake module in `CounterPlatformProjectConfigurator`.
     * So while the project is syncing, we show our custom tree, so that the user can at least see the project files and directories.
     */
    private boolean shouldNotCalculateCustomNodes(Project receiver) {
        return ModuleManager.getInstance(receiver).getModules().length != 0;
    }

    @Override
    @RequiredReadAction
    public Collection<AbstractTreeNode> getChildrenImpl() {
        // consulo: upstream returns null, Consulo PsiDirectoryNode.getChildrenImpl() is not nullable (null means no children)
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile == null) {
            return Collections.emptyList();
        }
        Project project = Objects.requireNonNull(getProject());
        PsiDirectory directory = PsiManager.getInstance(project).findDirectory(virtualFile);
        if (directory == null) {
            return Collections.emptyList();
        }
        ViewSettings settings = getSettings();
        Collection<AbstractTreeNode> customChildrenNodes = calculateCustomChildrenNodes(directory, project, settings);
        if (customChildrenNodes != null) {
            return customChildrenNodes;
        }

        BazelProjectViewDirectoryHelper helper = new BazelProjectViewDirectoryHelper(project);
        Collection<AbstractTreeNode> children = helper.getDirectoryChildren(directory, settings, true, myFilter);
        if (!settings.isFlattenPackages()) {
            if (!settings.isHideEmptyMiddlePackages()) {
                return children;
            }
            if (!helper.skipDirectory(directory)) {
                List<AbstractTreeNode> result = new ArrayList<>();
                for (AbstractTreeNode child : children) {
                    if (!(child instanceof PsiDirectoryNode childNode)) {
                        result.add(child);
                        continue;
                    }
                    PsiDirectory childDirectory = childNode.getValue();
                    while (!helper.isSourceRoot(childDirectory) && helper.isEmptyMiddleDirectory(childDirectory, true, myFilter)) {
                        ProgressManager.checkCanceled();
                        childDirectory = single(
                            childDirectory.getSubdirectories(),
                            it -> !FileTypeRegistry.getInstance().isFileIgnored(it.getVirtualFile())
                                && (myFilter == null || myFilter.shouldShow(it))
                        );
                    }
                    result.add(new BazelDirectoryNode(project, childDirectory, settings, myFilter));
                }
                return result;
            }
            // outside of source roots, directories don't correspond to packages, so collapse middle directories by the structure only
            List<AbstractTreeNode> result = new ArrayList<>();
            for (AbstractTreeNode child : children) {
                if (!(child instanceof PsiDirectoryNode childNode)) {
                    result.add(child);
                    continue;
                }
                PsiDirectory childDirectory = childNode.getValue();
                while (!helper.isSourceRoot(childDirectory)) {
                    ProgressManager.checkCanceled();
                    PsiDirectory collapsibleSubdirectory = helper.findCollapsibleSubdirectory(childDirectory, myFilter);
                    if (collapsibleSubdirectory == null) {
                        break;
                    }
                    childDirectory = collapsibleSubdirectory;
                }
                result.add(new BazelDirectoryNode(project, childDirectory, settings, myFilter));
            }
            return result;
        }

        if (helper.skipDirectory(directory)) {
            return children;
        }
        PsiDirectory parentDirectory = directory.getParentDirectory();
        if (parentDirectory != null && !helper.skipDirectory(parentDirectory)) {
            return children;
        }

        List<AbstractTreeNode> result = new ArrayList<>();
        for (AbstractTreeNode it : children) {
            if (!(it instanceof PsiDirectoryNode psiDirectoryNode) || helper.skipDirectory(psiDirectoryNode.getValue())) {
                result.add(it);
            }
        }
        Deque<PsiDirectory> directories = new ArrayDeque<>();
        directories.addAll(Arrays.asList(directory.getSubdirectories()));
        while (!directories.isEmpty()) {
            ProgressManager.checkCanceled();
            PsiDirectory subdirectory = directories.removeFirst();
            if (helper.skipDirectory(subdirectory) || (myFilter != null && !myFilter.shouldShow(subdirectory))) {
                continue;
            }
            else if (!settings.isHideEmptyMiddlePackages() || !helper.isEmptyMiddleDirectory(subdirectory, false, myFilter)) {
                result.add(new BazelDirectoryNode(project, subdirectory, settings, myFilter));
            }
            directories.addAll(Arrays.asList(subdirectory.getSubdirectories()));
        }
        return result;
    }

    // consulo: Kotlin Array.single(predicate)
    private static PsiDirectory single(PsiDirectory[] directories, Predicate<PsiDirectory> predicate) {
        @Nullable PsiDirectory single = null;
        boolean found = false;
        for (PsiDirectory it : directories) {
            if (predicate.test(it)) {
                if (found) {
                    throw new IllegalArgumentException("Array contains more than one matching element.");
                }
                single = it;
                found = true;
            }
        }
        if (single == null) {
            throw new java.util.NoSuchElementException("Array contains no element matching the predicate.");
        }
        return single;
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
        return BazelTreeStructureOrder.ROOT.getWeight();
    }
}

class UnimportedDirectoriesNode extends PsiDirectoryNode {
    UnimportedDirectoriesNode(
        Project project,
        PsiDirectory directory,
        ViewSettings viewSettings,
        PsiFileSystemItemFilter filter
    ) {
        super(project, directory, viewSettings, filter);
    }

    @Override
    public boolean isAlwaysShowPlus() {
        return true;
    }

    @Override
    public void update(PresentationData presentation) {
        presentation.setPresentableText(BazelPluginLocalize.widgetProjectTreeUnimportedDirectories());
        presentation.setTooltip(BazelPluginLocalize.widgetProjectTreeUnimportedDirectoriesTooltip().get());
        // consulo: no AllIcons.Nodes.Workspace in PlatformIconGroup, closest key
        presentation.setIcon(PlatformIconGroup.nodesProject());
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
        return BazelTreeStructureOrder.UNIMPORTED.getWeight();
    }
}

class ExcludedDirectoriesNode extends PsiDirectoryNode {
    ExcludedDirectoriesNode(
        Project project,
        PsiDirectory directory,
        ViewSettings viewSettings,
        PsiFileSystemItemFilter filter
    ) {
        super(project, directory, viewSettings, filter);
    }

    @Override
    public boolean isAlwaysShowPlus() {
        return true;
    }

    // TODO consulo: AbstractTreeNode.isIncludedInExpandAll() is missing
    //override fun isIncludedInExpandAll(): Boolean = false

    @Override
    public void update(PresentationData presentation) {
        presentation.setPresentableText(BazelPluginLocalize.widgetProjectTreeExcludedDirectories());
        presentation.setIcon(PlatformIconGroup.modulesExcluderoot());
        // Show in gray even if the project root dir is excluded (for better visibility)
        presentation.setBackground(null);
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
        return BazelTreeStructureOrder.EXCLUDED.getWeight();
    }
}

enum BazelTreeStructureOrder {
    ROOT(1),
    UNIMPORTED(2),
    EXCLUDED(3);

    private final int myWeight;

    BazelTreeStructureOrder(int weight) {
        myWeight = weight;
    }

    public int getWeight() {
        return myWeight;
    }
}
