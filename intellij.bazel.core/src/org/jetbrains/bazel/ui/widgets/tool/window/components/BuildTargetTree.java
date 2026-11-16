package org.jetbrains.bazel.ui.widgets.tool.window.components;

import consulo.project.Project;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.tree.Tree;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.BuildTargetClassifierExtension;
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.BuildTargetClassifierExtension.ListTargetClassifier;
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.BuildTargetClassifierExtension.TreeTargetClassifier;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction;
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction;
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts;
import org.jetbrains.bazel.ui.widgets.tool.window.utils.LoadedTargetsMouseListener;
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class BuildTargetTree extends Tree {
    private final Project myProject;
    private final DefaultMutableTreeNode myRootNode;
    private final LoadedTargetsMouseListener myLoadedTargetsMouseListener;

    public BuildTargetTree(Project project) {
        this(project, new DefaultMutableTreeNode(new DirectoryNodeData("[root]")));
    }

    public BuildTargetTree(Project project, DefaultMutableTreeNode rootNode) {
        super(rootNode);
        myProject = project;
        myRootNode = rootNode;
        myLoadedTargetsMouseListener = new LoadedTargetsMouseListener(project) {
            private final CopyTargetIdAction myCopyTargetIdAction =
                new CopyTargetIdAction.FromContainer(BuildTargetTree.this) {
                    @Override
                    protected @Nullable BuildTargetInfo getTargetInfo() {
                        return getSelectedBuildTarget();
                    }
                };

            private final BazelJumpToBuildFileAction myBazelJumpToBuildFileAction = createJumpToBuildFileAction();

            private BazelJumpToBuildFileAction createJumpToBuildFileAction() {
                BazelJumpToBuildFileAction.NonXmlRegistered it = new BazelJumpToBuildFileAction.NonXmlRegistered(this::getSelectedBuildTarget);
                it.registerShortcut(BuildTargetTree.this);
                return it;
            }

            @Override
            public String getSelectedComponentName() {
                DefaultMutableTreeNode selected = getLastSelectedPathComponent() instanceof DefaultMutableTreeNode node ? node : null;
                Object userObject = selected != null ? selected.getUserObject() : null;
                if (userObject instanceof TargetNodeData targetNodeData) {
                    return targetNodeData.displayName();
                }
                else if (userObject instanceof DirectoryNodeData directoryNodeData) {
                    return directoryNodeData.name();
                }
                else {
                    return "";
                }
            }

            @Override
            public boolean isPointSelectable(Point point) {
                return getPathForLocation(point.x, point.y) != null;
            }

            @Override
            public @Nullable BuildTargetInfo getSelectedBuildTarget() {
                DefaultMutableTreeNode selected = getLastSelectedPathComponent() instanceof DefaultMutableTreeNode node ? node : null;
                Object userObject = selected != null ? selected.getUserObject() : null;
                if (userObject instanceof TargetNodeData targetNodeData) {
                    return TargetStorage.getTargetInfo(TargetStorage.getTargetStorage(myProject), targetNodeData.id());
                }
                else {
                    return null;
                }
            }

            @Override
            public @Nullable VirtualFile getSelectedDirectory() {
                DefaultMutableTreeNode selected = getLastSelectedPathComponent() instanceof DefaultMutableTreeNode node ? node : null;
                if (selected == null || !(selected.getUserObject() instanceof DirectoryNodeData)) {
                    return null;
                }
                List<String> selectionPath = Arrays.stream(selected.getPath()).skip(1)  // don't include root node
                    .filter(it -> it instanceof DefaultMutableTreeNode)
                    .map(it -> ((DefaultMutableTreeNode) it).getUserObject())
                    .filter(it -> it instanceof DirectoryNodeData)
                    .map(it -> ((DirectoryNodeData) it).name())
                    .toList();
                // TODO: handle nested local repos properly, currently this works for community inside ultimate just by coincidence
                // (repo name is the same as local path)
                // consulo: fully qualified, the constructor parameter `project` hides the class
                @Nullable VirtualFile file = org.jetbrains.bazel.config.project.getRootDir(myProject);
                for (String name : selectionPath) {
                    file = file != null ? file.findFileByRelativePath(name) : null;
                }
                return file;
            }

            @Override
            public CopyTargetIdAction getCopyTargetIdAction() {
                return myCopyTargetIdAction;
            }

            @Override
            public BazelJumpToBuildFileAction getBazelJumpToBuildFileAction() {
                return myBazelJumpToBuildFileAction;
            }
        };

        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setCellRenderer(new TargetTreeCellRenderer(it -> it));
        setRootVisible(false);
        registerKeyboardShortcutForPopup(myLoadedTargetsMouseListener);
        addMouseListener(myLoadedTargetsMouseListener);
    }

    public void updateTree(Collection<Label> visibleTargets, boolean displayAsTree, @Nullable Pattern searchQuery) {
        BuildTargetClassifierExtension classifier;
        if (displayAsTree) {
            classifier = new TreeTargetClassifier(myProject);
        }
        else {
            classifier = new ListTargetClassifier(myProject);
        }

        // Generate the tree with the current targets
        generateTree(visibleTargets, classifier);

        // Notify the tree model that the structure has changed
        TreeModel treeModel = getModel();
        if (treeModel instanceof DefaultTreeModel defaultTreeModel) {
            defaultTreeModel.reload();
        }

        expandPath(new TreePath(myRootNode.getPath()));
        if (searchQuery != null) {
            expandMatchingNodes(myRootNode, searchQuery);
        }
    }

    private void generateTree(Collection<Label> targets, BuildTargetClassifierExtension classifier) {
        generateTreeFromIdentifiers(
            targets.stream()
                .map(it ->
                    new BuildTargetTreeIdentifier(
                        it,
                        classifier.calculateBuildTargetPath(it),
                        classifier.calculateBuildTargetName(it)
                    )
                )
                .toList(),
            classifier.getSeparator()
        );
    }

    private void generateTreeFromIdentifiers(List<BuildTargetTreeIdentifier> targets, @Nullable String separator) {
        Map<@Nullable String, List<BuildTargetTreeIdentifier>> pathToIdentifierMap =
            groupBy(targets, it -> it.path().isEmpty() ? null : it.path().get(0));
        List<String> sortedDirs =
            pathToIdentifierMap.keySet().stream()
                .filter(Objects::nonNull)
                .map(Objects::requireNonNull)
                .sorted()
                .toList();
        myRootNode.removeAllChildren();
        for (String dir : sortedDirs) {
            List<BuildTargetTreeIdentifier> it = pathToIdentifierMap.get(dir);
            if (it != null) {
                myRootNode.add(generateDirectoryNode(it, dir, separator, 0));
            }
        }
        List<BuildTargetTreeIdentifier> rootTargets = pathToIdentifierMap.get(null);
        if (rootTargets != null) {
            for (BuildTargetTreeIdentifier it : rootTargets) {
                myRootNode.add(generateTargetNode(it));
            }
        }
    }

    private DefaultMutableTreeNode generateDirectoryNode(
        List<BuildTargetTreeIdentifier> targets,
        String dirname,
        @Nullable String separator,
        int depth
    ) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        node.setUserObject(new DirectoryNodeData(dirname));

        Map<@Nullable String, List<BuildTargetTreeIdentifier>> pathToIdentifierMap =
            groupBy(targets, it -> depth + 1 < it.path().size() ? it.path().get(depth + 1) : null);
        List<TreeNode> childrenNodeList =
            new ArrayList<>(generateChildrenNodes(pathToIdentifierMap, separator, depth));

        simplifyNodeIfHasOneChild(node, dirname, separator, childrenNodeList);
        for (int index = 0; index < childrenNodeList.size(); index++) {
            TreeNode child = childrenNodeList.get(index);
            if (child instanceof MutableTreeNode mutableChild) {
                node.insert(mutableChild, index);
            }
        }
        return node;
    }

    private List<TreeNode> generateChildrenNodes(
        Map<@Nullable String, List<BuildTargetTreeIdentifier>> pathToIdentifierMap,
        @Nullable String separator,
        int depth
    ) {
        List<TreeNode> childrenDirectoryNodes =
            generateChildrenDirectoryNodes(pathToIdentifierMap, separator, depth);
        List<TreeNode> childrenTargetNodes =
            generateChildrenTargetNodes(pathToIdentifierMap);
        List<TreeNode> result = new ArrayList<>(childrenDirectoryNodes);
        result.addAll(childrenTargetNodes);
        return result;
    }

    private List<TreeNode> generateChildrenDirectoryNodes(
        Map<@Nullable String, List<BuildTargetTreeIdentifier>> pathToIdentifierMap,
        @Nullable String separator,
        int depth
    ) {
        List<TreeNode> result = new ArrayList<>();
        List<String> sortedDirs = pathToIdentifierMap
            .keySet()
            .stream()
            .filter(Objects::nonNull)
            .map(Objects::requireNonNull)
            .sorted()
            .toList();
        for (String dir : sortedDirs) {
            List<BuildTargetTreeIdentifier> it = pathToIdentifierMap.get(dir);
            if (it != null) {
                result.add(generateDirectoryNode(it, dir, separator, depth + 1));
            }
        }
        return result;
    }

    private List<TreeNode> generateChildrenTargetNodes(Map<@Nullable String, List<BuildTargetTreeIdentifier>> pathToIdentifierMap) {
        List<BuildTargetTreeIdentifier> targets = pathToIdentifierMap.get(null);
        if (targets == null) {
            return List.of();
        }
        return targets.stream()
            .sorted(Comparator.comparing(BuildTargetTreeIdentifier::displayName))
            .<TreeNode>map(this::generateTargetNode)
            .toList();
    }

    private DefaultMutableTreeNode generateTargetNode(BuildTargetTreeIdentifier identifier) {
        return new DefaultMutableTreeNode(
            new TargetNodeData(identifier.displayName(), identifier.id())
        );
    }

    private void simplifyNodeIfHasOneChild(
        DefaultMutableTreeNode node,
        String dirname,
        @Nullable String separator,
        List<TreeNode> childrenList
    ) {
        if (separator != null && childrenList.size() == 1) {
            DefaultMutableTreeNode onlyChild = childrenList.get(0) instanceof DefaultMutableTreeNode child ? child : null;
            Object onlyChildObject = onlyChild != null ? onlyChild.getUserObject() : null;
            if (onlyChild != null && onlyChildObject instanceof DirectoryNodeData directoryNodeData) {
                node.setUserObject(new DirectoryNodeData(dirname + separator + directoryNodeData.name()));
                childrenList.clear();
                childrenList.addAll(Collections.list(onlyChild.children()));
            }
        }
    }

    private void expandMatchingNodes(DefaultMutableTreeNode root, Pattern searchRegex) {
        ArrayDeque<DefaultMutableTreeNode> dirsToProcess = new ArrayDeque<>();
        for (TreeNode topLevelNode : Collections.list(root.children())) {
            if (topLevelNode instanceof DefaultMutableTreeNode node && node.getChildCount() > 0) {
                dirsToProcess.add(node);
            }
        }

        while (!dirsToProcess.isEmpty()) {
            DefaultMutableTreeNode node = dirsToProcess.removeFirst();
            if (!containsMatch(node, searchRegex)) {
                // if the node name does not match, expand it and go deeper
                expandPath(new TreePath(node.getPath()));
                for (TreeNode child : Collections.list(node.children())) {
                    if (child instanceof DefaultMutableTreeNode childNode && childNode.getChildCount() > 0) {
                        dirsToProcess.add(childNode);
                    }
                }
            }
        }
    }

    private static boolean containsMatch(DefaultMutableTreeNode receiver, Pattern regex) {
        Object userObject = receiver.getUserObject();
        if (userObject == null) {
            return false;
        }
        String nodeName;
        if (userObject instanceof DirectoryNodeData directoryNodeData) {
            nodeName = directoryNodeData.name();
        }
        else if (userObject instanceof TargetNodeData targetNodeData) {
            nodeName = targetNodeData.displayName();
        }
        else {
            return false;
        }
        return regex.matcher(nodeName).find();
    }

    @Override
    public boolean isEmpty() {
        return myRootNode.isLeaf();
    }

    private void registerKeyboardShortcutForPopup(PopupHandler popupHandler) {
        SimpleAction popupAction =
            new SimpleAction(() -> {
                TreePath selectionPath = getSelectionPath();
                if (selectionPath == null) {
                    return;
                }
                boolean directoryIsSelected = selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode node && !node.isLeaf();
                if (directoryIsSelected) {
                    // Normally, this is a built-in feature of a Tree.
                    //   However, it stops working out-of-the-box since its keyboard shortcut has been overridden.
                    toggleExpansion(selectionPath);
                }
                else {
                    Rectangle bounds = getPathBounds(selectionPath);
                    if (bounds == null) {
                        return;
                    }
                    Point selectedPoint = bounds.getLocation();
                    popupHandler.invokePopup(this, selectedPoint.x, selectedPoint.y);
                }
            });
        popupAction.registerCustomShortcutSet(BspShortcuts.openTargetContextMenu, this);
    }

    private void toggleExpansion(TreePath path) {
        if (isCollapsed(path)) {
            expandPath(path);
        }
        else {
            collapsePath(path);
        }
    }

    // consulo: Kotlin groupBy keeps null keys and the encounter order (Collectors.groupingBy rejects null keys)
    private static Map<@Nullable String, List<BuildTargetTreeIdentifier>> groupBy(
        List<BuildTargetTreeIdentifier> targets,
        Function<BuildTargetTreeIdentifier, @Nullable String> keySelector
    ) {
        Map<@Nullable String, List<BuildTargetTreeIdentifier>> result = new LinkedHashMap<>();
        for (BuildTargetTreeIdentifier target : targets) {
            result.computeIfAbsent(keySelector.apply(target), key -> new ArrayList<>()).add(target);
        }
        return result;
    }
}

record DirectoryNodeData(
    String name
) {
}

record TargetNodeData(
    String displayName,
    Label id
) {
}

record BuildTargetTreeIdentifier(
    Label id,
    List<String> path,
    String displayName
) {
}
