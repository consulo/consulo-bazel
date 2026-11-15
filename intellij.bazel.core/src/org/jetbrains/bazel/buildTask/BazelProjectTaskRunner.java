package org.jetbrains.bazel.buildTask;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.compiler.CompileContextEx;
import consulo.compiler.CompileDriver;
import consulo.compiler.CompilerManager;
import consulo.compiler.CompilerRunner;
import consulo.compiler.ExitException;
import consulo.compiler.scope.CompileScope;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.tasks.BuildTargetTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// consulo: IntelliJ's ProjectTaskRunner (ModuleBuildTask) does not exist; "Build" goes through the Consulo compiler, so the
// runner is a CompilerRunner that builds the targets of the affected modules. Trusted projects are not checked (no Consulo API).
// AdditionalProjectTaskRunnerProvider, AdditionalProjectTask, TargetsToBuildProvider and DefaultTargetsToBuildProvider,
// declared in this upstream file, have their own files.
@ExtensionImpl
public class BazelProjectTaskRunner implements CompilerRunner {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(org.jetbrains.bazel.config.BazelPluginConstants.BAZEL_DISPLAY_NAME);
    }

    @RequiredReadAction
    @Override
    public Result checkAvailable(DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        return project != null && org.jetbrains.bazel.config.project.isBazelProject(project)
            ? new YesResult(BazelPluginIcons.bazel())
            : NO;
    }

    @Override
    public boolean build(
        CompileDriver compileDriver,
        CompileContextEx context,
        BuildProgress<BuildProgressDescriptor> buildProgress,
        boolean isRebuild,
        boolean forceCompile,
        boolean onlyCheckStatus
    ) throws ExitException {
        if (onlyCheckStatus) {
            return true;
        }
        Project project = context.getProject();
        CompileScope compileScope = context.getCompileScope();
        BazelStatus status = run(project, context, compileScope).join();
        return status == BazelStatus.SUCCESS;
    }

    // consulo: upstream run(project, projectTaskContext, vararg tasks): Promise<Result>
    private CompletableFuture<BazelStatus> run(
        Project project,
        CompileContextEx projectTaskContext,
        CompileScope compileScope
    ) {
        return BazelCoroutineService.getInstance(project).startAsync(() -> {
            List<Label> targetsToBuild = ReadAction.compute(() -> obtainTargetsToBuild(project, compileScope.getAffectedModules()));
            List<AdditionalProjectTask> additionalTasks = new ArrayList<>();
            for (AdditionalProjectTaskRunnerProvider provider : AdditionalProjectTaskRunnerProvider.getEp()) {
                AdditionalProjectTask task = provider.createTask(project, projectTaskContext, targetsToBuild);
                if (task != null) {
                    additionalTasks.add(task);
                }
            }
            CompletableFuture<?> preRun = CompletableFuture.completedFuture(null);
            for (AdditionalProjectTask task : additionalTasks) {
                preRun = preRun.thenCompose(unused -> task.preRun());
            }
            return preRun.thenCompose(unused -> BuildTargetTask.runBuildTargetTask(
                targetsToBuild,
                project,
                false,
                () -> {
                    // Rerun through ProjectTaskManager to make sure hotswap works
                    // consulo: the rerun goes through the Consulo compiler with the same scope
                    return project.getUIAccess().giveAsync(() -> {
                        CompilerManager.getInstance(project).make(compileScope, null);
                    });
                }
            )).thenCompose(result -> {
                CompletableFuture<?> postRun = CompletableFuture.completedFuture(null);
                for (AdditionalProjectTask task : additionalTasks) {
                    postRun = postRun.thenCompose(unused -> task.postRun(result));
                }
                return postRun.thenApply(unused -> result);
            });
        }).await();
    }

    private List<Label> obtainTargetsToBuild(Project project, Module[] modules) {
        List<TargetsToBuildProvider> targetsToBuildProviders = TargetsToBuildProvider.getEp();
        List<Label> result = new ArrayList<>();
        for (Module module : modules) {
            for (TargetsToBuildProvider provider : targetsToBuildProviders) {
                result.addAll(provider.getTargetsToBuild(project, module));
            }
        }
        return result;
    }

    // consulo: upstream BazelStatus.toTaskRunnerResult(); the Consulo compiler takes a boolean, see build()
    @SuppressWarnings("unused")
    private static boolean toTaskRunnerResult(BazelStatus receiver) {
        return Objects.requireNonNull(receiver) == BazelStatus.SUCCESS;
    }
}
