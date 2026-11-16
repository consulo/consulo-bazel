package org.jetbrains.bazel.run.task;

import consulo.application.Application;
import consulo.build.ui.event.BuildEventFactory;
import consulo.build.ui.event.MessageEvent;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bsp.protocol.TaskFinishParams.CompileReport;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public class BazelBuildTaskListener implements BazelTaskListener {
    private final TaskConsole myTaskConsole;

    public BazelBuildTaskListener(TaskConsole taskConsole) {
        myTaskConsole = taskConsole;
    }

    @Override
    public void onTaskFinish(
        TaskId taskId,
        String message,
        BazelStatus status,
        @Nullable Object data
    ) {
        BuildEventFactory buildEventFactory = Application.get().getInstance(BuildEventFactory.class);
        if (data instanceof CompileReport compileReport) {
            if (compileReport.errors() == 0 && status == BazelStatus.SUCCESS) {
                myTaskConsole.finishSubtask(taskId, LocalizeValue.of(message), buildEventFactory.createSuccessResult());
            }
            else if (status == BazelStatus.CANCEL) {
                myTaskConsole.finishSubtask(taskId, LocalizeValue.of(message), buildEventFactory.createSkippedResult());
            }
            else {
                myTaskConsole.finishSubtask(taskId, LocalizeValue.of(message), buildEventFactory.newFailure().createResult());
            }
        }
        else {
            myTaskConsole.finishSubtask(taskId, LocalizeValue.of(message), buildEventFactory.createSuccessResult());
        }
    }

    @Override
    public void onDiagnostic(
        TaskId taskId,
        @Nullable Path textDocument,
        Label buildTarget,
        int line,
        int character,
        MessageEvent.Kind severity,
        String message,
        @Nullable String description
    ) {
        @Nullable Path path = textDocument;
        if (textDocument != null) {
            @Nullable VirtualFile virtualFile = PathUtil.findCanonicalVirtualFileThatExists(textDocument);
            if (virtualFile != null) {
                path = virtualFile.toNioPath();
            }
        }
        myTaskConsole.addDiagnosticMessage(
            taskId,
            path,
            line,
            character,
            LocalizeValue.of(message),
            description,
            severity
        );
    }

    @Override
    public void onLogMessage(TaskId taskId, String message) {
        myTaskConsole.addMessage(taskId, message);
    }
}
