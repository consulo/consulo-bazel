package org.jetbrains.bazel.ui.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableProvider;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CollectionComboBoxModel;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// consulo: BazelApplicationSettingsService and BazelApplicationSettings of the upstream file are public,
// they live in BazelApplicationSettingsService.java and BazelApplicationSettings.java

// consulo: upstream XML <applicationConfigurable id="bazel.application.settings" parentId="bazel.project.settings">
@ExtensionImpl
class BazelApplicationSettingsConfigurable implements ApplicationConfigurable, SearchableConfigurable {
    public static final String ID = "bazel.application.settings";
    public static final String DISPLAY_NAME_KEY = "application.settings.display.name";

    private final BazelApplicationSettings myPanelApplicationSettings = BazelApplicationSettingsService.getInstance().getSettings().copy();

    private final ComboBox<UpdateChannel> myUpdateChannelComboBox = new ComboBox<>();
    private final JBCheckBox myEnablePhasedSyncCheckBox = initEnablePhasedSyncCheckBox();

    public BazelApplicationSettingsConfigurable() {
        initUpdateChannelComboBox();
    }

    private void initUpdateChannelComboBox() {
        myUpdateChannelComboBox.setModel(new CollectionComboBoxModel<>(Arrays.asList(UpdateChannel.values())));
        myUpdateChannelComboBox.setRenderer(SimpleListCellRenderer.create("", UpdateChannel::getDisplayName));
        myUpdateChannelComboBox.setSelectedItem(myPanelApplicationSettings.getUpdateChannel());
        myUpdateChannelComboBox.addActionListener(e ->
            myPanelApplicationSettings.setUpdateChannel((UpdateChannel) Objects.requireNonNull(myUpdateChannelComboBox.getSelectedItem()))
        );
    }

    private JBCheckBox initEnablePhasedSyncCheckBox() {
        JBCheckBox checkBox = new JBCheckBox(BazelPluginLocalize.applicationSettingsPluginEnablePhasedSyncCheckboxText().get());
        BazelApplicationSettings settings = myPanelApplicationSettings;
        checkBox.setSelected(settings.getEnablePhasedSync());
        checkBox.addItemListener(e -> settings.setEnablePhasedSync(checkBox.isSelected()));
        return checkBox;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable String getParentId() {
        return BazelProjectSettingsConfigurable.ID;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.applicationSettingsDisplayName();
    }

    // consulo: Kotlin UI DSL panel -> FormBuilder, group -> TitledSeparator, contextHelp -> tooltip row
    public JComponent createComponent() {
        return FormBuilder.createFormBuilder()
            .addComponent(new TitledSeparator(BazelPluginLocalize.applicationSettingsUpdatesTitle().get()))
            .addLabeledComponent(BazelPluginLocalize.applicationSettingsUpdateChannelDropdownTitle().get(), myUpdateChannelComboBox)
            .addTooltip(BazelPluginLocalize.applicationSettingsUpdateChannelDropdownHelpDescription().get())
            .addComponent(new TitledSeparator(BazelPluginLocalize.applicationSettingsPluginExperimentalSettings().get()))
            .addComponent(myEnablePhasedSyncCheckBox)
            .addTooltip(BazelPluginLocalize.applicationSettingsPluginEnablePhasedSyncHelpText().get())
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        return TargetAWT.wrap(createComponent());
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return !myPanelApplicationSettings.equals(BazelApplicationSettingsService.getInstance().getSettings());
    }

    @RequiredUIAccess
    @Override
    public void apply() {
        BazelPluginUpdater.updatePluginsHost(myPanelApplicationSettings.getUpdateChannel());
        BazelPluginUpdater.updatePlugins();

        BazelApplicationSettingsService.getInstance().setSettings(myPanelApplicationSettings.copy());
    }

    static final class SearchIndex { // the companion object of a Configurable is not allowed to have non-const members
        static final List<String> KEYS =
            List.of(
                "application.settings.updates.title",
                "application.settings.update.channel.dropdown.title"
            );

        private SearchIndex() {
        }
    }
}

class BazelApplicationSettingsConfigurableProvider extends ConfigurableProvider {
    // consulo: not used, Consulo instantiates the @ExtensionImpl BazelApplicationSettingsConfigurable directly
    @Override
    public Configurable createConfigurable() {
        return new BazelApplicationSettingsConfigurable();
    }
}
