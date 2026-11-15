package org.jetbrains.bazel.ui.widgets.tool.window.components;

import consulo.application.dumb.DumbAware;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awt.JBPanel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts;
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction;
import org.jetbrains.bazel.ui.widgets.tool.window.utils.TextComponentExtension;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

// consulo: IntelliJ's ExtendableTextField is replaced by the Consulo UI TextBox: the extensions are shown as an INPLACE
// action toolbar in its suffix component (like the VCS log text filter), the clear extension is the TextBox clear button
public class SearchBarPanel extends JBPanel<SearchBarPanel> {
    private final BazelTargetsPanelModel myModel;

    private final TextBox myTextField;

    private final SimpleAction myFocusFindAction;
    private final SimpleAction myToggleMatchCaseAction;
    private final SimpleAction myToggleRegexAction;

    @RequiredUIAccess
    public SearchBarPanel(BazelTargetsPanelModel model) {
        super(new BorderLayout());
        myModel = model;
        myTextField = prepareTextField();

        myFocusFindAction = new SimpleAction(() -> myTextField.focus());
        myToggleMatchCaseAction =
            new SimpleAction(() -> {
                myModel.setMatchCase(!myModel.getMatchCase());
            });
        myToggleRegexAction =
            new SimpleAction(() -> {
                myModel.setRegexMode(!myModel.getRegexMode());
            });

        add(TargetAWT.to(myTextField));
        myToggleMatchCaseAction.registerCustomShortcutSet(BspShortcuts.matchCase, this);
        myToggleRegexAction.registerCustomShortcutSet(BspShortcuts.regexMode, this);
    }

    @RequiredUIAccess
    private TextBox prepareTextField() {
        TextBox newField = TextBox.create();
        // consulo: the switches repaint this panel (upstream: the text field), the TextBox is not a Swing component
        TextComponentExtension.Switch matchCaseExtension =
            new TextComponentExtension.Switch(
                // MatchCaseHovered icon matches the ShowAsTree icon better than the normal Regex; on new UI both icons look the same
                PlatformIconGroup.actionsMatchcasehovered(),
                () -> myModel.getMatchCase(),
                it -> myModel.setMatchCase(it),
                this,
                BazelPluginLocalize.widgetTargetSearchMatchCase(
                    getFirstKeyboardShortcutText(BspShortcuts.matchCase)
                ).get()
            );
        TextComponentExtension.Switch regexExtension =
            new TextComponentExtension.Switch(
                // RegexHovered icon matches the ShowAsTree icon better than the normal Regex; on new UI both icons look the same
                PlatformIconGroup.actionsRegexhovered(),
                () -> myModel.getRegexMode(),
                it -> myModel.setRegexMode(it),
                this,
                BazelPluginLocalize.widgetTargetSearchRegex(getFirstKeyboardShortcutText(BspShortcuts.regexMode)).get()
            );
        TextComponentExtension.Switch treeExtension =
            new TextComponentExtension.Switch(
                PlatformIconGroup.actionsShowastree(),
                () -> myModel.getDisplayAsTree(),
                it -> myModel.setDisplayAsTree(it),
                this,
                BazelPluginLocalize.widgetTargetSearchDisplayAsTree().get()
            );
        // consulo: upstream TextComponentExtension.Clear (tooltip widget.target.search.clear) -> the TextBox clear button
        newField.addStyle(TextBoxStyle.HAS_CLEAR_BUTTON);
        newField.setPlaceholder(
            BazelPluginLocalize.widgetTargetSearchHint(getFirstKeyboardShortcutText(CommonShortcuts.getFind()))
        );

        DefaultActionGroup extensions = new DefaultActionGroup();
        extensions.add(new ExtensionAction(treeExtension));
        extensions.add(new ExtensionAction(regexExtension));
        extensions.add(new ExtensionAction(matchCaseExtension));
        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, extensions, ActionToolbar.Style.INPLACE);
        toolbar.setTargetUIComponent(newField);
        newField.setSuffixComponent(toolbar.getUIComponent());

        newField.addValueListener(event -> {
            myModel.setSearchQuery(StringUtil.notNullize(event.getValue()));
        });
        return newField;
    }

    @RequiredUIAccess
    private void clearQuery() {
        myTextField.setValue("");
    }

    @Override
    @RequiredUIAccess
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        myTextField.setEnabled(enabled);
    }

    public void registerSearchShortcutsOn(JComponent component) {
        myFocusFindAction.registerCustomShortcutSet(CommonShortcuts.getFind(), component);
        myToggleMatchCaseAction.registerCustomShortcutSet(BspShortcuts.matchCase, component);
        myToggleRegexAction.registerCustomShortcutSet(BspShortcuts.regexMode, component);
    }

    @Override
    @RequiredUIAccess
    public void requestFocus() {
        myTextField.focus();
    }

    // consulo: upstream forwards the typed key event to the text field; the TextBox gets the typed character appended
    @Override
    @RequiredUIAccess
    public void processKeyEvent(KeyEvent event) {
        char keyChar = event.getKeyChar();
        if (keyChar == KeyEvent.CHAR_UNDEFINED || Character.isISOControl(keyChar)) {
            return;
        }
        myTextField.setValue(StringUtil.notNullize(myTextField.getValue()) + keyChar);
    }

    @RequiredUIAccess
    public boolean isEmpty() {
        return StringUtil.isEmpty(myTextField.getValue());
    }

    // consulo: KeymapUtil.getFirstKeyboardShortcutText(ShortcutSet) does not exist in Consulo, same logic as IntelliJ's
    private static String getFirstKeyboardShortcutText(ShortcutSet shortcutSet) {
        for (Shortcut shortcut : shortcutSet.getShortcuts()) {
            if (shortcut instanceof KeyboardShortcut) {
                return KeymapUtil.getShortcutText(shortcut);
            }
        }
        return "";
    }

    // consulo: an extension of the upstream ExtendableTextField, shown in the suffix toolbar of the TextBox
    private static final class ExtensionAction extends ToggleAction implements DumbAware {
        private final TextComponentExtension myExtension;

        private ExtensionAction(TextComponentExtension extension) {
            super(LocalizeValue.ofNullable(extension.getTooltip()), LocalizeValue.empty(), extension.getIcon(false));
            myExtension = extension;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myExtension.isSelected();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            Runnable actionOnClick = myExtension.getActionOnClick();
            if (actionOnClick != null) {
                actionOnClick.run();
            }
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            super.update(e);
            @Nullable Image icon = myExtension.getIcon(false);
            e.getPresentation().setIcon(icon);
            e.getPresentation().setVisible(icon != null);
        }
    }
}
