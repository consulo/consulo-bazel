package org.jetbrains.bazel.run.state;

import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentContainer;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jetbrains.bazel.run.BazelRunConfigurationState;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

// consulo: declared in upstream GenericTestState.kt, see GenericTestState
// consulo: upstream @com.intellij.configurationStore.Property(description = ...) annotations have no Consulo counterpart
public class AbstractGenericTestState<T extends AbstractGenericTestState<T>>
    extends BazelRunConfigurationState<T>
    implements GenericRunConfigurationState, Test.HasTestFilter, CoverageWithBazel.HasCoverageInstrumentationFilter {
    private @Nullable String myTestFilter;

    private @Nullable String myProgramArguments;

    // TODO: this field will be duplicated in the xml, figure out what's causing it
    //  (probably related to the fact
    //   that writeExternal is called multiple times as somehow the change detection is not working)
    private Common.EnvironmentVariablesDataOptions myEnv = new Common.EnvironmentVariablesDataOptions();

    private @Nullable String myAdditionalBazelParams;

    private @Nullable String myCoverageInstrumentationFilter;

    // Test filter
    @Attribute("testFilter")
    @Override
    public @Nullable String getTestFilter() {
        return myTestFilter;
    }

    @Override
    public void setTestFilter(@Nullable String testFilter) {
        myTestFilter = testFilter;
    }

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

    // Environment variables
    @Override
    public Common.EnvironmentVariablesDataOptions getEnv() {
        return myEnv;
    }

    @Override
    public void setEnv(Common.EnvironmentVariablesDataOptions env) {
        myEnv = env;
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

    // Coverage instrumentation filter
    @Attribute("coverageInstrumentationFilter")
    @Override
    public @Nullable String getCoverageInstrumentationFilter() {
        return myCoverageInstrumentationFilter;
    }

    @Override
    public void setCoverageInstrumentationFilter(@Nullable String coverageInstrumentationFilter) {
        myCoverageInstrumentationFilter = coverageInstrumentationFilter;
    }

    @Override
    public Collection<SettingsEditorFragment<BazelRunConfiguration, ?>> createFragments(BazelRunConfiguration configuration) {
        return SettingsEditorFragmentContainer.<BazelRunConfiguration>fragments(container -> {
            container.add(Common.bazelParamsFragment());
            Test.addTestFilterFragment(container);
            container.add(Common.programArgumentsFragment());
            Common.addEnvironmentFragment(container);
            container.add(new CoverageWithBazel.CoverageWithBazelFragment());
        });
    }
}
