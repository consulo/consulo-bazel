package org.jetbrains.bazel.progress;

import consulo.application.Application;
import consulo.build.ui.event.BuildEventFactory;
import consulo.build.ui.event.EventResult;
import consulo.build.ui.event.MessageEvent;
import consulo.localize.LocalizeValue;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface TaskConsole {
    /**
     * Displays start of a task in this console.
     * Will not display anything if a task with given `taskId` is already running
     *
     * @param taskId       ID of the newly started task
     * @param title        task title which will be displayed in the console
     * @param message      message informing about the start of the task
     * @param cancelAction action which will be executed on cancel button click
     * @param redoAction   action which will be executed on redo button click
     */
    void startTask(
        TaskId taskId,
        LocalizeValue title,
        LocalizeValue message,
        Runnable cancelAction,
        @Nullable Supplier<? extends CompletableFuture<?>> redoAction,
        ShowConsole showConsole
    );

    // consulo: overloads for the upstream default arguments
    default void startTask(TaskId taskId, LocalizeValue title, LocalizeValue message) {
        startTask(taskId, title, message, () -> {
        });
    }

    default void startTask(TaskId taskId, LocalizeValue title, LocalizeValue message, Runnable cancelAction) {
        startTask(taskId, title, message, cancelAction, null);
    }

    default void startTask(
        TaskId taskId,
        LocalizeValue title,
        LocalizeValue message,
        Runnable cancelAction,
        @Nullable Supplier<? extends CompletableFuture<?>> redoAction
    ) {
        startTask(taskId, title, message, cancelAction, redoAction, ShowConsole.ALWAYS);
    }

    boolean hasTasksInProgress();

    /**
     * Displays finish of a task (and all its children) in this console.
     * Will not display anything if a task with given `taskId` is not running
     *
     * @param taskId  ID of the finished task (last started task by default, if nothing passed)
     * @param message message informing about the start of the task
     * @param result  result of the task execution (success by default, if nothing passed)
     */
    void finishTask(
        TaskId taskId,
        LocalizeValue message,
        EventResult result
    );

    // consulo: overload for the upstream default argument (SuccessResultImpl())
    default void finishTask(TaskId taskId, LocalizeValue message) {
        finishTask(taskId, message, Application.get().getInstance(BuildEventFactory.class).createSuccessResult());
    }

    /**
     * Displays start of a subtask in this console
     *
     * @param parentTaskId id of the task (or another subtask) being this subtask's direct parent
     * @param subtaskId    id of the newly created subtask. <b>Has to be unique among all running subtasks in
     *                     this console - otherwise, unexpected behavior might occur</b>
     * @param message      will be displayed as this subtask's title until it's finished
     */
    void startSubtask(
        TaskId subtaskId,
        LocalizeValue message
    );

    /**
     * Displays finishing of a subtask in this console
     *
     * @param subtaskId id of the subtask to be finished.
     *                  If there is no such subtask running, this method will not do anything
     * @param result    result type of the subtask, default {@code SuccessResultImpl}
     */
    void finishSubtask(
        TaskId subtaskId,
        @Nullable LocalizeValue message,
        EventResult result
    );

    // consulo: overloads for the upstream default arguments
    default void finishSubtask(TaskId subtaskId) {
        finishSubtask(subtaskId, null);
    }

    default void finishSubtask(TaskId subtaskId, @Nullable LocalizeValue message) {
        finishSubtask(subtaskId, message, Application.get().getInstance(BuildEventFactory.class).createSuccessResult());
    }

    /**
     * Adds a diagnostic message to a particular task in this console.
     *
     * @param taskId      id of the task (or a subtask), to which the message will be added
     * @param path        absolute path to the file concerned by the diagnostic
     * @param line        line number in given file (first line is 0)
     * @param column      column number in given file (first column is 0)
     * @param message     message of the diagnostic
     * @param description description of the diagnostic
     * @param severity    severity of the diagnostic
     * @param error       the exception that the diagnostic reports. The console does not add the diagnostic when its task already
     *                    shows this exception.
     */
    // consulo: upstream defaults are path = null, line = -1, column = -1, description = null, error = null
    void addDiagnosticMessage(
        TaskId taskId,
        @Nullable Path path,
        int line,
        int column,
        LocalizeValue message,
        @Nullable String description,
        MessageEvent.Kind severity,
        @Nullable Throwable error
    );

    // consulo: overload for the upstream default argument (error = null)
    default void addDiagnosticMessage(
        TaskId taskId,
        @Nullable Path path,
        int line,
        int column,
        LocalizeValue message,
        @Nullable String description,
        MessageEvent.Kind severity
    ) {
        addDiagnosticMessage(taskId, path, line, column, message, description, severity, null);
    }

    /**
     * Adds a message to a particular task in this console. If the message is added to a subtask, it will also be
     * added to the subtask's parent task.
     *
     * @param taskId  id of the task (or a subtask), to which the message will be added
     * @param message message to be added. New line will be inserted at its end if it's not present there already
     */
    void addMessage(TaskId taskId, String message);
}
