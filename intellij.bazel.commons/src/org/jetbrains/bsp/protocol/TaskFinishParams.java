package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

public record TaskFinishParams(
    TaskId taskId,
    @Nullable Long eventTime,
    @Nullable String message,
    BazelStatus status,
    @Nullable TaskFinishData data
) {
    public TaskFinishParams(TaskId taskId, BazelStatus status) {
        this(taskId, null, null, status, null);
    }

    public sealed interface TaskFinishData {
    }

    public record CompileReport(
        Label target,
        int errors,
        int warnings,
        @Nullable Long time,
        @Nullable Boolean noOp
    ) implements TaskFinishData {
        public CompileReport(Label target, int errors, int warnings) {
            this(target, errors, warnings, null, null);
        }
    }

    public record TestFinish(
        String displayName,
        TestStatus status,
        @Nullable String message,
        @Nullable Location location,
        @Nullable TestFinishData data
    ) implements TaskFinishData {
        public TestFinish(String displayName, TestStatus status) {
            this(displayName, status, null, null, null);
        }
    }

    public sealed interface TestFinishData {
    }

    public record JUnitStyleTestCaseData(
        @Nullable Double time,
        @Nullable String className,
        @Nullable String errorMessage,
        @Nullable String output,
        @Nullable String errorType
    ) implements TestFinishData {
    }

    public record JUnitStyleTestSuiteData(
        @Nullable Double time,
        @Nullable String systemOut,
        @Nullable String systemErr
    ) implements TestFinishData {
    }
}
