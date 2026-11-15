package org.jetbrains.bazel.debug.configuration;

import consulo.application.dumb.DumbAware;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;

public class StarlarkDebugConfigurationFactory extends ConfigurationFactory implements DumbAware {
    public StarlarkDebugConfigurationFactory(ConfigurationType type) {
        super(type);
    }

    @Override
    public RunConfiguration createTemplateConfiguration(Project project) {
        return new StarlarkDebugConfiguration(project, this, "");
    }

    // consulo: ConfigurationFactory has no getOptionsClass(), StarlarkDebugConfiguration serializes its Options itself

    @Override
    public String getId() {
        return "StarlarkDebugFactory";
    }

    // consulo: ConfigurationFactory has no isEditableInDumbMode() (upstream returns true)
}
