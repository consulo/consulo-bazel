package org.jetbrains.bazel.run.synthetic;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.execution.event.ExecutionListener;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ProcessHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

@TopicImpl(ComponentScope.PROJECT)
public class SyntheticRunExecutionListener implements ExecutionListener {
    @Override
    public void processTerminated(
        String executorId,
        ExecutionEnvironment env,
        ProcessHandler handler,
        int exitCode
    ) {
        if (!(env.getRunProfile() instanceof BazelRunConfiguration runConfig)) {
            return;
        }
        // consulo: upstream removeUserData()
        SyntheticRunTargetSession syntheticTemplate = runConfig.getUserData(GenerateSyntheticTargetRunTaskProvider.SYNTHETIC_BUILD_SESSION);
        runConfig.putUserData(GenerateSyntheticTargetRunTaskProvider.SYNTHETIC_BUILD_SESSION, null);
        if (syntheticTemplate != null) {
            try {
                Files.deleteIfExists(syntheticTemplate.buildFilePath());
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
