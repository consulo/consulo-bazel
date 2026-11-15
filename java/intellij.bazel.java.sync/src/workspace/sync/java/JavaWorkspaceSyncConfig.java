package org.jetbrains.bazel.sync.workspace.languages.java;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSyncConfig;

import java.util.List;

// RC: every property that can affect output `JavaBazelWorkspaceImporter` shall be included here
public record JavaWorkspaceSyncConfig(
    List<String> testSourcesPatterns,
    boolean excludeCompiledSourceCodeInsideJars
) implements WorkspaceSyncConfig {
}
