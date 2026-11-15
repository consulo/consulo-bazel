package org.jetbrains.bazel.startup;

import consulo.project.Project;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.step.AwaitValue;

import java.util.concurrent.CompletableFuture;

// TODO consulo: ActivityTracker (com.intellij.platform.backend.observation, <activityTracker>) is missing in Consulo,
//  the class implements no platform interface and is not registered
// consulo: BspConfigurationTrackerService (declared in this file upstream) is in its own file
class BazelStartupActivityTracker {
    public String getPresentableName() {
        return "bsp-sync";
    }

    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    public CompletableFuture<?> awaitConfiguration(Project project) {
        return isStartupRunning(project).thenCompose(isRunning -> {
            // consulo: upstream isRunning.first { !it }
            CoroutineScope scope = CoroutineScope.of(project.coroutineContext());
            return Coroutine.first(AwaitValue.<Object, Boolean>until(isRunning, it -> !it))
                .runAsync(scope, null)
                .toFuture();
        });
    }

    public CompletableFuture<Boolean> isInProgress(Project project) {
        return isStartupRunning(project).thenApply(MutableObservableValue::get);
    }

    private CompletableFuture<MutableObservableValue<Boolean>> isStartupRunning(Project project) {
        return BspConfigurationTrackerService.getInstanceAsync(project).thenApply(BspConfigurationTrackerService::getIsRunning);
    }
}
