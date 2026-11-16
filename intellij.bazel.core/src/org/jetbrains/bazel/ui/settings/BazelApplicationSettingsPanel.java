package org.jetbrains.bazel.ui.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableProvider;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.http.HttpProxySettingService;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CollectionComboBoxModel;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.HyperlinkLabel;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.bazel.bazelrunner.BazelProxyForwarding;
import org.jetbrains.bazel.settings.bazel.BazelApplicationSettings;
import org.jetbrains.bazel.settings.bazel.BazelApplicationSettingsService;
import org.jetbrains.bazel.settings.bazel.UpdateChannel;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// consulo: upstream XML <applicationConfigurable id="bazel.application.settings" parentId="bazel.project.settings">
@ExtensionImpl
class BazelApplicationSettingsConfigurable implements ApplicationConfigurable, SearchableConfigurable {
    public static final String ID = "bazel.application.settings";
    public static final String DISPLAY_NAME_KEY = "application.settings.display.name";

    private final BazelApplicationSettings myPanelApplicationSettings = BazelApplicationSettingsService.getInstance().getSettings().copy();

    private final ComboBox<UpdateChannel> myUpdateChannelComboBox = new ComboBox<>();
    private final JBCheckBox myEnablePhasedSyncCheckBox = initEnablePhasedSyncCheckBox();
    private final JBCheckBox myForwardProxyCheckBox = initForwardProxyCheckBox();
    private final JBLabel myProxyStatusLabel = new JBLabel();

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

    private JBCheckBox initForwardProxyCheckBox() {
        JBCheckBox checkBox = new JBCheckBox(BazelPluginLocalize.applicationSettingsProxyForwardCheckboxText().get());
        BazelApplicationSettings settings = myPanelApplicationSettings;
        checkBox.setSelected(settings.getForwardProxySettings());
        checkBox.addItemListener(e -> settings.setForwardProxySettings(checkBox.isSelected()));
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
        // consulo: upstream link() -> HyperlinkLabel, HttpProxyConfigurable.editConfigurable -> HttpProxySettingService.showSettings
        HyperlinkLabel configureProxyLink = new HyperlinkLabel();
        configureProxyLink.setHyperlinkText(BazelPluginLocalize.applicationSettingsProxyConfigureLink().get());
        configureProxyLink.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                UIAccess uiAccess = UIAccess.current();
                Application.get().getInstance(HttpProxySettingService.class).showSettings(null)
                    .whenComplete((result, error) -> uiAccess.give(this::updateProxyStatus));
            }
        });
        // consulo: upstream indent { ... }.visibleIf(forwardProxyCheckBox.selected)
        JPanel proxyDetailsPanel = FormBuilder.createFormBuilder()
            .addComponent(myProxyStatusLabel)
            .addComponent(configureProxyLink)
            .getPanel();
        proxyDetailsPanel.setBorder(JBUI.Borders.emptyLeft(20));
        proxyDetailsPanel.setVisible(myForwardProxyCheckBox.isSelected());
        myForwardProxyCheckBox.addItemListener(e -> proxyDetailsPanel.setVisible(myForwardProxyCheckBox.isSelected()));
        // consulo: upstream launchOnShow("BazelProxyStatus") { updateProxyStatus() }, the status is computed when the panel is created
        updateProxyStatus();

        return FormBuilder.createFormBuilder()
            .addComponent(new TitledSeparator(BazelPluginLocalize.applicationSettingsUpdatesTitle().get()))
            .addLabeledComponent(BazelPluginLocalize.applicationSettingsUpdateChannelDropdownTitle().get(), myUpdateChannelComboBox)
            .addTooltip(BazelPluginLocalize.applicationSettingsUpdateChannelDropdownHelpDescription().get())
            .addComponent(new TitledSeparator(BazelPluginLocalize.applicationSettingsProxyTitle().get()))
            .addComponent(myForwardProxyCheckBox)
            .addTooltip(BazelPluginLocalize.applicationSettingsProxyForwardHelpText().get())
            .addComponent(proxyDetailsPanel)
            .addComponent(new TitledSeparator(BazelPluginLocalize.applicationSettingsPluginExperimentalSettings().get()))
            .addComponent(myEnablePhasedSyncCheckBox)
            .addTooltip(BazelPluginLocalize.applicationSettingsPluginEnablePhasedSyncHelpText().get())
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    private void updateProxyStatus() {
        if (BazelProxyForwarding.isSupported()) {
            myProxyStatusLabel.setIcon(PlatformIconGroup.generalInspectionsok());
            myProxyStatusLabel.setText(BazelPluginLocalize.applicationSettingsProxyStatusSupported().get());
        }
        else {
            myProxyStatusLabel.setIcon(PlatformIconGroup.generalWarning());
            myProxyStatusLabel.setText(BazelPluginLocalize.applicationSettingsProxyStatusUnsupported().get());
        }
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
        BazelApplicationSettingsService settingsService = BazelApplicationSettingsService.getInstance();
        if (settingsService.getSettings().getUpdateChannel() != myPanelApplicationSettings.getUpdateChannel()) {
            BazelPluginUpdater.updatePluginsHost(myPanelApplicationSettings.getUpdateChannel());
            BazelPluginUpdater.updatePlugins();
        }
        settingsService.setSettings(myPanelApplicationSettings.copy());
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        BazelApplicationSettings settings = BazelApplicationSettingsService.getInstance().getSettings();
        myUpdateChannelComboBox.setSelectedItem(settings.getUpdateChannel());
        myEnablePhasedSyncCheckBox.setSelected(settings.getEnablePhasedSync());
        myForwardProxyCheckBox.setSelected(settings.getForwardProxySettings());
    }

    static final class SearchIndex { // the companion object of a Configurable is not allowed to have non-const members
        static final List<String> KEYS =
            List.of(
                "application.settings.updates.title",
                "application.settings.update.channel.dropdown.title",
                "application.settings.proxy.title",
                "application.settings.proxy.forward.checkbox.text"
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
