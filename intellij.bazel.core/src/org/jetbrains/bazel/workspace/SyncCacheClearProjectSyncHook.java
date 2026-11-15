package org.jetbrains.bazel.workspace;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.sync.ProjectPostSyncHook;
import org.jetbrains.bazel.sync.SyncCache;

import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class SyncCacheClearProjectSyncHook implements ProjectPostSyncHook {
    @Override
    public CompletableFuture<Void> onPostSync(ProjectPostSyncHook.ProjectPostSyncHookEnvironment environment) {
        SyncCache.getInstance(environment.project()).clear();
        return CompletableFuture.completedFuture(null);
    }
}
