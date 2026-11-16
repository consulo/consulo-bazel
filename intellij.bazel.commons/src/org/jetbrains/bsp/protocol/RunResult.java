package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.BazelStatus;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Set;

/**
 * @param infoFiles the text proto files of the `intellij-info` output group
 */
public record RunResult(
    TaskId taskId,
    int exitCode,
    @Nullable String stdout,
    Set<Path> infoFiles
) {
    // consulo: overload for the upstream default argument (infoFiles = emptySet())
    public RunResult(TaskId taskId, int exitCode, @Nullable String stdout) {
        this(taskId, exitCode, stdout, Set.of());
    }

    public BazelStatus statusCode() {
        return BazelStatus.fromExitCode(exitCode);
    }
}
