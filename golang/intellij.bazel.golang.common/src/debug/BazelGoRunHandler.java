package org.jetbrains.bazel.golang.debug;

import com.goide.runconfig.application.GoApplicationConfiguration;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.module.Module;
import consulo.process.ExecutionException;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.config.BazelRunConfigurationType;
import org.jetbrains.bazel.run.state.GenericRunState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// consulo: the nested upstream BazelGoRunHandler.BazelGoRunHandlerProvider has its own file (nested @ExtensionImpl classes are not
// supported by the annotation processor), see BazelGoRunHandlerProvider
public class BazelGoRunHandler implements BazelRunHandler {
    private final GenericRunState myState = new GenericRunState();

    public BazelGoRunHandler(BazelRunConfiguration configuration) {
        List<BeforeRunTask> beforeRunTasks = new ArrayList<>();
        ScriptPathDebugBeforeRunTaskProvider.Task task = new ScriptPathDebugBeforeRunTaskProvider().createTask(configuration);
        if (task != null) {
            beforeRunTasks.add(task);
        }
        configuration.setBeforeRunTasksFromHandler(beforeRunTasks);
    }

    @Override
    public String getName() {
        return "Bazel Go Run Handler";
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
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) throws ExecutionException {
        if (executor instanceof DefaultDebugExecutor) {
            environment.putCopyableUserData(BazelGoTestHandler.EXECUTABLE_KEY, new AtomicReference<>());
            Label target = getTargetId(environment);
            Module module = GoWorkspaceModuleUtil.findModule(environment.getProject());
            if (module == null) {
                throw new IllegalStateException("Could not find module for target " + target);
            }
            GoApplicationConfiguration configuration =
                new GoApplicationConfiguration(environment.getProject(), "default", new BazelRunConfigurationType());
            configuration.setModule(module);
            return new GoRunWithDebugCommandLineState(
                environment,
                module,
                configuration,
                myState
            );
        }
        else {
            return new BazelRunCommandLineState(environment, myState);
        }
    }

    private static Label getTargetId(ExecutionEnvironment environment) throws ExecutionException {
        if (environment.getRunProfile() instanceof BazelRunConfiguration configuration && configuration.getTargets().size() == 1) {
            return configuration.getTargets().get(0);
        }
        throw new ExecutionException(BazelPluginLocalize.goRunnerWrongConfiguration());
    }
}
