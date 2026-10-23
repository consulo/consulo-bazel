package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.label.Label;

import java.util.List;

public record CompileParams(
    TaskId taskId,
    List<Label> targets,
    List<String> arguments
) {
    public CompileParams(TaskId taskId, List<Label> targets) {
        this(taskId, targets, List.of());
    }
}
