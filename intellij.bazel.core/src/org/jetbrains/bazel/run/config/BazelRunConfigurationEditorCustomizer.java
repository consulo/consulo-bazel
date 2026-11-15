package org.jetbrains.bazel.run.config;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.bazel.execution.ui.SettingsEditorFragment;

import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelRunConfigurationEditorCustomizer {

    void fragmentsCreated(BazelRunConfiguration configuration, List<SettingsEditorFragment<BazelRunConfiguration, ?>> fragments);


    void fragmentsReset(BazelRunConfiguration configuration, List<SettingsEditorFragment<BazelRunConfiguration, ?>> fragments);

    // consulo: upstream EP_NAME is replaced by Application.get().getExtensionPoint(BazelRunConfigurationEditorCustomizer.class)
}
