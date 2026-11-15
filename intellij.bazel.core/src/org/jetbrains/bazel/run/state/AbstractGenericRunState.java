package org.jetbrains.bazel.run.state;

import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentContainer;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jetbrains.bazel.run.BazelRunConfigurationState;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

// consulo: declared in upstream GenericRunState.kt, see GenericRunState
// consulo: upstream @com.intellij.configurationStore.Property(description = ...) annotations have no Consulo counterpart
public class AbstractGenericRunState<T extends AbstractGenericRunState<T>>
    extends BazelRunConfigurationState<T>
    implements GenericRunConfigurationState {
    private @Nullable String myProgramArguments;

    private @Nullable String myAdditionalBazelParams;

    // TODO: handle passing system environment variables
    private Common.EnvironmentVariablesDataOptions myEnv = new Common.EnvironmentVariablesDataOptions();

    // Arguments
    @Attribute("programArguments")
    @Override
    public @Nullable String getProgramArguments() {
        return myProgramArguments;
    }

    @Override
    public void setProgramArguments(@Nullable String programArguments) {
        myProgramArguments = programArguments;
    }

    // Bazel parameters
    @Override
    public @Nullable String getAdditionalBazelParams() {
        return myAdditionalBazelParams;
    }

    @Override
    public void setAdditionalBazelParams(@Nullable String additionalBazelParams) {
        myAdditionalBazelParams = additionalBazelParams;
    }

    // Environment variables
    @Override
    public Common.EnvironmentVariablesDataOptions getEnv() {
        return myEnv;
    }

    @Override
    public void setEnv(Common.EnvironmentVariablesDataOptions env) {
        myEnv = env;
    }

    @Override
    public Collection<SettingsEditorFragment<BazelRunConfiguration, ?>> createFragments(BazelRunConfiguration configuration) {
        return SettingsEditorFragmentContainer.<BazelRunConfiguration>fragments(container -> {
            container.add(Common.bazelParamsFragment());
            container.add(Common.programArgumentsFragment());
            Common.addEnvironmentFragment(container);
        });
    }
}
