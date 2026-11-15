package org.jetbrains.bazel.workspace;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.ProjectPostSyncHook;

import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class CleanupLabelsCacheSyncHook implements ProjectPostSyncHook {
    @Override
    public CompletableFuture<Void> onPostSync(ProjectPostSyncHook.ProjectPostSyncHookEnvironment environment) {
        Label.cleanInternCache();
        return CompletableFuture.completedFuture(null);
    }
}
