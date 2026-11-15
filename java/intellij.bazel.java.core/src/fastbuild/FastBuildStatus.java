package org.jetbrains.bazel.fastbuild;

import consulo.virtualFileSystem.VirtualFile;

import java.nio.file.Path;

public record FastBuildStatus(Path workspaceRoot, FastBuildActionStatus status) {
    // consulo: upstream declares this public data class as a top-level declaration in the same file
    public record FastBuildTargetStatus(
        VirtualFile inputFile,
        Path targetJar,
        FastBuildActionStatus status
    ) {
    }
}
