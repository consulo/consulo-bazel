package org.jetbrains.bazel.golang.debug;

import com.goide.runconfig.testing.GoTestRunConfiguration;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.module.Module;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.config.BazelRunConfigurationType;
import org.jetbrains.bazel.run.state.AbstractGenericTestState;
import org.jetbrains.bazel.run.state.GenericTestState;
import org.jetbrains.bazel.utils.ExecutableInfo;
import org.jetbrains.bsp.protocol.TestParams;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

// consulo: the nested upstream BazelGoTestHandler.BazelGoTestHandlerProvider has its own file (nested @ExtensionImpl classes are not
// supported by the annotation processor), see BazelGoTestHandlerProvider
public class BazelGoTestHandler implements BazelRunHandler {
    // consulo: upstream top-level declarations of BazelGoTestHandler.kt

    /**
     * Used to store a runner to an {@link ExecutionEnvironment}.
     */
    static final Key<AtomicReference<ExecutableInfo>> EXECUTABLE_KEY = Key.create("bazel.debug.golang.executable");

    // ensures proper test output recognized by the Go test runner
    // without this env the test results are not shown properly in the IDE
    static final Map<String, String> GO_TEST_WRAP_TESTV_1 = Map.of("GO_TEST_WRAP_TESTV", "1");

    private final GenericTestState myState = new GenericTestState();

    public BazelGoTestHandler(BazelRunConfiguration configuration) {
        List<BeforeRunTask> beforeRunTasks = new ArrayList<>();
        ScriptPathDebugBeforeRunTaskProvider.Task task = new ScriptPathDebugBeforeRunTaskProvider().createTask(configuration);
        if (task != null) {
            beforeRunTasks.add(task);
        }
        configuration.setBeforeRunTasksFromHandler(beforeRunTasks);
    }

    @Override
    public String getName() {
        return "Bazel Go Test Handler";
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }

    @Override
    public GenericTestState getState() {
        return myState;
    }

    @Override
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) throws ExecutionException {
        if (executor instanceof DefaultDebugExecutor) {
            environment.putCopyableUserData(EXECUTABLE_KEY, new AtomicReference<>());
            Label target = getTargetId(environment);
            Project project = environment.getProject();
            Module module = GoWorkspaceModuleUtil.findModule(project);
            if (module == null) {
                throw new IllegalStateException("Could not find module for target " + target);
            }
            GoTestRunConfiguration configuration = new GoTestRunConfiguration(project, "default", new BazelRunConfigurationType());
            configuration.setModule(module);
            return new GoTestWithDebugCommandLineState(
                environment,
                module,
                configuration,
                myState
            );
        }
        else {
            return new BazelGoTestCommandLineState(environment, myState);
        }
    }

    private static Label getTargetId(ExecutionEnvironment environment) throws ExecutionException {
        if (environment.getRunProfile() instanceof BazelRunConfiguration configuration && configuration.getTargets().size() == 1) {
            return configuration.getTargets().get(0);
        }
        throw new ExecutionException(BazelPluginLocalize.goTestHandlerErrorTargetMissingFromConfig());
    }
}

class BazelGoTestCommandLineState extends BazelTestCommandLineState {
    BazelGoTestCommandLineState(
        ExecutionEnvironment environment,
        AbstractGenericTestState<?> state
    ) {
        super(environment, state);
    }

    @Override
    protected TestParams transformTestParams(TestParams params) {
        // consulo: Kotlin `GO_TEST_WRAP_TESTV_1 + params.environmentVariables`, later entries win
        Map<String, String> environmentVariables = new LinkedHashMap<>(BazelGoTestHandler.GO_TEST_WRAP_TESTV_1);
        environmentVariables.putAll(params.environmentVariables());
        return new TestParams(
            params.taskId(),
            params.targets(),
            params.arguments(),
            environmentVariables,
            params.useCoverage(),
            params.coverageInstrumentationFilter(),
            params.testFilter(),
            params.additionalBazelParams(),
            params.streamTestOutput()
        );
    }
}
