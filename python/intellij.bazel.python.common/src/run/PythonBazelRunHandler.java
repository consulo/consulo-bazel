package org.jetbrains.bazel.python.run;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import org.jetbrains.bazel.run.BazelCommandLineStateBase;
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState;
import org.jetbrains.bazel.run.state.GenericRunState;

// consulo: the nested upstream PythonBazelRunHandler.Provider has its own file (nested @ExtensionImpl classes are not supported by
// the annotation processor), see PythonBazelRunHandlerProvider
public class PythonBazelRunHandler extends PythonBazelHandler<GenericRunState> {
    private final GenericRunState myState = new GenericRunState();

    @Override
    public String getName() {
        return BazelPluginLocalize.pythonRunHandlerName().get();
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }

    @Override
    public GenericRunState getState() {
        return myState;
    }

    @Override
    protected BazelCommandLineStateBase createCommandLineState(ExecutionEnvironment environment) {
        return new BazelRunCommandLineState(environment, myState);
    }
}
