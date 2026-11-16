package com.intellij.bazel.python.backend.sync;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSyncConfig;

// consulo: upstream `object PythonWorkspaceSyncConfig`, the snapshot serializer recognizes the INSTANCE field as a singleton
public final class PythonWorkspaceSyncConfig implements WorkspaceSyncConfig {
    public static final PythonWorkspaceSyncConfig INSTANCE = new PythonWorkspaceSyncConfig();

    private PythonWorkspaceSyncConfig() {
    }
}
