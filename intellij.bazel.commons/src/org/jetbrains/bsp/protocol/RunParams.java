package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public record RunParams(
    TaskId taskId,
    Label target,
    boolean checkVisibility,
    List<String> arguments,
    Map<String, String> environmentVariables,
    List<String> additionalBazelParams,
    @Nullable CompletableFuture<@Nullable Long> pidDeferred,
    boolean captureStdout
) {
    public RunParams(TaskId taskId, Label target, boolean checkVisibility) {
        this(taskId, target, checkVisibility, List.of(), Map.of(), List.of(), null, false);
    }
}
