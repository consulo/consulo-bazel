package org.jetbrains.bazel.taskEvents;

import consulo.build.ui.event.MessageEvent;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public interface BazelTaskListener {
    default void onDiagnostic(
        TaskId taskId,
        @Nullable Path textDocument,
        Label buildTarget,
        int line,
        int character,
        MessageEvent.Kind severity,
        String message
    ) {
    }

    default void onOutputStream(@Nullable TaskId taskId, String text) {
    }

    default void onErrorStream(@Nullable TaskId taskId, String text) {
    }

    default void onTaskStart(
        TaskId taskId,
        String message,
        @Nullable Object data
    ) {
    }

    default void onTaskFinish(
        TaskId taskId,
        String message,
        BazelStatus status,
        @Nullable Object data
    ) {
    }

    default void onLogMessage(TaskId taskId, String message) {
    }

    default void onPublishCoverageReport(Path coverageReport) {
    }

    default void onCachedTestLog(Path testLog) {
    }
}
