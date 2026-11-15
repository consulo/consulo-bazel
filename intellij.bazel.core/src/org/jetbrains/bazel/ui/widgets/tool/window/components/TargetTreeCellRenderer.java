package org.jetbrains.bazel.ui.widgets.tool.window.components;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.JBLabel;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Component;
import java.util.function.Function;

public class TargetTreeCellRenderer implements TreeCellRenderer {
    private final Function<String, String> myLabelHighlighter;

    public TargetTreeCellRenderer(Function<String, String> labelHighlighter) {
        myLabelHighlighter = labelHighlighter;
    }

    public Function<String, String> getLabelHighlighter() {
        return myLabelHighlighter;
    }

    @Override
    public Component getTreeCellRendererComponent(
        @Nullable JTree tree,
        @Nullable Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
        Object userObject = value instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
        if (userObject instanceof DirectoryNodeData directoryNodeData) {
            return new JBLabel(
                myLabelHighlighter.apply(directoryNodeData.name()),
                // consulo: PlatformIcons.FOLDER_ICON
                PlatformIconGroup.nodesFolder(),
                SwingConstants.LEFT
            );
        }
        else if (userObject instanceof TargetNodeData targetNodeData) {
            return new JBLabel(
                myLabelHighlighter.apply(targetNodeData.displayName()),
                BazelPluginIcons.bazel(),
                SwingConstants.LEFT
            );
        }
        else {
            return new JBLabel(
                BazelPluginLocalize.widgetNoRenderableComponent().get(),
                // consulo: PlatformIcons.ERROR_INTRODUCTION_ICON (AllIcons.Nodes.ErrorIntroduction) does not exist, closest icon
                PlatformIconGroup.nodesErrormark(),
                SwingConstants.LEFT
            );
        }
    }

    private String labelStrikethrough(String text) {
        return "<html><s>" + text + "</s><html>";
    }
}
