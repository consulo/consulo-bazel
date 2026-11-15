/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.bazel.execution.ui;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunConfigurationExtensionsManager;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Consulo replacement of IntelliJ {@code com.intellij.execution.ui.RunConfigurationFragmentedEditor} (and its base
 * {@code FragmentedSettingsEditor}): a {@link SettingsEditor} built of {@link SettingsEditorFragment}s.
 * <p>
 * The fragments are laid out vertically in the order of {@link #createRunFragments()}: one row per fragment (a label for text
 * like components, the fragment hint below), headers as separators. There is no "Modify options" chooser: every
 * {@link SettingsEditorFragment#isAvailable() available} fragment is shown. Reset and apply are delegated to the fragments;
 * validation errors of the fragments are reported as {@link ConfigurationException}, warnings are only shown on the components.
 * <p>
 * Consulo shows the editor through {@link SettingsEditorRunConfigurationFragment}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public abstract class RunConfigurationFragmentedEditor<Settings extends RunConfigurationBase> extends SettingsEditor<Settings> {
    private final Supplier<Collection<SettingsEditorFragment<Settings, ?>>> myFragments = LazyValue.notNull(() -> {
        Collection<SettingsEditorFragment<Settings, ?>> fragments = createFragments();
        initFragments(fragments);
        return fragments;
    });

    private final Map<SettingsEditorFragment<Settings, ?>, JComponent> myRows = new IdentityHashMap<>();

    protected final Settings mySettings;

    protected RunConfigurationFragmentedEditor(Settings runConfiguration, @Nullable RunConfigurationExtensionsManager<?, ?> extensionsManager) {
        super(() -> cloneSettings(runConfiguration));
        mySettings = runConfiguration;
        // consulo: upstream adds the fragments of the run configuration extensions of the extension manager; Consulo contributes
        // extension settings as RunConfigurationFragment extensions shown by the platform, so the manager is not used here
    }

    protected RunConfigurationFragmentedEditor(Settings runConfiguration) {
        this(runConfiguration, null);
    }

    @SuppressWarnings("unchecked")
    private static <Settings extends RunConfigurationBase> Settings cloneSettings(Settings settings) {
        return (Settings) settings.clone();
    }

    protected Project getProject() {
        return mySettings.getProject();
    }

    protected final List<SettingsEditorFragment<Settings, ?>> createFragments() {
        List<SettingsEditorFragment<Settings, ?>> fragments = new ArrayList<>(createRunFragments());
        // consulo: upstream also adds the runner settings editors of the executors (not ported)
        for (SettingsEditorFragment<Settings, ?> fragment : fragments) {
            Disposer.register(this, fragment);
        }
        return fragments;
    }

    /**
     * @return a list of {@link SettingsEditorFragment} instances representing particular settings fragments that users can configure.
     */
    protected abstract List<SettingsEditorFragment<Settings, ?>> createRunFragments();

    protected void initFragments(Collection<? extends SettingsEditorFragment<Settings, ?>> fragments) {
    }

    protected final Collection<SettingsEditorFragment<Settings, ?>> getFragments() {
        return myFragments.get();
    }

    private List<SettingsEditorFragment<Settings, ?>> getAllFragments() {
        List<SettingsEditorFragment<Settings, ?>> result = new ArrayList<>();
        for (SettingsEditorFragment<Settings, ?> fragment : getFragments()) {
            result.addAll(fragment.getChildren());
            result.add(fragment);
        }
        return result;
    }

    @Override
    protected void resetEditorFrom(Settings settings) {
        for (SettingsEditorFragment<Settings, ?> fragment : getFragments()) {
            fragment.resetFrom(settings);
        }
        updateFragmentVisibility();
    }

    @RequiredUIAccess
    @Override
    protected void applyEditorTo(Settings settings) throws ConfigurationException {
        for (SettingsEditorFragment<Settings, ?> fragment : getFragments()) {
            fragment.applyTo(settings);
        }

        @Nullable ValidationInfo error = null;
        for (SettingsEditorFragment<Settings, ?> fragment : getAllFragments()) {
            for (ValidationInfo info : fragment.validate(settings)) {
                if (!info.warning && error == null) {
                    error = info;
                }
            }
        }
        updateFragmentVisibility();

        // consulo: upstream only shows the validation result on the components; errors also reject the apply here,
        // warnings stay in place only (RuntimeConfigurationWarning is reported by checkConfiguration(), not by editors)
        if (error != null) {
            throw new ConfigurationException(error.message);
        }
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, true, false));
        for (SettingsEditorFragment<Settings, ?> fragment : getFragments()) {
            JComponent row = createRow(fragment);
            myRows.put(fragment, row);
            panel.add(row);
        }
        updateFragmentVisibility();

        JPanel result = new JPanel(new BorderLayout());
        result.add(panel, BorderLayout.NORTH);
        return TargetAWT.wrap(result);
    }

    private void updateFragmentVisibility() {
        for (Map.Entry<SettingsEditorFragment<Settings, ?>, JComponent> entry : myRows.entrySet()) {
            entry.getValue().setVisible(entry.getKey().isAvailable());
        }
    }

    /**
     * One row of the editor: the fragment component, with a label (the fragment name) for text like components that have no label
     * of their own, and the fragment hint below.
     */
    static JComponent createRow(SettingsEditorFragment<?, ?> fragment) {
        JComponent component = fragment.component();
        FormBuilder builder = FormBuilder.createFormBuilder();
        if (needsLabel(fragment, component)) {
            JBLabel label = new JBLabel();
            LabeledComponent.TextWithMnemonic.fromTextWithMnemonic(fragment.getName().get()).setToLabel(label);
            label.setLabelFor(fragment.getEditorComponent());
            builder.addLabeledComponent(label, component);
        }
        else {
            builder.addComponent(component);
        }
        if (fragment.getHint().isNotEmpty()) {
            builder.addTooltip(fragment.getHint().get());
        }
        return builder.getPanel();
    }

    private static boolean needsLabel(SettingsEditorFragment<?, ?> fragment, JComponent component) {
        if (fragment.isHeader() || fragment.getName().isEmpty()) {
            return false;
        }
        return component instanceof RawCommandLineEditor
            || component instanceof JTextComponent
            || component instanceof JComboBox<?>
            || component instanceof ComponentWithBrowseButton<?>;
    }
}
