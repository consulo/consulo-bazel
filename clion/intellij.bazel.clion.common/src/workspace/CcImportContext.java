package org.jetbrains.bazel.clion.workspace;

import consulo.build.ui.event.MessageEvent;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public interface CcImportContext {

    TaskId getTaskId();

    Project getProject();

    // consulo: upstream vfuManager (VirtualFileUrlManager) is a workspace model object, urls are VFS url strings

    WorkspaceSnapshot getSnapshot();

    Path getExecroot();

    void reportEvent(MessageEvent.Kind severity, LocalizeValue message, @Nullable String description);

    // consulo: overload for the upstream default value of description
    default void reportEvent(MessageEvent.Kind severity, LocalizeValue message) {
        reportEvent(severity, message, null);
    }

    OutputLocationResolver getOutputResolver();

    OutputLocationParser getOutputParser();

    static CcImportContext create(TaskId taskId, WorkspaceImporterContext ctx, WorkspaceSnapshot snapshot) {
        return new CcImportContextImpl(ctx, taskId, snapshot);
    }
}

class CcImportContextImpl implements CcImportContext {
    private final WorkspaceImporterContext myCtx;
    private final TaskId myTaskId;
    private final WorkspaceSnapshot mySnapshot;

    CcImportContextImpl(WorkspaceImporterContext ctx, TaskId taskId, WorkspaceSnapshot snapshot) {
        myCtx = ctx;
        myTaskId = taskId;
        mySnapshot = snapshot;
    }

    @Override
    public TaskId getTaskId() {
        return myTaskId;
    }

    @Override
    public WorkspaceSnapshot getSnapshot() {
        return mySnapshot;
    }

    @Override
    public Project getProject() {
        return myCtx.project();
    }

    @Override
    public Path getExecroot() {
        return myCtx.bazelInfo().execRoot();
    }

    @Override
    public void reportEvent(MessageEvent.Kind severity, LocalizeValue message, @Nullable String description) {
        myCtx.taskConsole().addDiagnosticMessage(myTaskId, null, -1, -1, message, description, severity);
    }

    @Override
    public OutputLocationResolver getOutputResolver() {
        return myCtx.outputResolver();
    }

    @Override
    public OutputLocationParser getOutputParser() {
        return myCtx.outputParser();
    }
}
