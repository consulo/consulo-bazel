package org.jetbrains.bazel.ui.settings;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.settings.bazel.BazelJVMProjectSettings;

import javax.swing.JComponent;
import java.util.List;

// consulo: upstream registers this provider in the Java integration (intellij.bazel.java.core.xml,
// bazelExperimentalSettingsProvider), so it has no @ExtensionImpl here
public class BazelJVMExperimentalSettingsProvider implements BazelSettingsProvider {
    @Override
    public UnnamedConfigurable createConfigurable(Project project) {
        return new BazelJVMExperimentalConfigurable(project);
    }

    @Override
    public List<String> searchIndexKeys() {
        return List.of(
            "project.settings.plugin.hotswap.enabled.checkbox.text"
        );
    }

    // consulo: upstream extension point bazelExperimentalSettingsProvider
    @Override
    public boolean isExperimental() {
        return true;
    }
}

class BazelJVMExperimentalConfigurable implements UnnamedConfigurable {
    private final Project myProject;

    private final JBCheckBox myHotswapEnabledCheckBox;
    private final JBCheckBox myEnableKotlinCoroutineDebugCheckBox;

    private BazelJVMProjectSettings myCurrentJVMProjectSettings;

    BazelJVMExperimentalConfigurable(Project project) {
        myProject = project;
        myCurrentJVMProjectSettings = BazelJVMProjectSettings.getBazelJVMProjectSettings(project);

        myHotswapEnabledCheckBox = initHotSwapEnabledCheckBox();
        myEnableKotlinCoroutineDebugCheckBox = initEnableKotlinCoroutineDebugCheckBox();
    }

    public JComponent createComponent() {
        // consulo: Kotlin UI DSL panel -> FormBuilder, contextHelp -> tooltip row
        return FormBuilder.createFormBuilder()
            .addComponent(myHotswapEnabledCheckBox)
            .addTooltip(BazelPluginLocalize.projectSettingsPluginHotswapEnabledCheckboxHelpText().get())
            .addComponent(myEnableKotlinCoroutineDebugCheckBox)
            .getPanel();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        return TargetAWT.wrap(createComponent());
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return !myCurrentJVMProjectSettings.equals(BazelJVMProjectSettings.getBazelJVMProjectSettings(myProject));
    }

    @RequiredUIAccess
    @Override
    public void apply() {
        BazelJVMProjectSettings.setBazelJVMProjectSettings(myProject, myCurrentJVMProjectSettings);
    }

    private JBCheckBox initHotSwapEnabledCheckBox() {
        JBCheckBox checkBox = new JBCheckBox(BazelPluginLocalize.projectSettingsPluginHotswapEnabledCheckboxText().get());
        checkBox.setEnabled(!BazelFeatureFlags.getFastBuildEnabled());
        checkBox.setSelected(myCurrentJVMProjectSettings.hotSwapEnabled());
        checkBox.addItemListener(e ->
            myCurrentJVMProjectSettings =
                new BazelJVMProjectSettings(checkBox.isSelected(), myCurrentJVMProjectSettings.enableKotlinCoroutineDebug())
        );
        return checkBox;
    }

    private JBCheckBox initEnableKotlinCoroutineDebugCheckBox() {
        JBCheckBox checkBox = new JBCheckBox(BazelPluginLocalize.projectSettingsPluginEnableKotlinCoroutineDebugCheckboxText().get());
        checkBox.setSelected(myCurrentJVMProjectSettings.enableKotlinCoroutineDebug());
        checkBox.addItemListener(e ->
            myCurrentJVMProjectSettings =
                new BazelJVMProjectSettings(myCurrentJVMProjectSettings.hotSwapEnabled(), checkBox.isSelected())
        );
        return checkBox;
    }
}
