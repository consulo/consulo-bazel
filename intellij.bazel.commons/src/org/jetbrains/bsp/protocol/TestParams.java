package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record TestParams(
    TaskId taskId,
    List<Label> targets,
    List<String> arguments,
    Map<String, String> environmentVariables,
    boolean useCoverage,
    @Nullable String coverageInstrumentationFilter,
    @Nullable String testFilter,
    List<String> additionalBazelParams,
    boolean streamTestOutput
) {
    public TestParams(TaskId taskId, List<Label> targets) {
        this(taskId, targets, List.of(), Map.of(), false, null, null, List.of(), false);
    }
}
