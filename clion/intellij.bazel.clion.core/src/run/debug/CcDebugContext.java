package org.jetbrains.bazel.clion.run.debug;

import consulo.build.ui.event.MessageEvent;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.run.state.GenericRunConfigurationState;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

public interface CcDebugContext {

    TaskId getTaskId();

    Project getProject();

    /**
     * The label of the target to debug.
     */
    Label getLabel();

    GenericRunConfigurationState getState();

    void reportEvent(MessageEvent.Kind severity, LocalizeValue message, @Nullable String description);

    // consulo: upstream default value `description: String? = null`
    default void reportEvent(MessageEvent.Kind severity, LocalizeValue message) {
        reportEvent(severity, message, null);
    }

    static CcDebugContext create(
        TaskId taskId,
        TaskConsole console,
        Project project,
        Label label,
        GenericRunConfigurationState state
    ) {
        return new CcDebugContextImpl(taskId, console, project, label, state);
    }
}

class CcDebugContextImpl implements CcDebugContext {
    private final TaskId myTaskId;
    private final TaskConsole myConsole;
    private final Project myProject;
    private final Label myLabel;
    private final GenericRunConfigurationState myState;

    CcDebugContextImpl(
        TaskId taskId,
        TaskConsole console,
        Project project,
        Label label,
        GenericRunConfigurationState state
    ) {
        myTaskId = taskId;
        myConsole = console;
        myProject = project;
        myLabel = label;
        myState = state;
    }

    @Override
    public TaskId getTaskId() {
        return myTaskId;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public Label getLabel() {
        return myLabel;
    }

    @Override
    public GenericRunConfigurationState getState() {
        return myState;
    }

    @Override
    public void reportEvent(MessageEvent.Kind severity, LocalizeValue message, @Nullable String description) {
        myConsole.addDiagnosticMessage(
            /* taskId = */ myTaskId,
            /* path = */ null,
            /* line = */ -1,
            /* column = */ -1,
            /* message = */ message,
            /* description = */ description,
            /* severity = */ severity
        );
    }
}
