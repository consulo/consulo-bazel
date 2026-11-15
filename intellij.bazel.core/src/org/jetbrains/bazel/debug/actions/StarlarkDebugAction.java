package org.jetbrains.bazel.debug.actions;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jetbrains.bazel.debug.configuration.StarlarkDebugConfiguration;
import org.jetbrains.bazel.debug.configuration.StarlarkDebugConfigurationType;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;

import java.util.concurrent.CompletableFuture;

public class StarlarkDebugAction extends SuspendableAction {
    private final Label myTargetId;

    public StarlarkDebugAction(Label targetId) {
        super(
            BazelPluginLocalize.starlarkDebugActionName(),
            ExecutionDebugIconGroup.actionStartdebugger()
        );
        myTargetId = targetId;
    }

    @Override
    protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
        RunnerAndConfigurationSettings config = createConfigSettings(project);
        config.setTemporary(true);
        save(config, project);
        execute(config);
        return CompletableFuture.completedFuture(null);
    }

    private RunnerAndConfigurationSettings createConfigSettings(Project project) {
        RunManager runManager = RunManager.getInstance(project);
        String configName = BazelPluginLocalize.starlarkDebugConfigTemplate(BazelRepoMappingUtils.toShortString(myTargetId, project)).get();
        ConfigurationFactory factory = new StarlarkDebugConfigurationType().getConfigurationFactories()[0];
        // consulo: RunManager.createConfiguration(name, factory) is createRunConfiguration(name, factory)
        return withTarget(runManager.createRunConfiguration(configName, factory), myTargetId);
    }

    private static RunnerAndConfigurationSettings withTarget(RunnerAndConfigurationSettings receiver, Label target) {
        if (receiver.getConfiguration() instanceof StarlarkDebugConfiguration configuration) {
            configuration.setTarget(target.toString());
        }
        return receiver;
    }

    private static void save(RunnerAndConfigurationSettings receiver, Project project) {
        // consulo: RunManagerEx.setTemporaryConfiguration is internal API; it adds the (temporary) settings and selects them
        RunManager runManager = RunManager.getInstance(project);
        runManager.addConfiguration(receiver);
        runManager.setSelectedConfiguration(receiver);
    }

    private static void execute(RunnerAndConfigurationSettings receiver) {
        Executor executor = ExecutorRegistry.getInstance().getExecutorById(DefaultDebugExecutor.EXECUTOR_ID);
        if (executor == null) {
            throw new IllegalStateException("Failed to obtain the executor");
        }
        ProgramRunnerUtil.executeConfiguration(receiver, executor);
    }
}
