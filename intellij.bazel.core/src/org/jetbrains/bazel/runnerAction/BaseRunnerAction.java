package org.jetbrains.bazel.runnerAction;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunnerRegistry;
import consulo.localize.LocalizeValue;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public abstract class BaseRunnerAction extends SuspendableAction {
    private final Executor myExecutor;
    private final String myConfigurationName;

    protected BaseRunnerAction(
        Project project,
        Executor executor,
        String configurationName
    ) {
        super(
            /* text = */ getActionText(project, executor, configurationName),
            /* icon = */ executor.getIcon()
        );
        myExecutor = executor;
        myConfigurationName = configurationName;
    }

    protected String getConfigurationName() {
        return myConfigurationName;
    }

    protected abstract CompletableFuture<@Nullable RunnerAndConfigurationSettings> getRunnerSettings();

    @Override
    protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
        return doPerformAction(project);
    }

    public CompletableFuture<?> doPerformAction(Project project) {
        CompletableFuture<@Nullable Object> perform;
        try {
            perform = getRunnerSettings().thenCompose(settings -> {
                if (settings == null) {
                    return CompletableFuture.<@Nullable Object>completedFuture(null);
                }
                // TODO consulo: RunManagerEx is internal in Consulo (consulo.execution.internal is not exported to plugins),
                //  the settings are not registered as the temporary configuration
                // RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(settings)
                ProgramRunner runner = RunnerRegistry.getInstance().getRunner(myExecutor.getId(), settings.getConfiguration());

                if (runner != null) {
                    ExecutionEnvironment executionEnvironment =
                        new ExecutionEnvironmentBuilder(project, myExecutor)
                            .runnerAndSettings(runner, settings)
                            .build();
                    return project.getUIAccess().giveAsync(() -> {
                        try {
                            runner.execute(executionEnvironment);
                        }
                        catch (ExecutionException ex) {
                            throw new CompletionException(ex);
                        }
                    }).thenApply(ignored -> (@Nullable Object) null);
                }
                else {
                    throw new IllegalStateException("Runner not found for executor " + myExecutor.getId());
                }
            });
        }
        catch (Exception e) {
            perform = CompletableFuture.failedFuture(e);
        }
        return perform.exceptionallyCompose(error -> {
            // consulo: the failure of a dependent stage is wrapped into a CompletionException
            Throwable e = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            return project.getUIAccess().giveAsync(() -> {
                Messages.showErrorDialog(project, e.toString(), BazelPluginLocalize.widgetSideMenuErrorTitle().get());
            }).thenApply(ignored -> (@Nullable Object) null);
        });
    }

    private static LocalizeValue getActionText(Project project, Executor executor, String configurationName) {
        LocalizeValue normalText = executor.getStartActiveText(configurationName);
        if (org.jetbrains.bazel.config.project.isBazelProject(project)) {
            // E.g.: Debug `//src:binary`
            return normalText;
        }
        else {
            // Ultimate monorepo opened via Monorepo DevKit: add a clarifying text, e.g.: Debug `src:binary` (Bazel)
            return normalText.map(text -> text + " (Bazel)");
        }
    }
}
