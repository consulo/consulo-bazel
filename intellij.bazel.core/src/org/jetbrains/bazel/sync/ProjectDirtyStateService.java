package org.jetbrains.bazel.sync;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.externalSystem.autoimport.ExternalSystemProjectTracker;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.projectAware.BazelWorkspace;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Holds the Bazel files that changed since the last sync.
 * <p>
 * The plugin tracks these changes itself. It does not use the platform settings-file CRC scan,
 * which recalculates the CRC of every tracked file on each change (BAZEL-3506).
 * <p>
 * A future partial sync reads this state to find the scope of the next sync.
 * <p>
 * [TODO] drop as a part of https://youtrack.jetbrains.com/issue/BAZEL-3570
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class ProjectDirtyStateService {
    /**
     * The Bazel targets/packages that changed since the last sync.
     *
     * @param paths        the changed package directories
     * @param wholeProject true when the plugin cannot tell which part of the project changed
     */
    // consulo: upstream top-level data class of ProjectDirtyStateService.kt
    public record ProjectDirtyState(
        Set<Path> paths,
        boolean wholeProject
    ) {
        // consulo: overloads for the upstream default arguments (paths = emptySet(), wholeProject = false)
        public ProjectDirtyState() {
            this(Set.of(), false);
        }

        public ProjectDirtyState(boolean wholeProject) {
            this(Set.of(), wholeProject);
        }

        public boolean isEmpty() {
            return paths.isEmpty() && !wholeProject;
        }

        public static final ProjectDirtyState EMPTY = new ProjectDirtyState();
    }

    private final Project myProject;

    // consulo: kotlinx MutableStateFlow replaced by an observable value
    private final MutableObservableValue<ProjectDirtyState> myMutableState = MutableObservableValue.of(ProjectDirtyState.EMPTY);

    /**
     * The current dirty state. A consumer can observe it.
     */
    private final ObservableValue<ProjectDirtyState> myState = myMutableState;

    @Inject
    public ProjectDirtyStateService(Project project) {
        myProject = project;
        // consulo: upstream collects `state.map { !it.isEmpty }.distinctUntilChanged()` in the service coroutine scope;
        // the mapped observable value only notifies about changes, the platform is notified on a pooled thread
        myState
            .map(it -> !it.isEmpty())
            .addListener(dirty -> {
                if (dirty) {
                    CompletableFuture.runAsync(this::notifyPlatform, Application.get().getInstance(ApplicationConcurrency.class).executor());
                }
            });
    }

    /**
     * The current dirty state. A consumer can observe it.
     */
    public ObservableValue<ProjectDirtyState> getState() {
        return myState;
    }

    /**
     * Adds {@code paths} to the dirty set. Returns true if the state changed.
     */
    public boolean markDirty(Collection<Path> paths) {
        if (paths.isEmpty()) {
            return false;
        }
        ProjectDirtyState old = myMutableState.get();
        if (old.wholeProject()) {
            return false;
        }
        if (old.paths().containsAll(paths)) {
            return false;
        }

        Set<Path> newPaths = new LinkedHashSet<>(old.paths());
        newPaths.addAll(paths);
        ProjectDirtyState next;
        if (newPaths.size() > MAX_TRACKED_PATHS) {
            LOG.info("Too many dirty Bazel files in " + myProject.getName() + ". Marking the whole project.");
            next = new ProjectDirtyState(/* wholeProject = */ true);
        }
        else {
            next = new ProjectDirtyState(newPaths, old.wholeProject());
        }
        myMutableState.set(next);
        return true;
    }

    /**
     * Marks the whole project. Use it when the plugin cannot tell which files changed.
     */
    public boolean markWholeProjectDirty(String reason) {
        if (myMutableState.get().wholeProject()) {
            return false;
        }
        LOG.info("Marking the whole Bazel project " + myProject.getName() + " as dirty: " + reason);
        myMutableState.set(new ProjectDirtyState(/* wholeProject = */ true));
        return true;
    }

    public ProjectDirtyState current() {
        return myMutableState.get();
    }

    public boolean isDirty() {
        return !myMutableState.get().isEmpty();
    }

    public void finishSync(ProjectSyncScope scope) {
        myMutableState.update(current -> {
            // [todo] derive state with difference
            return ProjectDirtyState.EMPTY;
        });
    }

    public void clearAll() {
        myMutableState.set(ProjectDirtyState.EMPTY);
    }

    /**
     * Project disposal runs inside a write action, so the read action keeps the disposal from starting during this call (BAZEL-3453).
     * The call does not create {@link BazelWorkspace} or the tracker. Without a registered {@link BazelWorkspace}, there is nothing to mark.
     */
    private void notifyPlatform() {
        ReadAction.run(() -> {
            if (myProject.isDisposed()) {
                return;
            }
            BazelWorkspace workspace = myProject.getInstanceIfCreated(BazelWorkspace.class);
            if (workspace == null) {
                return;
            }
            ExternalSystemProjectTracker tracker = myProject.getInstanceIfCreated(ExternalSystemProjectTracker.class);
            if (tracker == null) {
                return;
            }
            // consulo: upstream calls the internal markDirtyInternal(), Consulo exposes markDirty()
            tracker.markDirty(workspace.getProjectId());
        });
    }

    public static ProjectDirtyStateService getInstance(Project project) {
        return project.getInstance(ProjectDirtyStateService.class);
    }

    /**
     * The largest number of dirty files to track. Above it the service marks the whole project.
     * A branch switch can change hundreds of thousands of files.
     */
    private static final int MAX_TRACKED_PATHS = 5_000;

    private static final Logger LOG = Logger.getInstance(ProjectDirtyStateService.class);
}
