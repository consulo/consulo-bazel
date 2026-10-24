package org.jetbrains.bazel.sync;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.component.extension.ExtensionPoint;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bsp.protocol.TaskId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a sync hook which will be executed on each sync (if `isEnabled` returns true).
 * <p>
 * It should be used to perform sync actions, e.g., to add a language support.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProjectSyncHook {
    /**
     * Tells the sync mechanism whatever this hook should be executed during sync.
     * It will always be called before each `onSync` call.
     */
    default boolean isEnabled(Project project) {
        return true;
    }

    /**
     * Method which will be called during sync. It can perform any type of activity that is part of sync.
     */
    CompletableFuture<Void> onSync(ProjectSyncHookEnvironment environment);

    static ExtensionPoint<ProjectSyncHook> getEp() {
        return Application.get().getExtensionPoint(ProjectSyncHook.class);
    }

    /**
     * consulo: {@code diff} is the classic project model builder instead of the workspace model {@code MutableEntityStorage}.
     *
     * @param project          project on which sync is executed
     * @param syncScope        scope the sync is effectively running with
     * @param server           BSP server which is used during sync
     * @param snapshot         workspace snapshot published by this sync
     * @param diff             diff which was prepared before sync and which should be updated in the hook
     * @param taskId           task id which should be used in the sync console as root
     * @param progressReporter should be used to report the progress of the hook
     */
    record ProjectSyncHookEnvironment(
        Project project,
        ProjectSyncScope syncScope,
        BazelServerFacade server,
        WorkspaceSnapshot snapshot,
        TaskId taskId,
        BazelModelBuilder diff,
        ProgressIndicator progressReporter
    ) {
        public <T> CompletableFuture<T> withSubtask(LocalizeValue text, Function<TaskId, CompletableFuture<T>> block) {
            return util.withSubtask(util.getSyncConsole(project), progressReporter, taskId.subTask(text.get()), text, block);
        }
    }

    static List<ProjectSyncHook> getProjectSyncHooks(Project receiver) {
        List<ProjectSyncHook> result = new ArrayList<>();
        for (ProjectSyncHook it : getEp().getExtensionList()) {
            if (it.isEnabled(receiver)) {
                result.add(it);
            }
        }
        return result;
    }
}
