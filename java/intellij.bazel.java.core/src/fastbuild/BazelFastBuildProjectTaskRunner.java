package org.jetbrains.bazel.fastbuild;

import consulo.annotation.access.RequiredReadAction;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.compiler.CompileContextEx;
import consulo.compiler.CompileDriver;
import consulo.compiler.CompilerRunner;
import consulo.compiler.ExitException;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.config.BazelFeatureFlags;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// consulo: IntelliJ's ProjectTaskRunner does not exist; like core's BazelProjectTaskRunner this is a Consulo CompilerRunner.
// TODO consulo: not registered (upstream: <projectTaskRunner order="before BazelProjectTaskRunner">). Consulo picks the first
//  available CompilerRunner for every build and its CompileScope does not tell a ModuleFilesBuildTask ("compile these files") from a
//  module/project build, so registering it would route every build through the fast build when the flag is on
public class BazelFastBuildProjectTaskRunner implements CompilerRunner {
    // obsolete, kept for compatibility with 261.*
    // override fun canRun(projectTask: ProjectTask): Boolean = false

    // consulo: upstream canRun(project, projectTask, context); `projectTask is ModuleFilesBuildTask` cannot be checked here
    @RequiredReadAction
    @Override
    public Result checkAvailable(DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        return BazelFeatureFlags.getFastBuildEnabled() &&
            project != null && org.jetbrains.bazel.config.project.isBazelProject(project)
            ? new YesResult(BazelPluginIcons.bazel())
            : NO;
    }

    @Override
    public LocalizeValue getName() {
        return BazelPluginLocalize.widgetFastbuildRecompile();
    }

    // consulo: upstream run(project, context, vararg tasks): Promise<Result>; the files of the ModuleFilesBuildTasks are the files
    // of the compile scope
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
        // TODO: IntelliJ will try to hotswap using JPS and fail, displaying "Loaded classes are up to date. Nothing to reload."
        List<VirtualFile> files = new ArrayList<>();
        for (Path path : context.getCompileScope().getFiles(null)) {
            VirtualFile file = LocalFileSystem.getInstance().findFileByNioFile(path);
            if (file != null) {
                files.add(file);
            }
        }
        return FastBuildUtils.fastBuildFilesPromise(context.getProject(), files).join();
    }
}
