package org.jetbrains.bazel.golang.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunFragment;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.golang.targetKinds.GoBazelRules;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.Common.HasBazelParams;
import org.jetbrains.bazel.server.tasks.BuildTargetTask;
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier;
import org.jetbrains.bazel.utils.ExecutableInfo;
import org.jetbrains.bazel.utils.ExecutableInfo.RunfileManifestOnlyException;
import org.jetbrains.bazel.utils.ExecutableInfo.UnexpectedScriptContentException;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

// consulo: IntelliJ BeforeRunTaskProvider is Consulo's BeforeRunFragment (stepsBeforeRunProvider -> @ExtensionImpl)
@ExtensionImpl
public class ScriptPathDebugBeforeRunTaskProvider implements BeforeRunFragment<ScriptPathDebugBeforeRunTaskProvider.Task> {
    // consulo: upstream private top-level declarations of ScriptPathDebugBeforeRunTaskProvider.kt
    private static final String PROVIDER_NAME = "ScriptPathDebugBeforeRunTaskProvider";

    // consulo: Consulo before run tasks are identified by their state class, the key is kept from upstream
    @SuppressWarnings("unused")
    private static final Key<Task> PROVIDER_ID = Key.create(PROVIDER_NAME);

    private static final Logger LOG = Logger.getInstance(ScriptPathDebugBeforeRunTaskProvider.class);

    // consulo: upstream BeforeRunTask<Task>(PROVIDER_ID); Consulo before run tasks are identified by their class
    public static class Task extends BeforeRunTask {
    }

    @Override
    public @Nullable Task createTask(RunConfiguration runConfiguration) {
        if (!(runConfiguration instanceof BazelRunConfiguration)) {
            return null;
        }
        return new Task();
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(PROVIDER_NAME);
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

    // consulo: upstream returns a Boolean (blocking on runBlocking { }), here the future of it
    public CompletableFuture<Boolean> executeTask(
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        Task task
    ) {
        BazelRunConfiguration runConfiguration = BazelRunConfiguration.get(environment);
        // EXECUTABLE_KEY is not present for non-debugging run config
        AtomicReference<ExecutableInfo> executableKey = environment.getCopyableUserData(BazelGoTestHandler.EXECUTABLE_KEY);
        if (executableKey == null) {
            return CompletableFuture.completedFuture(true);
        }
        Project project = environment.getProject();
        TargetStorage targetUtils = TargetStorage.getTargetStorage(project);
        List<BuildTarget> targetInfos = runConfiguration.getTargets().stream()
            .map(targetUtils::getTargetSummary)
            .filter(Objects::nonNull)
            .toList();
        if (targetInfos.stream().anyMatch(it ->
            !GoBazelRules.includesGo(it.getKind()) ||
                (it.getKind().ruleType() != RuleType.TEST && it.getKind().ruleType() != RuleType.BINARY))) {
            return CompletableFuture.completedFuture(false);
        }
        // consulo: Kotlin List.single()
        if (runConfiguration.getTargets().size() != 1) {
            throw new IllegalArgumentException("List must have exactly one element.");
        }
        Label target = runConfiguration.getTargets().get(0);
        Path scriptPath = ScriptPathBuildTargetTask.createTempScriptFile();
        List<String> extraBazelParams = new ArrayList<>(List.of("--dynamic_mode=off", "--compilation_mode=dbg"));
        extraBazelParams.addAll(extractAdditionalBazelParams(runConfiguration));
        // runBlocking instead of runBlockingCancellable because before run tasks aren't cancellable
        // consulo: the before run task is asynchronous in Consulo, no runBlocking
        CompletableFuture<BazelStatus> status = BuildTargetTask.runBuildTargetTask(
            List.of(target),
            project,
            true,
            new ScriptPathBuildTargetTask(
                scriptPath,
                List.of(),
                extraBazelParams
            )
        );
        return status.thenApply(it -> {
            if (it != BazelStatus.SUCCESS) {
                return false;
            }
            String scriptContent;
            try {
                scriptContent = Files.readString(scriptPath);
            }
            catch (Throwable e) {
                handleScriptReadingErrors(e, target);
                return false;
            }
            ExecutableInfo executableInfo;
            try {
                executableInfo = ExecutableInfo.fromBazelRunScript(scriptContent);
            }
            catch (Throwable e) {
                handleScriptParsingErrors(e, target);
                return false;
            }
            executableKey.set(executableInfo);
            return true;
        });
    }

    private static List<String> extractAdditionalBazelParams(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return Utils.parseAsProgramArguments(
            handler != null && handler.getState() instanceof HasBazelParams hasBazelParams
                ? hasBazelParams.getAdditionalBazelParams()
                : null
        );
    }

    private static void handleScriptParsingErrors(Throwable error, Label target) {
        LocalizeValue content;
        if (error instanceof UnexpectedScriptContentException) {
            content = BazelPluginLocalize.goDebugPrepareFailedReasonUnexpectedScriptContent();
        }
        else if (error instanceof RunfileManifestOnlyException) {
            content = BazelPluginLocalize.goDebugPrepareFailedReasonRunfileManifestOnly();
        }
        else {
            content = BazelPluginLocalize.goDebugPrepareFailedReasonUnexpectedError();
        }
        LOG.error("Unable to prepare Go binary for debugging. Failed while parsing the script.", error);
        showScriptError(target, content);
    }

    private static void handleScriptReadingErrors(Throwable error, Label target) {
        LOG.error("Unable to prepare Go binary for debugging. Failed to read the script.", error);
        showScriptError(target, BazelPluginLocalize.goDebugPrepareFailedReasonReadScript());
    }

    private static void showScriptError(Label target, LocalizeValue content) {
        LocalizeValue title = BazelPluginLocalize.goDebugPrepareFailedTitle(target.toString());
        BazelBalloonNotifier.error(title, content);
    }
}
