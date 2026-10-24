package org.jetbrains.bazel.sync;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.component.extension.ExtensionPoint;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bsp.protocol.TaskId;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a post-sync hook which will be executed after each sync (if `isEnabled` returns true).
 * <p>
 * It should be used to perform post-sync actions, e.g., code cleanup after sync.
 * It's guaranteed that hook will be called *after* all {@link ProjectSyncHook} calls.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProjectPostSyncHook {
    /**
     * Tells the sync mechanism whatever this hook should be executed after a sync.
     * It will always be called before each `onPostSync` call.
     */
    default boolean isEnabled(Project project) {
        return true;
    }

    /**
     * Method which will be called after a sync.
     */
    CompletableFuture<Void> onPostSync(ProjectPostSyncHookEnvironment environment);

    static ExtensionPoint<ProjectPostSyncHook> getEp() {
        return Application.get().getExtensionPoint(ProjectPostSyncHook.class);
    }

    /**
     * @param project project on which sync is executed
     * @param taskId task id which should be used in the sync console as root
     * @param progressReporter should be used to report the progress of the hook
     * @param projectModelUpdated whether the workspace model, TargetUtils, etc., were updated during sync or not
     *                            (because of a sync failure)
     */
    record ProjectPostSyncHookEnvironment(
        Project project,
        TaskId taskId,
        ProgressIndicator progressReporter,
        boolean projectModelUpdated
    ) {
    }

    static List<ProjectPostSyncHook> getProjectPostSyncHooks(Project receiver) {
        return getEp().getExtensionList().stream().filter(it -> it.isEnabled(receiver)).toList();
    }

    // consulo: the subtask id is built from the localized text, like upstream builds it from the text
    static <T> CompletableFuture<T> withSubtask(
        ProjectPostSyncHookEnvironment receiver,
        LocalizeValue text,
        Function<TaskId, CompletableFuture<T>> block
    ) {
        return util.withSubtask(
            util.getSyncConsole(receiver.project()),
            receiver.progressReporter(),
            receiver.taskId().subTask(text.get()),
            text,
            block
        );
    }
}
