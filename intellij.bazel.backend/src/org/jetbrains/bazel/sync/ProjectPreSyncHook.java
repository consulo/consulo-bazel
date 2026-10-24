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
 * Represents a pre-sync hook which will be executed before each sync (if `isEnabled` returns true).
 * <p>
 * It should be used to perform pre-sync actions, e.g., run code generation before sync.
 * It's guaranteed that hook will be called *before* all {@link ProjectSyncHook} calls.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProjectPreSyncHook {
    /**
     * Tells the sync mechanism whatever this hook should be executed before a sync.
     * It will always be called before each `onPreSync` call.
     */
    default boolean isEnabled(Project project) {
        return true;
    }

    /**
     * Method which will be called before a sync.
     */
    CompletableFuture<Void> onPreSync(ProjectPreSyncHookEnvironment environment);

    static ExtensionPoint<ProjectPreSyncHook> getEp() {
        return Application.get().getExtensionPoint(ProjectPreSyncHook.class);
    }

    /**
     * @param project          project on which sync is executed
     * @param taskId           task id which should be used in the sync console as root
     * @param progressReporter should be used to report the progress of the hook
     */
    record ProjectPreSyncHookEnvironment(
        Project project,
        TaskId taskId,
        ProgressIndicator progressReporter
    ) {
    }

    static List<ProjectPreSyncHook> getProjectPreSyncHooks(Project receiver) {
        return getEp()
            .getExtensionList()
            .stream()
            .filter(it -> it.isEnabled(receiver))
            .toList();
    }

    // consulo: the subtask id is built from the localized text, like upstream builds it from the text
    static <T> CompletableFuture<T> withSubtask(
        ProjectPreSyncHookEnvironment receiver,
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
