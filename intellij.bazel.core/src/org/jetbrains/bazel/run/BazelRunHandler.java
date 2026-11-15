package org.jetbrains.bazel.run;

import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunConfigurationExtensionsManager;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ExecutionException;
import org.jspecify.annotations.Nullable;

/**
 * Supports the run configuration flow for BSP run configurations.
 * The lifetime of a handler is shorter than the lifetime of a run configuration and fully owned by the run configuration.
 *
 * <p>Provides language-specific configuration state, validation, presentation, and runner.
 */
public interface BazelRunHandler {
    BazelRunConfigurationState<?> getState();

    /**
     * The name of the run handler (shown in the UI).
     */
    String getName();

    boolean isTestHandler();

    RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) throws ExecutionException;

    default @Nullable RunConfigurationExtensionsManager<? super RunConfigurationBase, ?> getExtensionsManager() {
        return null;
    }
}
