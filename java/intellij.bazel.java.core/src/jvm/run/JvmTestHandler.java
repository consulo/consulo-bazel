package org.jetbrains.bazel.jvm.run;

import com.intellij.java.execution.impl.JavaRunConfigurationExtensionManager;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.BeforeRunTask;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunConfigurationExtensionsManager;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ExecutionException;
import consulo.util.lang.Pair;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.TestParams;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

// consulo: the nested upstream JvmTestHandler.JvmTestHandlerProvider has its own file (nested @ExtensionImpl classes are not
// supported by the annotation processor), see JvmTestHandlerProvider
public class JvmTestHandler implements BazelRunHandler {
    private final BazelRunConfiguration myConfiguration;

    private final JvmTestState myState;

    public JvmTestHandler(BazelRunConfiguration configuration) {
        myConfiguration = configuration;
        // KotlinCoroutineLibraryFinderBeforeRunTaskProvider must be run before BuildScriptBeforeRunTaskProvider
        List<BeforeRunTask> beforeRunTasks = new ArrayList<>();
        KotlinCoroutineLibraryFinderBeforeRunTaskProvider.Task kotlinCoroutineTask =
            new KotlinCoroutineLibraryFinderBeforeRunTaskProvider().createTask(configuration);
        if (kotlinCoroutineTask != null) {
            beforeRunTasks.add(kotlinCoroutineTask);
        }
        ScriptPathBeforeRunTaskProvider.Task scriptPathTask = new ScriptPathBeforeRunTaskProvider().createTask(configuration);
        if (scriptPathTask != null) {
            beforeRunTasks.add(scriptPathTask);
        }
        configuration.setBeforeRunTasksFromHandler(beforeRunTasks);
        myState = new JvmTestState(configuration.getProject());
    }

    @Override
    public String getName() {
        return BazelPluginLocalize.jvmTestHandlerName().get();
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }

    @Override
    public JvmTestState getState() {
        return myState;
    }

    @Override
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) {
        if (executor instanceof DefaultDebugExecutor) {
            environment.putCopyableUserData(JvmRunHandler.COROUTINE_JVM_FLAGS_KEY, new SimpleReference<>());
        }
        if (RunWithScriptPathExtension.Companion.shouldRunWithScriptPath(executor, myConfiguration)) {
            environment.putCopyableUserData(ScriptPathBeforeRunTaskProvider.SCRIPT_PATH_KEY, new SimpleReference<>());
            return new ScriptPathTestCommandLineState(environment, myState, myConfiguration);
        }
        else {
            return new JvmTestCommandLineState(environment, myState);
        }
    }

    @Override
    public RunConfigurationExtensionsManager<? super RunConfigurationBase, ?> getExtensionsManager() {
        return JavaRunConfigurationExtensionManager.getInstance();
    }

    // consulo: upstream private top-level extension fun BazelRunConfiguration.getJvmTestRunnerExtension()
    static JvmTestRunnerExtension getJvmTestRunnerExtension(BazelRunConfiguration receiver) {
        return JvmTestRunnerExtension.getInstance(receiver.getProject(), receiver.getTargets());
    }
}

class JvmTestCommandLineState extends BazelTestCommandLineState {
    private final Supplier<JvmTestRunnerExtension> myTestRunner;

    JvmTestCommandLineState(
        ExecutionEnvironment environment,
        JvmTestState state
    ) {
        super(/* environment = */ environment, /* state = */ state);
        myTestRunner = LazyValue.notNull(() -> JvmTestHandler.getJvmTestRunnerExtension(BazelRunConfiguration.get(environment)));
    }

    @Override
    protected boolean isIdBasedTestTree() {
        return myTestRunner.get().isIdBasedTestTree();
    }

    @Override
    protected TestParams transformTestParams(TestParams params) {
        return myTestRunner.get().transformTestParams(params);
    }

    @Override
    protected BazelTaskListener createAndAddTaskListener(BazelProcessHandler handler) {
        return myTestRunner.get().createTaskListener(handler, getCoverageReportListener());
    }
}

class ScriptPathTestCommandLineState extends JvmDebuggableCommandLineState {
    private final JvmTestState mySettings;

    private final Supplier<JvmTestRunnerExtension> myTestRunner;

    ScriptPathTestCommandLineState(
        ExecutionEnvironment environment,
        JvmTestState settings,
        BazelRunConfiguration configuration
    ) {
        super(environment, settings.getDebugPort(), configuration);
        mySettings = settings;
        myTestRunner = LazyValue.notNull(() -> JvmTestHandler.getJvmTestRunnerExtension(BazelRunConfiguration.get(environment)));
    }

    public JvmTestState getSettings() {
        return mySettings;
    }

    @Override
    protected boolean isIdBasedTestTree() {
        return myTestRunner.get().isIdBasedTestTree();
    }

    @Override
    protected BazelTaskListener createAndAddTaskListener(BazelProcessHandler handler) {
        return myTestRunner.get().createTaskListener(handler, null);
    }

    @Override
    public ExecutionResult execute(Executor executor, ProgramRunner runner) throws ExecutionException {
        return executeWithTestConsole(executor);
    }

    @Override
    protected CompletableFuture<Integer> runCommand(
        BazelServerFacade server,
        CompletableFuture<@Nullable Long> pidDeferred,
        BazelProcessHandler handler
    ) {
        SimpleReference<Path> scriptPathRef = getEnvironment().getCopyableUserData(ScriptPathBeforeRunTaskProvider.SCRIPT_PATH_KEY);
        Path scriptPath = scriptPathRef == null ? null : scriptPathRef.get();
        if (scriptPath == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Missing --script_path"));
        }
        Pair<Map<String, String>, @Nullable String> envAndTestFilter =
            myTestRunner.get().transformScriptPathEnvAndTestFilter(mySettings.getEnv().getEnvs(), mySettings.getTestFilter());
        Map<String, String> env = envAndTestFilter.getFirst();
        String testFilter = envAndTestFilter.getSecond();
        List<String> additionalScriptParameters;
        try {
            additionalScriptParameters = JvmRunHandler.getAdditionalJvmRunParameters(getEnvironment(), mySettings.getDebugPort());
        }
        catch (ExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
        return RunWithScriptPath.runWithScriptPath(
            getTaskGroupId().task("jvm-test"),
            /* scriptPath = */ scriptPath,
            /* project = */ getEnvironment().getProject(),
            /* pidDeferred = */ pidDeferred,
            /* handler = */ handler,
            /* env = */ env,
            /* additionalScriptParameters = */ additionalScriptParameters,
            /* isTest = */ true,
            /* isPassParentEnvs = */ mySettings.getEnv().isPassParentEnvs(),
            /* testFilter = */ testFilter,
            processHandler -> {
                JvmRunHandler.attachJvmRunExtensions(getEnvironment(), processHandler);
                return CompletableFuture.completedFuture(null);
            }
        );
    }
}
