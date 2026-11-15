package org.jetbrains.bazel.run.config;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.SimpleConfigurationType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.assets.BazelPluginIcons;

import static org.jetbrains.bazel.config.BazelPluginConstants.BAZEL_DISPLAY_NAME;

@ExtensionImpl
public class BazelRunConfigurationType extends SimpleConfigurationType implements DumbAware {
    public static final String ID = "BazelRunConfigurationType";

    public BazelRunConfigurationType() {
        super(
            /* id = */ ID,
            /* name = */ LocalizeValue.of(BAZEL_DISPLAY_NAME),
            /* description = */ BazelPluginLocalize.runconfigRunDescription(BAZEL_DISPLAY_NAME),
            // consulo: upstream passes a NotNullLazyValue, Consulo takes the (lazily loaded) image
            /* icon = */ BazelPluginIcons.bazel()
        );
    }

    @Override
    public RunConfiguration createTemplateConfiguration(Project project) {
        return new BazelRunConfiguration(project, "", this);
    }

    // consulo: ConfigurationFactory has no isEditableInDumbMode(), ConfigurationType.isDumbAware() is true by default
    // override fun isEditableInDumbMode(): Boolean = true

    public static ConfigurationFactory getBazelRunConfigurationFactory() {
        return ConfigurationTypeUtil.findConfigurationType(BazelRunConfigurationType.class).getConfigurationFactories()[0];
    }
}
