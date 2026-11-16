package org.jetbrains.bazel.clion.run.debug;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import com.intellij.aspect.lib.Definition;
import com.intellij.aspect.lib.OutputGroups;
import com.intellij.aspect.lib.ReadTarget;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.build.ui.event.MessageEvent;
import consulo.process.ExecutionException;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.action.SaveAllFiles;
import org.jetbrains.bazel.bazelrunner.params.BazelFlag;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask;
import org.jetbrains.bazel.utils.ExecutableInfo;
import org.jetbrains.bazel.utils.ExecutableInfo.UnexpectedScriptContentException;
import org.jetbrains.bsp.protocol.RunParams;
import org.jetbrains.bsp.protocol.RunResult;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

// consulo: top-level declarations of upstream buildExecutable.kt
public final class buildExecutable {
    private buildExecutable() {
    }

    // consulo: upstream internal top-level class of buildExecutable.kt
    public record CcDebugBuildResult(
        ExecutableInfo executable,

        /* All targets that Bazel built for the executable, empty if the debug aspect did not run. */
        List<TargetIdeInfo> targets
    ) {
    }

    /**
     * Returns the target that the {@code intellij_cc_debug_aspect} reported for the label.
     */
    static @Nullable TargetIdeInfo findDebugTarget(Collection<TargetIdeInfo> receiver, Label label) {
        // it's very unlikely that the main target exists under two configurations here
        for (TargetIdeInfo it : receiver) {
            if (Label.label(it).equals(label)) {
                return it;
            }
        }
        return null;
    }

    /**
     * Builds the target and writes the run script with one {@code bazel run --script_path}. The same command
     * also runs the {@code intellij_cc_debug_aspect}, if the last sync deployed it.
     */
    // consulo: @Throws(ExecutionException::class) context(ctx: CcDebugContext) suspend fun -> ctx parameter, the future fails with the
    //   ExecutionException
    static CompletableFuture<CcDebugBuildResult> buildExecutable(CcDebugContext ctx) {
        Path scriptPath;
        try {
            // consulo: upstream withContext(Dispatchers.IO) { }
            scriptPath = Files.createTempFile("bazel-cc-debug-", ".sh");
        }
        catch (IOException e) {
            return CompletableFuture.failedFuture(new ExecutionException(BazelCLionCoreLocalize.debugErrorScriptCreateFailed().get(), e));
        }

        boolean aspectDeployed = isCcDebugAspectDeployed(ctx);
        if (!aspectDeployed) {
            ctx.reportEvent(MessageEvent.Kind.WARNING, BazelCLionCoreLocalize.debugWarningAspectNotDeployed());
        }

        CompletableFuture<RunResult> resultFuture =
            BazelServerConnection.getConnection(ctx.getProject()).runWithServer(ctx.getTaskId(), server -> {
                List<String> bazelParams = new ArrayList<>();
                bazelParams.addAll(ScriptPathBuildTargetTask.scriptPathParams(scriptPath));
                if (aspectDeployed) {
                    bazelParams.addAll(ccDebugAspectParams());
                }
                bazelParams.addAll(ProjectViewExtensions.getDebugFlags(server.getProjectView()));
                bazelParams.addAll(Utils.parseAsProgramArguments(ctx.getState().getAdditionalBazelParams()));

                RunParams params = new RunParams(
                    /* taskId = */ ctx.getTaskId(),
                    /* target = */ ctx.getLabel(),
                    /* checkVisibility = */ true,
                    /* arguments = */ Utils.parseAsProgramArguments(ctx.getState().getProgramArguments()),
                    /* environmentVariables = */ ctx.getState().getEnv().getEnvs(),
                    /* additionalBazelParams = */ bazelParams,
                    /* pidDeferred = */ null,
                    /* captureStdout = */ false
                );

                return SaveAllFiles.saveAllFiles().thenCompose(ignored -> server.buildTargetRun(params));
            });

        return resultFuture.thenApply(result -> {
            BazelStatus status = result.statusCode();
            if (status != BazelStatus.SUCCESS) {
                throw new CompletionException(new ExecutionException(BazelCLionCoreLocalize.debugErrorScriptGenerateFailed(status)));
            }

            ExecutableInfo executableInfo;
            try {
                // consulo: upstream withContext(Dispatchers.IO) { }
                executableInfo = ExecutableInfo.fromBazelRunScript(Files.readString(scriptPath));
                executableInfo.createRequiredDirectories();
            }
            catch (IOException e) {
                throw new CompletionException(new ExecutionException(BazelCLionCoreLocalize.debugErrorScriptReadFailed().get(), e));
            }
            catch (UnexpectedScriptContentException e) {
                throw new CompletionException(new ExecutionException(BazelCLionCoreLocalize.debugErrorScriptParseFailed().get(), e));
            }

            return new CcDebugBuildResult(executableInfo, readDebugTargets(ctx, result.infoFiles()));
        }).whenComplete((result, error) -> {
            // consulo: upstream finally { withContext(NonCancellable + Dispatchers.IO) { } }, whenComplete also runs after a cancellation
            // best effort cleanup, also after a cancellation
            try {
                Files.deleteIfExists(scriptPath);
            }
            catch (IOException | RuntimeException ignored) {
            }
        });
    }

    /**
     * Reports all files that it cannot read as one warning.
     */
    // consulo: context(ctx: CcDebugContext) suspend fun -> ctx parameter, runs in the calling (pooled) thread
    private static List<TargetIdeInfo> readDebugTargets(CcDebugContext ctx, Set<Path> infoFiles) {
        List<String> problems = new ArrayList<>();
        List<TargetIdeInfo> targets = new ArrayList<>();
        for (Path path : infoFiles) {
            TargetIdeInfo target = ReadTarget.readTargetFromFile(path, msg -> problems.add(path + ": " + StringUtil.notNullize(msg)));
            if (target != null) {
                targets.add(target);
            }
        }

        if (!problems.isEmpty()) {
            ctx.reportEvent(
                /* severity = */ MessageEvent.Kind.WARNING,
                /* message = */ BazelCLionCoreLocalize.debugWarningInfoReadFailed(problems.size()),
                /* description = */ String.join("\n", problems)
            );
        }

        return targets;
    }

    /**
     * Only a sync deploys the aspects, thus the files of an older plugin version can miss the debug aspect.
     * <p>
     * TODO: this is a very simple heuristic, maybe could re-deploy the aspect here or check some sha-sum instead?
     */
    // consulo: context(ctx: CcDebugContext) suspend fun -> ctx parameter
    private static boolean isCcDebugAspectDeployed(CcDebugContext ctx) {
        Path path = org.jetbrains.bazel.config.project.getRootDir(ctx.getProject()).toNioPath()
            .resolve(Constants.DOT_BAZELBSP_DIR_NAME)
            .resolve(Definition.INTELLIJ_CC_DEBUG_ASPECT.getLocation().file());

        // consulo: upstream withContext(Dispatchers.IO) { }
        return Files.exists(path);
    }

    private static List<String> ccDebugAspectParams() {
        return List.of(
            BazelFlag.aspect(BazelBspAspectsManager.resolveAspectLabel(Definition.INTELLIJ_CC_DEBUG_ASPECT.getLocation().toString())),
            // keep the default outputs, otherwise bazel does not build the executable
            BazelFlag.outputGroups(List.of("+" + OutputGroups.INFO.getGroupName(), "+" + OutputGroups.DEBUG.getGroupName()))
        );
    }
}
