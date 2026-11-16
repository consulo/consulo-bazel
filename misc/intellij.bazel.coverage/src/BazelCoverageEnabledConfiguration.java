package org.jetbrains.bazel.run.coverage;

import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.CoverageEnabledConfiguration;

public class BazelCoverageEnabledConfiguration extends CoverageEnabledConfiguration {
    // consulo: the Consulo CoverageEnabledConfiguration constructor takes no runner, it is set afterwards
    public BazelCoverageEnabledConfiguration(RunConfigurationBase configuration) {
        super(configuration);
        setCoverageRunner(BazelCoverageRunner.getInstance());
    }
}
