package org.jetbrains.bazel.debug.configuration;

import consulo.application.dumb.DumbAware;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jetbrains.bazel.assets.BazelPluginIcons;

// consulo: not registered as an extension, like upstream (no <configurationType> entry); it is created by StarlarkDebugAction
public class StarlarkDebugConfigurationType implements ConfigurationType, DumbAware {
    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.starlarkDebugConfigTypeName();
    }

    @Override
    public LocalizeValue getConfigurationTypeDescription() {
        return BazelPluginLocalize.starlarkDebugConfigTypeDescription();
    }

    @Override
    public Image getIcon() {
        return BazelPluginIcons.bazel();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new StarlarkDebugConfigurationFactory(this)};
    }

    public static final String ID = "RemoteStarlarkDebug";
}
