package org.jetbrains.bazel.run.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.ConfigurationInfoProvider;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.execution.coverage.CoverageExecutor;
import consulo.execution.coverage.CoverageHelper;
import consulo.execution.coverage.CoverageRunnerData;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.GenericProgramRunner;
import consulo.execution.runner.RunContentBuilder;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

@ExtensionImpl(id = "bazelCoverageProgramRunner")
public class BazelCoverageProgramRunner extends GenericProgramRunner<RunnerSettings> {
    // consulo: upstream private top-level const of BazelCoverageProgramRunner.kt
    private static final String RUNNER_ID = "BazelCoverageProgramRunner";

    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        if (!executorId.equals(CoverageExecutor.EXECUTOR_ID)) {
            return false;
        }
        if (!(profile instanceof BazelRunConfiguration bazelRunConfiguration)) {
            return false;
        }
        return BazelCoverageEngine.getInstance().isApplicableTo(bazelRunConfiguration);
    }

    @Override
    public @Nullable RunnerSettings createConfigurationData(ConfigurationInfoProvider settingsProvider) {
        return new CoverageRunnerData();
    }

    @Override
    protected @Nullable RunContentDescriptor doExecute(RunProfileState state, ExecutionEnvironment environment) throws ExecutionException {
        if (!(environment.getRunProfile() instanceof BazelRunConfiguration configuration)) {
            return null;
        }
        if (!(CoverageEnabledConfiguration.getOrCreate(configuration)
            instanceof BazelCoverageEnabledConfiguration coverageEnabledConfiguration)) {
            return null;
        }
        String coverageFilePath = coverageEnabledConfiguration.getCoverageFilePath();
        if (coverageFilePath == null) {
            return null;
        }
        Path coverageOutputDirectory = Path.of(coverageFilePath);

        if (!(state instanceof BazelTestCommandLineState testState)) {
            return null;
        }
        AtomicInteger coverageFileIndex = new AtomicInteger();
        testState.setCoverageReportListener(coverageReport -> {
            // There can be several coverage files, e.g. if we run several test targets at once. Save them to a directory.
            Path destinationPath = coverageOutputDirectory.resolve(String.valueOf(coverageFileIndex.getAndIncrement()));
            try {
                Files.createDirectories(destinationPath.getParent());
                Files.copy(coverageReport, destinationPath);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        ExecutionResult executionResult = testState.execute(environment.getExecutor(), this);
        CoverageHelper.attachToProcess(configuration, executionResult.getProcessHandler(), environment.getRunnerSettings());

        // consulo: ExecutionUiService.showRunContent(executionResult, environment) -> RunContentBuilder
        return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
    }
}
