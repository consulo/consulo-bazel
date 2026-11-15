package org.jetbrains.bazel.debug.configuration;

import consulo.bazel.execution.ui.SettingsEditorRunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.localize.LocalizeValue;
import java.util.List;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.execution.configuration.RunConfigurationWithSuppressedDefaultRunAction;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

public class StarlarkDebugConfiguration extends RunConfigurationBase implements RunConfigurationWithSuppressedDefaultRunAction {
    private final Options myOptions = new Options();

    public StarlarkDebugConfiguration(
        Project project,
        @Nullable ConfigurationFactory configurationFactory,
        String name
    ) {
        super(project, configurationFactory, name);
    }

    @Override
    public Options getOptions() {
        return myOptions;
    }

    public String getTarget() {
        String target = myOptions.getStarlarkDebugTarget();
        return target != null ? target : "";
    }

    public void setTarget(String target) {
        myOptions.setStarlarkDebugTarget(target);
    }

    @Override
    public void writeExternal(Element element) {
        XmlSerializer.serializeInto(myOptions, element);
    }

    @Override
    public void readExternal(Element element) {
        XmlSerializer.deserializeInto(myOptions, element);
    }

    @Override
    public StarlarkDebugConfiguration clone() {
        RunConfiguration it = super.clone();
        StarlarkDebugConfiguration result = new StarlarkDebugConfiguration(it.getProject(), it.getFactory(), it.getName());
        result.myOptions.setStarlarkDebugTarget(this.myOptions.getStarlarkDebugTarget());
        return result;
    }

    // consulo: RunConfigurationBase has no default checkConfiguration(), upstream inherits an empty one
    @Override
    public void checkConfiguration() {
    }

    @Override
    public RunProfileState getState(Executor executor, ExecutionEnvironment environment) {
        return new StarlarkDebugConfigurationState(getProject(), getTarget(), environment);
    }

    // consulo: RunConfiguration has no getConfigurationEditor(), the editor is shown through createFragments()
    public SettingsEditor<StarlarkDebugConfiguration> getConfigurationEditor() {
        return new StarlarkDebugSettingsEditor();
    }

    // consulo: the whole upstream editor (getConfigurationEditor()) is one Consulo run configuration fragment
    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(
            new SettingsEditorRunConfigurationFragment<StarlarkDebugConfiguration>(
                "starlark.debug.configuration",
                LocalizeValue.of(getName()),
                this::getConfigurationEditor
            )
        );
    }

    public static class Options extends RunConfigurationOptions {
        private @Nullable String myStarlarkDebugTarget;

        // platform code does not use bundle messages here
        // example: com.intellij.execution.application.JvmMainMethodRunConfigurationOptions
        // consulo: upstream @com.intellij.configurationStore.Property(description = "Target to debug") has no Consulo counterpart
        @Attribute("starlark_debug_target")
        public @Nullable String getStarlarkDebugTarget() {
            return myStarlarkDebugTarget;
        }

        public void setStarlarkDebugTarget(@Nullable String starlarkDebugTarget) {
            myStarlarkDebugTarget = starlarkDebugTarget;
        }
    }
}
