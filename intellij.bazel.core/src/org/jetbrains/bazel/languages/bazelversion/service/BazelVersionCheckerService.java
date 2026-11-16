package org.jetbrains.bazel.languages.bazelversion.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// consulo: StoragePathMacros.CACHE_FILE does not exist in Consulo, the cache is stored in the workspace file
@State(name = "BazelVersionCache", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelVersionCheckerService implements PersistentStateComponent<BazelVersionCheckerService.State> {
    // consulo: upstream BaseState with string() delegates -> plain bean
    public static class State {
        public @Nullable String resolverId = null;
        public @Nullable String currentBazelVersion = null;
        public @Nullable String latestBazelVersion = null;
    }

    private final Project myProject;

    private @Nullable String myResolverId = null;

    private @Nullable BazelVersionLiteral myLatestBazelVersion = null;

    private @Nullable BazelVersionLiteral myCurrentBazelVersion = null;

    @Inject
    public BazelVersionCheckerService(Project project) {
        myProject = project;
    }

    public @Nullable BazelVersionLiteral getLatestBazelVersion() {
        return myLatestBazelVersion;
    }

    public @Nullable BazelVersionLiteral getCurrentBazelVersion() {
        return myCurrentBazelVersion;
    }

    @Override
    public BazelVersionCheckerService.State getState() {
        State it = new State();
        it.resolverId = myResolverId;
        it.currentBazelVersion =
            myCurrentBazelVersion != null ? BazelVersionLiteral.toBazelVersionStringLiteral(myCurrentBazelVersion) : null;
        it.latestBazelVersion =
            myLatestBazelVersion != null ? BazelVersionLiteral.toBazelVersionStringLiteral(myLatestBazelVersion) : null;
        return it;
    }

    @Override
    public void loadState(BazelVersionCheckerService.State state) {
        myResolverId = state.resolverId;
        myCurrentBazelVersion =
            state.currentBazelVersion != null ? BazelVersionLiteral.fromString(state.currentBazelVersion) : null;
        myLatestBazelVersion =
            state.latestBazelVersion != null ? BazelVersionLiteral.fromString(state.latestBazelVersion) : null;
    }

    public CompletableFuture<Void> refreshLatestBazelVersion(Project project, @Nullable BazelVersionLiteral currentVersion) {
        if (myResolverId == null || myResolverId.isBlank()) {
            BazelVersionResolver first = BazelVersionResolver.getEp().findFirstSafe(it -> true);
            if (first == null) {
                return CompletableFuture.completedFuture(null);
            }
            myResolverId = first.getId();
        }
        BazelVersionResolver resolver =
            BazelVersionResolver.getEp().findFirstSafe(it -> Objects.equals(it.getId(), getState().resolverId));
        if (resolver == null) {
            return CompletableFuture.completedFuture(null);
        }
        myCurrentBazelVersion = currentVersion;
        return resolver.resolveLatestBazelVersion(project, currentVersion)
            .thenAccept(latestBazelVersion -> {
                if (latestBazelVersion == null) {
                    return;
                }
                myLatestBazelVersion = latestBazelVersion;
            });
    }
}
