package org.jetbrains.bazel.run.config;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentType;
import consulo.bazel.execution.ui.SettingsEditorLabeledComponent;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jspecify.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import java.util.ArrayList;
import java.util.List;

public class RunHandlerFragment
    extends SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<ComboBox<@Nullable RunHandlerProvider>>> {
    private final ComboBox<@Nullable RunHandlerProvider> myHandlerCombo;

    // the @Nullable element type of the combo box is lost when NullAway reads it back through the generic fragment API
    @SuppressWarnings("NullAway")
    public RunHandlerFragment(ComboBox<@Nullable RunHandlerProvider> handlerCombo) {
        super(
            "bazel.run.handler.fragment",
            BazelPluginLocalize.runconfigHandlerName(),
            BazelPluginLocalize.runconfigTargetsGroup(),
            new SettingsEditorLabeledComponent<>(BazelPluginLocalize.runconfigHandlerName(), handlerCombo),
            /* priority = */ 0,
            SettingsEditorFragmentType.EDITOR,
            (configuration, component) -> resetHandlerCombo(component.getComponent(), configuration),
            (configuration, component) -> {
                RunHandlerProvider selected =
                    component.getComponent().getSelectedItem() instanceof RunHandlerProvider provider &&
                        findOfferedProviders(configuration).contains(provider) ? provider : null;
                configuration.updateForcedHandlerProvider(selected != null ? selected.getId() : null);
            },
            configuration -> true
        );
        myHandlerCombo = handlerCombo;
        setHint(BazelPluginLocalize.runconfigHandlerHint());
        setActionHint(BazelPluginLocalize.runconfigHandlerActionHint());
        myHandlerCombo.addActionListener(e -> fireEditorStateChanged());
    }

    public RunHandlerFragment() {
        this(createHandlerCombo());
    }

    // consulo: upstream private top-level functions of RunHandlerFragment.kt

    private static @Nullable RunHandlerProvider findSelectedProvider(BazelRunConfiguration configuration) {
        String forcedHandlerProviderId = configuration.getForcedHandlerProviderId();
        return forcedHandlerProviderId != null ? RunHandlerProvider.findRunHandlerProvider(forcedHandlerProviderId) : null;
    }

    private static ComboBox<@Nullable RunHandlerProvider> createHandlerCombo() {
        ComboBox<@Nullable RunHandlerProvider> combo = new ComboBox<>();
        combo.setRenderer(
            SimpleListCellRenderer.<@Nullable RunHandlerProvider>create(
                BazelPluginLocalize.runconfigHandlerAuto().get(),
                it -> it == null ? "" : it.getProvidedRunHandlerName().get()
            )
        );
        return combo;
    }

    private static void resetHandlerCombo(ComboBox<@Nullable RunHandlerProvider> combo, BazelRunConfiguration configuration) {
        List<RunHandlerProvider> offered = findOfferedProviders(configuration);
        RunHandlerProvider selectedProvider = findSelectedProvider(configuration);
        @Nullable RunHandlerProvider selected = selectedProvider != null && offered.contains(selectedProvider) ? selectedProvider : null;
        List<@Nullable RunHandlerProvider> items = new ArrayList<>();
        items.add(null);
        items.addAll(offered);
        combo.setModel(new DefaultComboBoxModel<>(items.toArray(new RunHandlerProvider[0])));
        combo.setSelectedItem(selected);
        combo.setEnabled(items.size() > 1); // disable when there's nothing to choose
    }

    private static List<RunHandlerProvider> findOfferedProviders(BazelRunConfiguration configuration) {
        List<Label> targets = configuration.getTargets();
        RunHandlerProvider defaultProvider =
            !targets.isEmpty() ? RunHandlerProvider.getRunHandlerProviderOrNull(configuration.getProject(), targets) : null;
        @Nullable Boolean isTestConfig = defaultProvider != null ? defaultProvider.isTestHandler() : null;
        List<RunHandlerProvider> result = new ArrayList<>();
        for (RunHandlerProvider it : RunHandlerProvider.findSelectableRunHandlerProviders()) {
            // offer only test handlers for test targets and vice versa
            if (isTestConfig == null || it.isTestHandler() == isTestConfig) {
                result.add(it);
            }
        }
        return result;
    }
}
