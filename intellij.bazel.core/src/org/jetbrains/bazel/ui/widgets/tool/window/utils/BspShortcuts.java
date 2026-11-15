package org.jetbrains.bazel.ui.widgets.tool.window.utils;

import consulo.ui.ex.action.CustomShortcutSet;

import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyStroke;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;

/**
 * Custom keyboard shortcuts for BSP plugin's UI.
 * If a shortcut is present in {@link consulo.ui.ex.action.CommonShortcuts}, it does not have to be defined here
 */
public final class BspShortcuts {
    private BspShortcuts() {
    }

    /**
     * Toggle case-sensitivity in target search
     */
    public static final CustomShortcutSet matchCase = new CustomShortcutSet(KeyStroke.pressed(KeyCode.C, Modifier.ALT));

    /**
     * Toggle regex mode in target search
     */
    public static final CustomShortcutSet regexMode = new CustomShortcutSet(KeyStroke.pressed(KeyCode.X, Modifier.ALT));

    /**
     * Switch focus from the searchbar, down to the tree/list of targets
     */
    public static final CustomShortcutSet fromSearchBarToTargets = new CustomShortcutSet(KeyStroke.pressed(KeyCode.DOWN));

    /**
     * Open context menu for the selected target
     */
    public static final CustomShortcutSet openTargetContextMenu = new CustomShortcutSet(KeyStroke.pressed(KeyCode.ENTER));
}
