package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.BazelStatus;
import org.jspecify.annotations.Nullable;

public record RunResult(TaskId taskId, BazelStatus statusCode, @Nullable String stdout) {
}
