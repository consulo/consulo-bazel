package org.jetbrains.bazel.python.run;

import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import org.jetbrains.bazel.python.debug.PythonDebugCommandLineState;
import org.jetbrains.bazel.run.BazelCommandLineStateBase;
import org.jetbrains.bazel.run.BazelRunConfigurationState;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.state.Common.HasBazelParams;
import org.jetbrains.bazel.run.state.Common.HasProgramArguments;

public abstract class PythonBazelHandler<T extends BazelRunConfigurationState<T> & HasProgramArguments & HasBazelParams>
    implements BazelRunHandler {
    @Override
    public abstract T getState();

    @Override
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) {
        if (executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            return new PythonDebugCommandLineState(
                environment,
                getState().getProgramArguments(),
                getState().getAdditionalBazelParams()
            );
        }
        else {
            return createCommandLineState(environment);
        }
    }

    protected abstract BazelCommandLineStateBase createCommandLineState(ExecutionEnvironment environment);
}
