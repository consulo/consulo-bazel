package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunFragment;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.run.BazelRunConfigurationState;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.Common;
import org.jetbrains.bazel.server.tasks.BuildTargetTask;
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * As per <a href="https://bazel.build/reference/command-line-reference#run-flag--script_path">--script_path</a> doc:
 * Writes a shell script to the given file which invokes the target.
 * Then the following can be used to invoke target {@code //foo}: {@code bazel run --script_path=foo //foo && ./foo}.
 * This differs from {@code bazel run //foo} in that the bazel lock is released allowing hotswap in tests.
 * This gets us:
 * - Diagnostics for a failed build
 * - Hotswap while debugging a test (because the lock is released)
 * - Because debugger is attached only once the ready script is run, it doesn't have to wait for the build (and possibly timeout)
 */
// consulo: IntelliJ BeforeRunTaskProvider is Consulo's BeforeRunFragment (stepsBeforeRunProvider -> project @ExtensionImpl)
@ExtensionImpl
public class ScriptPathBeforeRunTaskProvider implements BeforeRunFragment<ScriptPathBeforeRunTaskProvider.Task> {
    // consulo: upstream top-level declarations of ScriptPathBeforeRunTaskProvider.kt
    public static final Key<SimpleReference<Path>> SCRIPT_PATH_KEY = Key.create("bazel.jvm.script.path");

    private static final String PROVIDER_NAME = "BuildScriptBeforeRunTaskProvider";

    // consulo: Consulo before run tasks are identified by their state class (BeforeRunFragment.find(project, Task.class)), the key is
    // kept for upstream callers
    @SuppressWarnings("unused")
    private static final Key<Task> PROVIDER_ID = Key.create(PROVIDER_NAME);

    public ScriptPathBeforeRunTaskProvider() {
    }

    public Task createTaskInstance() {
        return new Task();
    }

    @Override
    public @Nullable Task createTask(RunConfiguration runConfiguration) {
        if (!(runConfiguration instanceof BazelRunConfiguration)) {
            return null;
        }
        return createTaskInstance();
    }

    // consulo: BeforeRunFragment.executeAsync() replaces executeTask(); a task returning false cancels the run like upstream
    @Override
    public CompletableFuture<?> executeAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        Task task
    ) {
        return executeTask(context, configuration, environment, task).thenCompose(success -> {
            if (success) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.failedFuture(new CancellationException());
        });
    }

    // consulo: upstream returns a Boolean from runBlocking { }, here the future of it
    public CompletableFuture<Boolean> executeTask(
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        Task task
    ) {
        // Skip for running with coverage, in which case SCRIPT_PATH_KEY is not set
        SimpleReference<Path> scriptPathRef = environment.getCopyableUserData(SCRIPT_PATH_KEY);
        BazelRunConfiguration runConfiguration = BazelRunConfiguration.get(environment);
        boolean isDebug = environment.getExecutor() instanceof DefaultDebugExecutor;
        Project project = environment.getProject();
        Path scriptPath = scriptPathRef != null ? ScriptPathBuildTargetTask.createTempScriptFile() : null;
        CompletableFuture<BazelStatus> status;
        try {
            BuildTargetTask buildTargetTask;
            if (scriptPath != null) {
                BazelRunHandler handler = runConfiguration.getHandler();
                BazelRunConfigurationState<?> state = handler == null ? null : handler.getState();
                List<String> programArguments = Utils.parseAsProgramArguments(
                    state instanceof Common.HasProgramArguments hasProgramArguments ? hasProgramArguments.getProgramArguments() : null
                );
                List<String> additionalBazelParams = Utils.parseAsProgramArguments(
                    state instanceof Common.HasBazelParams hasBazelParams ? hasBazelParams.getAdditionalBazelParams() : null
                );
                buildTargetTask = new ScriptPathBuildTargetTask(
                    /* scriptPath = */ scriptPath,
                    /* programArguments = */ programArguments,
                    /* additionalBazelParams = */ additionalBazelParams
                );
            }
            else {
                /*
                 * scriptPath is null when running tests without debug, which indicates that we should build but not pass --script_path
                 * because --script_path screws up test result caching, but we still want diagnostics if the build fails
                 */
                buildTargetTask = BuildTargetTask.DefaultBuildTargetTask.INSTANCE;
            }
            // runBlocking instead of runBlockingCancellable because before run tasks aren't cancellable
            // consulo: the before run task is asynchronous in Consulo, no runBlocking
            status = BuildTargetTask.runBuildTargetTask(
                runConfiguration.getTargets(),
                project,
                isDebug,
                buildTargetTask
            );
        }
        catch (CancellationException e) {
            return CompletableFuture.completedFuture(false);
        }
        return status.handle((result, error) -> {
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                if (cause instanceof CancellationException) {
                    return false;
                }
                throw new CompletionException(cause);
            }
            if (result == BazelStatus.SUCCESS) {
                if (scriptPathRef != null) {
                    scriptPathRef.set(scriptPath);
                }
                return true;
            }
            else {
                return false;
            }
        });
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public LocalizeValue getName() {
        return BazelPluginLocalize.consoleTaskBuildTitle();
    }

    // consulo: BeforeRunFragment requires an icon, upstream BeforeRunTaskProvider has none by default
    @Override
    public @Nullable Image getIcon(RunConfiguration configuration) {
        return null;
    }

    @Override
    public Class<Task> getStateClass() {
        return Task.class;
    }

    // consulo: upstream BeforeRunTask<Task>(PROVIDER_ID); Consulo before run tasks are identified by their class
    public static class Task extends BeforeRunTask {
    }
}
