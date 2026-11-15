package org.jetbrains.bazel.startup;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * consulo: upstream declares this service in {@code BazelStartupActivityTracker.kt}; it is a public top-level class here because
 * the annotation processor cannot handle a nested or package-private {@code @ServiceImpl} used by other packages.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BspConfigurationTrackerService {
    public static BspConfigurationTrackerService getInstance(Project project) {
        return project.getInstance(BspConfigurationTrackerService.class);
    }

    public static CompletableFuture<BspConfigurationTrackerService> getInstanceAsync(Project project) {
        return CompletableFuture.completedFuture(getInstance(project));
    }

    // consulo: kotlinx MutableStateFlow<Boolean> replaced by an observable value
    private final MutableObservableValue<Boolean> myIsRunning = MutableObservableValue.of(false);

    @Inject
    public BspConfigurationTrackerService() {
    }

    public MutableObservableValue<Boolean> getIsRunning() {
        return myIsRunning;
    }

    public boolean isRunning() {
        return myIsRunning.get();
    }

    public void setRunning(boolean value) {
        myIsRunning.set(value);
    }
}
