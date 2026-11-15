package org.jetbrains.bazel.java.coverage;

import com.intellij.java.execution.impl.DefaultJavaProgramRunner;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.ConfigurationInfoProvider;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.coverage.CoverageExecutor;
import consulo.execution.coverage.CoverageRunnerData;

/**
 * {@link com.intellij.java.coverage.DefaultJavaCoverageRunner}
 */
// TODO consulo: upstream order="before bazelCoverageProgramRunner"; misc/intellij.bazel.coverage is not ported yet
@ExtensionImpl(id = "BazelJavaCoverageRunner")
public class BazelJavaCoverageRunner extends DefaultJavaProgramRunner {
    private static final String RUNNER_ID = "BazelJavaCoverageRunner";

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        return executorId.equals(CoverageExecutor.EXECUTOR_ID) && BazelJavaAgentCoverageEngine.isJavaAgentCoverageApplicableTo(profile);
    }

    @Override
    public RunnerSettings createConfigurationData(ConfigurationInfoProvider settingsProvider) {
        return new CoverageRunnerData();
    }

    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }
}
