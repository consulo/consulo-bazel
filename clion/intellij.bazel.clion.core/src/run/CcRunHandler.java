package org.jetbrains.bazel.clion.run;

import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState;
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.GenericRunState;
import org.jetbrains.bazel.run.state.GenericTestState;
import org.jspecify.annotations.Nullable;

// consulo: the nested upstream CcRunHandler.Provider and CcTestHandler.Provider have their own files (nested @ExtensionImpl classes
// are not supported by the annotation processor), see CcRunHandlerProvider and CcTestHandlerProvider
public class CcRunHandler implements BazelRunHandler {
    private final GenericRunState myState = new GenericRunState();

    @Override
    public GenericRunState getState() {
        return myState;
    }

    @Override
    public String getName() {
        return BazelCLionCoreLocalize.runHandlerCcName().get();
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }

    @Override
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) {
        if (executor instanceof DefaultDebugExecutor) {
            return new CcDebugCommandLineState(environment, myState);
        }
        else {
            return new BazelRunCommandLineState(environment, myState);
        }
    }

    // consulo: upstream public top-level class of CcRunHandler.kt
    public static class CcTestHandler implements BazelRunHandler {
        private final GenericTestState myState = new GenericTestState();

        @Override
        public GenericTestState getState() {
            return myState;
        }

        @Override
        public String getName() {
            return BazelCLionCoreLocalize.testHandlerCcName().get();
        }

        @Override
        public boolean isTestHandler() {
            return true;
        }

        @Override
        public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) {
            if (executor instanceof DefaultDebugExecutor) {
                return new CcDebugCommandLineState(environment, myState);
            }
            else {
                return new BazelTestCommandLineState(environment, myState);
            }
        }
    }
}

// TODO consulo: not registered (upstream cidr debugger.debugProfilesEnabler), CidrDebugProfilesEnabler
//   (com.intellij.cidr.debugger.profiles) does not exist; consulo-native-dev chooses the debugger per session
//   (NativeDebugSessionStarter), so CcDebugRunner needs no enabler
class DebugProfileEnabler /* : CidrDebugProfilesEnabler */ {
    public DebugProfileEnabler() {
    }

    public boolean debugProfilesEnabled(Project project, @Nullable RunConfiguration runConfiguration) {
        return runConfiguration instanceof BazelRunConfiguration bazelRunConfiguration &&
            (bazelRunConfiguration.getHandler() instanceof CcRunHandler ||
                bazelRunConfiguration.getHandler() instanceof CcRunHandler.CcTestHandler);
    }
}
