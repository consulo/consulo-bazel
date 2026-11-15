package org.jetbrains.bazel.debug.configuration;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;


public class StarlarkDebugSettingsEditor extends SettingsEditor<StarlarkDebugConfiguration> {
    private final JBTextField myTargetField = new JBTextField();

    @Override
    protected void resetEditorFrom(StarlarkDebugConfiguration config) {
        myTargetField.setText(config.getTarget());
    }

    @Override
    protected void applyEditorTo(StarlarkDebugConfiguration config) {
        config.setTarget(myTargetField.getText());
    }

    // consulo: upstream createEditor() builds the row with the Kotlin UI DSL
    @Override
    @RequiredUIAccess
    protected Component createUIComponent() {
        return TargetAWT.wrap(
            FormBuilder.createFormBuilder()
                .addLabeledComponent(BazelPluginLocalize.starlarkDebugConfigTargetLabel().get(), myTargetField)
                .getPanel()
        );
    }
}
