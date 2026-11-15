package org.jetbrains.bazel.ui.widgets.tool.window.components;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBPanel;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.UIUtil;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts;
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class BazelTargetsPanel extends JBPanel<BazelTargetsPanel> {
    private final SearchBarPanel mySearchBarPanel;
    private final BuildTargetTree myTargetTree;
    private final JBScrollPane myScrollPane;

    private final JBLabel myMessage =
        new JBLabel(
            BazelPluginLocalize.widgetNoTargetsMessage().get(),
            SwingConstants.CENTER
        );

    public BazelTargetsPanel(Project project, BazelTargetsPanelModel model) {
        super(new BorderLayout());
        mySearchBarPanel = new SearchBarPanel(model);
        myTargetTree = new BuildTargetTree(project);
        myScrollPane = new JBScrollPane(myTargetTree);

        myTargetTree.addKeyListener(new SearchKeyAdapter());
        myMessage.addMouseListener(new SearchBarFocusAdapter());

        mySearchBarPanel.setEnabled(true);
        mySearchBarPanel.registerSearchShortcutsOn(myScrollPane);
        registerMoveDownShortcut(mySearchBarPanel);
        add(mySearchBarPanel, BorderLayout.NORTH);
        showTargetTree();
    }

    private void showTargetTree() {
        if (!Arrays.asList(getComponents()).contains(myScrollPane)) {
            add(myScrollPane, BorderLayout.CENTER);
        }
    }

    private void hideTargetTree() {
        remove(myScrollPane);
    }

    private void showMessage() {
        if (!Arrays.asList(getComponents()).contains(myMessage)) {
            add(myMessage, BorderLayout.CENTER);
        }
    }

    private void hideMessage() {
        remove(myMessage);
    }

    @RequiredUIAccess
    public void update(
        List<Label> visibleTargets,
        @Nullable Pattern searchRegex,
        boolean hasAnyTargets,
        boolean displayAsTree
    ) {
        boolean isSearchActive = searchRegex != null;
        // Update the tree highlighter to highlight search matches
        if (isSearchActive) {
            Pattern regex = Objects.requireNonNull(searchRegex);
            myTargetTree.setCellRenderer(new TargetTreeCellRenderer(it -> QueryHighlighter.highlight(it, regex)));
        }
        else {
            myTargetTree.setCellRenderer(new TargetTreeCellRenderer(it -> it));
        }

        // Update visibility of components based on search results
        if (!hasAnyTargets) {
            myMessage.setText(BazelPluginLocalize.widgetNoTargetsMessage().get());
            hideTargetTree();
            showMessage();
        }
        else if (isSearchActive && visibleTargets.isEmpty()) {
            myMessage.setText(BazelPluginLocalize.widgetTargetSearchNoResults().get());
            hideTargetTree();
            showMessage();
        }
        else {
            hideMessage();
            showTargetTree();
        }

        myTargetTree.updateTree(visibleTargets, displayAsTree);

        // Refresh the panel
        revalidate();
        repaint();
    }

    private void registerMoveDownShortcut(SearchBarPanel searchBarPanel) {
        SimpleAction action =
            new SimpleAction(() -> {
//              searchBarPanel.selectionRows = intArrayOf(0)
                requestFocus();
            });
        action.registerCustomShortcutSet(BspShortcuts.fromSearchBarToTargets, searchBarPanel);
    }

    private class SearchKeyAdapter extends KeyAdapter {
        @Override
        public void keyTyped(KeyEvent e) {
            if (!e.isConsumed() && UIUtil.isReallyTypedEvent(e)) {
                mySearchBarPanel.requestFocus();
                mySearchBarPanel.processKeyEvent(e);
                e.consume();
            }
        }
    }

    private class SearchBarFocusAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(@Nullable MouseEvent e) {
            if (e != null && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1) {
                mySearchBarPanel.requestFocus();
                e.consume();
            }
        }
    }
}
