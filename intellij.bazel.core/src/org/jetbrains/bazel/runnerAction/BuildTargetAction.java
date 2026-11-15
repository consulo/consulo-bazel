package org.jetbrains.bazel.runnerAction;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.tasks.BuildTargetTask;
import org.jetbrains.bazel.target.ModuleTargetService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BuildTargetAction extends SuspendableAction {
    private final Label myTargetId;

    public BuildTargetAction(Label targetId) {
        super(
            /* text = */ BazelPluginLocalize.widgetBuildTargetPopupMessage(),
            /* icon = */ PlatformIconGroup.toolwindowsToolwindowbuild()
        );
        myTargetId = targetId;
    }

    @Override
    protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
        buildTarget(project, myTargetId);
        return CompletableFuture.completedFuture(null);
    }

    public static void buildTarget(Project project, Label targetId) {
        BazelCoroutineService.getInstance(project).start(() -> {
            List<Module> modules = project.getInstance(ModuleTargetService.class).findLegacyModulesByLabel(/* label = */ targetId);
            if (!modules.isEmpty()) {
                // Run through ProjectTaskManager to make sure hotswap works
                // TODO consulo: ProjectTaskManager does not exist in Consulo (the build goes through CompilerRunner)
                // ProjectTaskManager.getInstance(project).build(*modules.toTypedArray())
                return CompletableFuture.completedFuture(null);
            }
            else {
                return BuildTargetTask.runBuildTargetTask(List.of(targetId), project);
            }
        });
    }
}
