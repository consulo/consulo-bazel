package org.jetbrains.bazel.run.handler;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState;
import org.jetbrains.bazel.run.state.GenericTestState;

class GenericBazelTestHandler implements BazelRunHandler {
    private final GenericTestState myState = new GenericTestState();

    @Override
    public GenericTestState getState() {
        return myState;
    }

    @Override
    public String getName() {
        return BazelPluginLocalize.genericTestHandlerName().get();
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }

    @Override
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) {
        return new BazelTestCommandLineState(environment, myState);
    }
}
