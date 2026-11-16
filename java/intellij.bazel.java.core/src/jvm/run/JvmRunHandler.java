package org.jetbrains.bazel.jvm.run;

import com.intellij.java.debugger.impl.engine.AsyncStacksUtils;
import com.intellij.java.execution.impl.JavaRunConfigurationExtensionManager;
import com.intellij.java.execution.impl.RunConfigurationExtension;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunConfigurationExtensionsManager;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.task.BazelRunTaskListener;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// consulo: the nested upstream JvmRunHandler.JvmRunHandlerProvider has its own file (nested @ExtensionImpl classes are not supported
// by the annotation processor), see JvmRunHandlerProvider
public class JvmRunHandler implements BazelRunHandler {
    // consulo: upstream top-level declaration of JvmRunHandler.kt
    public static final Key<SimpleReference<List<String>>> COROUTINE_JVM_FLAGS_KEY = Key.create("bazel.coroutine.jvm.flags");

    private final BazelRunConfiguration myConfiguration;

    private final JvmRunState myState;

    public JvmRunHandler(BazelRunConfiguration configuration) {
        myConfiguration = configuration;
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
        myState = new JvmRunState(configuration.getProject());
    }

    @Override
    public String getName() {
        return BazelPluginLocalize.jvmRunHandlerName().get();
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }

    @Override
    public JvmRunState getState() {
        return myState;
    }

    @Override
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) {
        if (executor instanceof DefaultDebugExecutor) {
            environment.putCopyableUserData(COROUTINE_JVM_FLAGS_KEY, new SimpleReference<>());
        }
        if (RunWithScriptPathExtension.Companion.shouldRunWithScriptPath(executor, myConfiguration)) {
            environment.putCopyableUserData(ScriptPathBeforeRunTaskProvider.SCRIPT_PATH_KEY, new SimpleReference<>());
            return new RunScriptPathCommandLineState(environment, myState, myConfiguration);
        }
        else {
            return new BazelRunCommandLineState(environment, myState);
        }
    }

    @Override
    public RunConfigurationExtensionsManager<? super RunConfigurationBase, ?> getExtensionsManager() {
        return JavaRunConfigurationExtensionManager.getInstance();
    }

    // consulo: upstream top-level functions of JvmRunHandler.kt

    // consulo: throws ExecutionException of RunConfigurationExtension.updateJavaParameters(), see getJvmRunExtensionVmOptions
    public static List<String> getAdditionalJvmRunParameters(ExecutionEnvironment environment, int debugPort) throws ExecutionException {
        List<String> result = new ArrayList<>();
        if (!(environment.getRunProfile() instanceof BazelRunConfiguration)) {
            return result;
        }

        if (environment.getExecutor() instanceof DefaultDebugExecutor) {
            // https://bazel.build/reference/command-line-reference#flag--java_debug
            // https://github.com/bazelbuild/rules_java/blob/747bddd6091a624c54a42c1ac20308190c1ad849/java/bazel/rules/java_stub_template.txt#L23
            result.add("--wrapper_script_flag=--debug=" + debugPort);
        }
        for (String it : getJvmDebuggerAgentVmOptions(environment)) {
            result.add(wrapVmOptionAsArg(it));
        }
        for (String it : getJvmRunExtensionVmOptions(environment)) {
            result.add(wrapVmOptionAsArg(it));
        }
        return result;
    }

    /**
     * The VM options of the coroutine agent and the debugger agent when {@code environment} debugs, else an empty list.
     * The JDWP agent option is not in the list.
     */
    public static List<String> getJvmDebuggerAgentVmOptions(ExecutionEnvironment environment) {
        if (!(environment.getExecutor() instanceof DefaultDebugExecutor)) {
            return List.of();
        }
        OwnJavaParameters debugParameters = new OwnJavaParameters();
        debugParameters.getVMParametersList().addAll(
            KotlinCoroutineLibraryFinderBeforeRunTaskProvider.retrieveKotlinCoroutineParams(environment, environment.getProject())
        );
        // async stacks and the log-capture console decoration require the debugger agent inside the debuggee
        AsyncStacksUtils.addDebuggerAgent(debugParameters, environment.getProject(), false);
        return debugParameters.getVMParametersList().getParameters();
    }

    /**
     * The VM options that the run configuration extensions add, for example for the profiler or the coverage.
     */
    public static List<String> getJvmRunExtensionVmOptions(ExecutionEnvironment environment) throws ExecutionException {
        if (!(environment.getRunProfile() instanceof BazelRunConfiguration configuration)) {
            return List.of();
        }
        OwnJavaParameters profilerParameters = new OwnJavaParameters();
        // consulo: JavaRunConfigurationExtensionManager has no updateJavaParameters(configuration, params, runnerSettings, executor);
        // like Consulo's BaseJavaApplicationCommandLineState every extension is called, the extensions check isApplicableFor()
        for (RunConfigurationExtension ext : RunConfigurationExtension.EP_NAME.getExtensionList()) {
            ext.updateJavaParameters(configuration, profilerParameters, environment.getRunnerSettings());
        }
        return profilerParameters.getVMParametersList().getParameters();
    }

    private static String wrapVmOptionAsArg(String vmOption) {
        // https://github.com/bazelbuild/rules_java/blob/747bddd6091a624c54a42c1ac20308190c1ad849/java/bazel/rules/java_stub_template.txt#L33
        return "--wrapper_script_flag=--jvm_flag=" + vmOption;
    }

    // consulo: upstream OSProcessHandler is internal in Consulo, any ProcessHandler is accepted
    public static void attachJvmRunExtensions(ExecutionEnvironment environment, ProcessHandler processHandler) {
        if (!(environment.getRunProfile() instanceof BazelRunConfiguration configuration)) {
            return;
        }
        JavaRunConfigurationExtensionManager.getInstance()
            .attachExtensionsToProcess(configuration, processHandler, environment.getRunnerSettings());
    }
}

class RunScriptPathCommandLineState extends JvmDebuggableCommandLineState {
    private final JvmRunState mySettings;

    RunScriptPathCommandLineState(
        ExecutionEnvironment environment,
        JvmRunState settings,
        BazelRunConfiguration configuration
    ) {
        super(environment, settings.getDebugPort(), configuration);
        mySettings = settings;
    }

    @Override
    protected BazelTaskListener createAndAddTaskListener(BazelProcessHandler handler) {
        return new BazelRunTaskListener(handler);
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
        List<String> additionalScriptParameters;
        try {
            additionalScriptParameters = JvmRunHandler.getAdditionalJvmRunParameters(getEnvironment(), mySettings.getDebugPort());
        }
        catch (ExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
        return RunWithScriptPath.runWithScriptPath(
            getTaskGroupId().task("jvm-run"),
            scriptPath,
            getEnvironment().getProject(),
            pidDeferred,
            handler,
            mySettings.getEnv().getEnvs(),
            /* additionalScriptParameters = */ additionalScriptParameters,
            /* isTest = */ false,
            /* isPassParentEnvs = */ mySettings.getEnv().isPassParentEnvs(),
            /* testFilter = */ null,
            processHandler -> {
                JvmRunHandler.attachJvmRunExtensions(getEnvironment(), processHandler);
                return CompletableFuture.completedFuture(null);
            }
        );
    }
}
