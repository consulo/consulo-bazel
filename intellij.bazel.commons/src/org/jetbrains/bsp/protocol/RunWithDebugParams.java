package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;

import java.util.List;

public final class RunWithDebugParams {
    private RunWithDebugParams() {
    }

    public record AnalysisDebugParams(
        TaskId taskId,
        int port,
        List<Label> targets
    ) {
    }

    public record AnalysisDebugResult(BazelStatus statusCode) {
    }
}
