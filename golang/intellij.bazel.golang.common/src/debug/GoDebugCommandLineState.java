package org.jetbrains.bazel.golang.debug;

import com.goide.compiler.GoCompilerRunner;
import com.goide.runconfig.GoRunConfigurationBase;
import com.goide.runconfig.GoRunningState;
import com.goide.runconfig.application.GoApplicationConfiguration;
import com.goide.runconfig.application.GoApplicationRunningState;
import com.goide.runconfig.testing.GoTestRunConfiguration;
import com.goide.runconfig.testing.GoTestRunningState;
import consulo.bazel.go.localize.BazelGoLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.module.Module;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.run.state.Common.EnvironmentVariablesDataOptions;
import org.jetbrains.bazel.run.state.GenericRunState;
import org.jetbrains.bazel.run.state.GenericTestState;
import org.jetbrains.bazel.utils.ExecutableInfo;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

// consulo: IntelliJ GoBuildingRunningState (isDebug / getBuildingTarget / createBuildExecutor, a state that runs a prebuilt binary
// under Delve) does not exist; the states are GoRunningState as far as it goes, and createDebugTarget() describes the launch of the
// binary for consulo-native-dev, which starts it under Delve (BazelGoDebugRunner)
interface GoDebugCommandLineState {

    void patchNativeState();

    /**
     * The binary that {@link ScriptPathDebugBeforeRunTaskProvider} prepared, launched as the patched state runs it.
     * Called after {@link #patchNativeState()}.
     */
    // consulo: upstream GoBuildingRunningState.execute() starts `dlv exec <output file>` with the configuration of the state
    NativeDebugTarget createDebugTarget() throws ExecutionException;

    static ExecutableInfo getExecutableInfo(ExecutionEnvironment environment) throws ExecutionException {
        AtomicReference<ExecutableInfo> executableInfoRef = environment.getCopyableUserData(BazelGoTestHandler.EXECUTABLE_KEY);
        ExecutableInfo executableInfo = executableInfoRef != null ? executableInfoRef.get() : null;
        if (executableInfo == null) {
            throw new ExecutionException(BazelGoLocalize.goDebugErrorNoExecutable());
        }
        return executableInfo;
    }

    // consulo: the environment of the command line is passed, Delve gives the program its own parent environment
    static NativeDebugTarget createDebugTarget(GeneralCommandLine commandLine, GoRunningState<?> state) {
        File workDirectory = commandLine.getWorkDirectory();
        NativeDebugTarget target = NativeDebugTarget.launch(
            Path.of(commandLine.getExePath()),
            commandLine.getParametersList().getList(),
            workDirectory == null ? null : workDirectory.toPath(),
            commandLine.getEnvironment(),
            null
        );
        // consulo: upstream BazelDlvPosConverterFactory (dlv.positionConverterFactory) maps the paths of the binary
        GoRunConfigurationBase<?> configuration = state.getConfiguration();
        return target.withSetup(BazelDlvDebuggerSetup.create(configuration.getProject(), configuration.getConfigurationModule().getModule()));
    }

    // consulo: upstream private top-level extension `fun GoBuildingRunningState<*>.applyExecutableInfo(executableInfo)`; the kind and
    //   the output directory are set by the states, the configurations of consulo-google-go have no common base for them
    static void applyExecutableInfo(GoRunningState<?> receiver, ExecutableInfo executableInfo) {
        GoRunConfigurationBase<?> configuration = receiver.getConfiguration();
        // consulo: the custom environment is replaced instead of mutated in place
        Map<String, String> customEnvironment = new HashMap<>(configuration.getCustomEnvironment());
        for (Map.Entry<String, String> entry : executableInfo.envVars().entrySet()) {
            customEnvironment.put(entry.getKey(), entry.getValue());
        }
        configuration.setCustomEnvironment(customEnvironment);
        configuration.setWorkingDirectory(executableInfo.workingDir().toString());
        // consulo: upstream setOutputFilePath(executableInfo.binary.pathString); GoApplicationRunningState runs the output file of
        //   the environment
        receiver.getEnvironment().putUserData(GoCompilerRunner.OUTPUT_FILE, executableInfo.binary().toFile());
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

    // consulo: upstream isDebug() = true, getBuildingTarget() = null and createBuildExecutor() = null (GoBuildingRunningState) do not
    //   exist; the state is never run, createDebugTarget() gives its prebuilt binary to the debugger

    @Override
    public void patchNativeState() {
        AtomicReference<ExecutableInfo> executableInfoRef = myEnvironment.getCopyableUserData(BazelGoTestHandler.EXECUTABLE_KEY);
        ExecutableInfo executableInfo = executableInfoRef != null ? executableInfoRef.get() : null;
        if (executableInfo != null) {
            GoDebugCommandLineState.applyExecutableInfo(this, executableInfo);
            getConfiguration().setKind(GoApplicationConfiguration.Kind.PACKAGE);
            // the binary is not a temporary output file, it stays after the debug session
            Path outputDirectory = executableInfo.binary().getParent();
            getConfiguration().setOutputDirectory(outputDirectory != null ? outputDirectory.toString() : null);
        }
        GoDebugCommandLineState.applyState(this, mySettings.getEnv(), mySettings.getProgramArguments());
    }

    @Override
    public NativeDebugTarget createDebugTarget() throws ExecutionException {
        GoDebugCommandLineState.getExecutableInfo(myEnvironment);
        return GoDebugCommandLineState.createDebugTarget(createProgramCommandLine(), this);
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

    // consulo: upstream isDebug() = true, getBuildingTarget() = null and createBuildExecutor() = null (GoBuildingRunningState) do not
    //   exist; the state is never run, createDebugTarget() gives its prebuilt binary to the debugger

    @Override
    public void patchNativeState() {
        AtomicReference<ExecutableInfo> executableInfoRef = myEnvironment.getCopyableUserData(BazelGoTestHandler.EXECUTABLE_KEY);
        ExecutableInfo executableInfo = executableInfoRef != null ? executableInfoRef.get() : null;
        if (executableInfo != null) {
            GoDebugCommandLineState.applyExecutableInfo(this, executableInfo);
            myConfiguration.setKind(GoTestRunConfiguration.Kind.PACKAGE);
        }
        GoDebugCommandLineState.applyState(this, mySettings.getEnv(), mySettings.getProgramArguments());
        Map<String, String> customEnvironment = new HashMap<>(myConfiguration.getCustomEnvironment());
        String testFilter = mySettings.getTestFilter();
        customEnvironment.put(Utils.BAZEL_TEST_FILTER_ENV, testFilter != null && !testFilter.isBlank() ? testFilter : ".*");
        // GO_TEST_WRAP=0 ensures the tests are run in the debugged process, not a child process
        customEnvironment.put("GO_TEST_WRAP", "0");
        myConfiguration.setCustomEnvironment(customEnvironment);
    }

    @Override
    public NativeDebugTarget createDebugTarget() throws ExecutionException {
        ExecutableInfo executableInfo = GoDebugCommandLineState.getExecutableInfo(myEnvironment);
        // consulo: GoTestRunningState.patchExecutor builds `go test`; the test binary runs like the program of
        //   GoApplicationRunningState.createProgramCommandLine, with the environment, the working directory and the parameters of the
        //   configuration
        GeneralCommandLine commandLine = createCommonExecutor()
            .withExePath(executableInfo.binary().toString())
            .withParameterString(myConfiguration.getParams())
            .createCommandLine();
        return GoDebugCommandLineState.createDebugTarget(commandLine, this);
    }
}
