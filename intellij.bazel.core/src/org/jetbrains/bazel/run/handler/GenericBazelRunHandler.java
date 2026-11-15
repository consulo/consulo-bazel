package org.jetbrains.bazel.run.handler;

import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState;
import org.jetbrains.bazel.run.state.GenericRunState;

class GenericBazelRunHandler implements BazelRunHandler {
    private final GenericRunState myState = new GenericRunState();

    @Override
    public GenericRunState getState() {
        return myState;
    }

    @Override
    public String getName() {
        return "Generic BSP Run Handler";
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }

    @Override
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) {
        return new BazelRunCommandLineState(environment, myState);
    }
}
