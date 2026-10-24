package org.jetbrains.bazel.sync.projectStructure;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.bazel.backend.localize.BazelBackendLocalize;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.project.Project;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bsp.protocol.TaskId;

import java.util.concurrent.CompletableFuture;

// consulo: the classic project model is applied instead of the workspace model. Upstream replaces the Bazel-sourced
// entities (replaceBySource) with up to MAX_REPLACE_WSM_ATTEMPTS retries; BazelModelBuilder.commit() replaces the
// Bazel-owned modules and libraries in one write action.
public class ProjectModelApplicationTask {
    private final Project myProject;
    private final TaskId myTaskId;

    public ProjectModelApplicationTask(Project project, TaskId taskId) {
        myProject = project;
        myTaskId = taskId;
    }

    public CompletableFuture<Void> apply(BazelModelBuilder storage) {
        return util.withSubtask(
            util.getSyncConsole(myProject),
            myTaskId.subTask("apply-changes-on-workspace-model"),
            BazelBackendLocalize.consoleTaskModelApplyChanges(),
            taskId -> {
                CompletableFuture<Void> result = new CompletableFuture<>();
                Application.get().invokeLater(() -> {
                    try {
                        WriteAction.run(storage::commit);
                        result.complete(null);
                    }
                    catch (Throwable e) {
                        storage.dispose();
                        result.completeExceptionally(e);
                    }
                });
                return result;
            }
        );
    }
}
