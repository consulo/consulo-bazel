package org.jetbrains.bazel.ui.widgets.fileTargets;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.sync.ProjectPostSyncHook;

import java.util.concurrent.CompletableFuture;

/**
 * A sync hook that updates the BazelFileTargetsWidget when a sync event occurs.
 */
@ExtensionImpl
class BazelFileTargetsWidgetSyncHook implements ProjectPostSyncHook {
    public BazelFileTargetsWidgetSyncHook() {
    }

    @Override
    public CompletableFuture<Void> onPostSync(ProjectPostSyncHookEnvironment environment) {
        return BazelFileTargetsWidget.updateBazelFileTargetsWidget(environment.project());
    }
}
