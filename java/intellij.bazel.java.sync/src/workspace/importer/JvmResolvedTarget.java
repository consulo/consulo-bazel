package org.jetbrains.bazel.workspace.importer;

import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JvmDependency;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.WorkspaceLibraries.LibraryItem;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public record JvmResolvedTarget(
    WorkspaceTargetKey key,
    List<LibraryItem> libraries,
    List<JvmDependency> jvmDependencies,
    @Nullable Path javaHome,
    String javaVersion
) {
}
