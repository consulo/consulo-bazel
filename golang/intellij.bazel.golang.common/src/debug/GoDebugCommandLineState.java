package org.jetbrains.bazel.golang.debug;

import com.goide.runconfig.GoRunConfigurationBase;
import com.goide.runconfig.GoRunningState;
import com.goide.runconfig.application.GoApplicationConfiguration;
import com.goide.runconfig.application.GoApplicationRunningState;
import com.goide.runconfig.testing.GoTestRunConfiguration;
import com.goide.runconfig.testing.GoTestRunningState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.module.Module;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.run.state.Common.EnvironmentVariablesDataOptions;
import org.jetbrains.bazel.run.state.GenericRunState;
import org.jetbrains.bazel.run.state.GenericTestState;
import org.jetbrains.bazel.utils.ExecutableInfo;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

// TODO consulo: IntelliJ GoBuildingRunningState / GoBuildingRunConfiguration (a state that runs a prebuilt binary for Delve) are
//  missing in consulo-google-go, and there is no Delve debugger (com.goide.dlv); the states are ported on GoRunningState as far as
//  it goes and are only used by the unregistered BazelGoDebugRunner
interface GoDebugCommandLineState {

    void patchNativeState();

    // consulo: upstream private top-level extension `fun GoBuildingRunningState<*>.applyExecutableInfo(executableInfo)`
    static void applyExecutableInfo(GoRunningState<?> receiver, ExecutableInfo executableInfo) {
        GoRunConfigurationBase<?> configuration = receiver.getConfiguration();
        // consulo: the custom environment is replaced instead of mutated in place
        Map<String, String> customEnvironment = new HashMap<>(configuration.getCustomEnvironment());
        for (Map.Entry<String, String> entry : executableInfo.envVars().entrySet()) {
            customEnvironment.put(entry.getKey(), entry.getValue());
        }
        configuration.setCustomEnvironment(customEnvironment);
        // TODO consulo: GoBuildingRunConfiguration.kind / outputDirectory are missing
        // kind = GoBuildingRunConfiguration.Kind.PACKAGE
        // outputDirectory = executableInfo.binary.parent?.pathString
        configuration.setWorkingDirectory(executableInfo.workingDir().toString());
        // TODO consulo: GoBuildingRunningState.setOutputFilePath is missing
        // setOutputFilePath(executableInfo.binary.pathString)
    }

    // consulo: upstream private top-level extension `fun GoBuildingRunningState<*>.applyState(env, programArguments)`
    static void applyState(
        GoRunningState<?> receiver,
        EnvironmentVariablesDataOptions env,
        @Nullable String programArguments
    ) {
        GoRunConfigurationBase<?> configuration = receiver.getConfiguration();
        Map<String, String> customEnvironment = new HashMap<>(configuration.getCustomEnvironment());
        for (Map.Entry<String, String> entry : env.getEnvs().entrySet()) {
            customEnvironment.put(entry.getKey(), entry.getValue());
        }
        configuration.setCustomEnvironment(customEnvironment);
        configuration.setPassParentEnvironment(env.isPassParentEnvs());
        if (programArguments != null) {
            configuration.setParams(programArguments);
        }
    }
}

class GoRunWithDebugCommandLineState extends GoApplicationRunningState implements GoDebugCommandLineState {
    private final ExecutionEnvironment myEnvironment;
    private final GenericRunState mySettings;

    GoRunWithDebugCommandLineState(
        ExecutionEnvironment environment,
        Module module,
        GoApplicationConfiguration configuration,
        GenericRunState settings
    ) {
        super(environment, module, configuration);
        myEnvironment = environment;
        mySettings = settings;
    }

    // TODO consulo: GoBuildingRunningState.isDebug / getBuildingTarget / createBuildExecutor are missing
    // override fun isDebug(): Boolean = true
    // override fun getBuildingTarget(): List<GoCommandLineParameter>? = null
    // override fun createBuildExecutor(): GoExecutor? = null

    @Override
    public void patchNativeState() {
        AtomicReference<ExecutableInfo> executableInfoRef = myEnvironment.getCopyableUserData(BazelGoTestHandler.EXECUTABLE_KEY);
        ExecutableInfo executableInfo = executableInfoRef != null ? executableInfoRef.get() : null;
        if (executableInfo != null) {
            GoDebugCommandLineState.applyExecutableInfo(this, executableInfo);
        }
        GoDebugCommandLineState.applyState(this, mySettings.getEnv(), mySettings.getProgramArguments());
    }
}

class GoTestWithDebugCommandLineState extends GoTestRunningState implements GoDebugCommandLineState {
    private final ExecutionEnvironment myEnvironment;
    private final GoTestRunConfiguration myConfiguration;
    private final GenericTestState mySettings;

    GoTestWithDebugCommandLineState(
        ExecutionEnvironment environment,
        Module module,
        GoTestRunConfiguration configuration,
        GenericTestState settings
    ) {
        super(environment, module, configuration);
        myEnvironment = environment;
        myConfiguration = configuration;
        mySettings = settings;
    }

    // TODO consulo: GoBuildingRunningState.isDebug / getBuildingTarget / createBuildExecutor are missing
    // override fun isDebug(): Boolean = true
    // override fun getBuildingTarget(): List<GoCommandLineParameter>? = null
    // override fun createBuildExecutor(): GoExecutor? = null

    @Override
    public void patchNativeState() {
        AtomicReference<ExecutableInfo> executableInfoRef = myEnvironment.getCopyableUserData(BazelGoTestHandler.EXECUTABLE_KEY);
        ExecutableInfo executableInfo = executableInfoRef != null ? executableInfoRef.get() : null;
        if (executableInfo != null) {
            GoDebugCommandLineState.applyExecutableInfo(this, executableInfo);
        }
        GoDebugCommandLineState.applyState(this, mySettings.getEnv(), mySettings.getProgramArguments());
        Map<String, String> customEnvironment = new HashMap<>(myConfiguration.getCustomEnvironment());
        String testFilter = mySettings.getTestFilter();
        customEnvironment.put(Utils.BAZEL_TEST_FILTER_ENV, testFilter != null && !testFilter.isBlank() ? testFilter : ".*");
        // GO_TEST_WRAP=0 ensures the tests are run in the debugged process, not a child process
        customEnvironment.put("GO_TEST_WRAP", "0");
        myConfiguration.setCustomEnvironment(customEnvironment);
    }
}
