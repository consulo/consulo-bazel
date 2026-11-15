package org.jetbrains.bazel.flow.exclude;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.dumb.DumbAware;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.performance.BspTracer;
import org.jetbrains.bazel.workspace.ExcludeSymlinksFromFileWatcher;
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelSymlinkExcludeService implements DumbAware, Disposable {
    private static final Logger LOG = Logger.getInstance(BazelSymlinkExcludeService.class);

    private final Project myProject;

    // consulo: upstream MutableSharedFlow + debounce(1.seconds); a pending refresh is rescheduled on every update
    private final Object myRefreshLock = new Object();
    private @Nullable ScheduledFuture<?> myPendingRefresh;

    private volatile Set<Path> mySymlinksToExclude = Set.of();

    @Inject
    public BazelSymlinkExcludeService(Project project) {
        myProject = project;
    }

    public Set<Path> getBazelSymlinksToExclude() {
        return mySymlinksToExclude;
    }

    public CompletableFuture<Set<Path>> scanForBazelSymlinksToExclude(Path bazelWorkspace) {
        CompletableFuture<Set<Path>> computedSymlinksFuture = BspTracer.measure(
            "BazelSymlinkExcludeService.scanForBazelSymlinksToExclude.ms",
            () -> CompletableFuture.supplyAsync(() -> {
                LOG.info("Scanning directory for bazel symlinks to exclude: " + bazelWorkspace);
                return BazelSymlinksCalculator.calculateBazelSymlinksToExclude(bazelWorkspace, BazelFeatureFlags.getSymlinkScanMaxDepth());
            }, AppExecutorUtil.getAppExecutorService())
        );

        return computedSymlinksFuture.thenCompose(computedSymlinks -> {
            if (!computedSymlinks.isEmpty()) {
                LOG.info("Found bazel symlinks to exclude during workspace scan: " + computedSymlinks);
                // consulo: upstream backgroundWriteAction
                return myProject.getUIAccess().giveAsync(() -> {
                    WriteAction.run(() -> addBazelSymlinksToExclude(computedSymlinks));
                }).thenApply(unused -> computedSymlinks);
            }

            return CompletableFuture.completedFuture(computedSymlinks);
        });
    }

    @RequiredWriteAction
    public void addBazelSymlinksToExclude(Set<Path> newSymlinks) {
        Application.get().assertWriteAccessAllowed();
        LOG.info("Excluding newly detected Bazel symlinks: " + newSymlinks);
        ExcludeSymlinksFromFileWatcher.excludeSymlinksFromFileWatcher(new ArrayList<>(newSymlinks));
        Set<Path> symlinksToExclude = new LinkedHashSet<>(mySymlinksToExclude);
        symlinksToExclude.addAll(newSymlinks);
        mySymlinksToExclude = Set.copyOf(symlinksToExclude);
        // Make sure that IntelliJ's workspace machinery will pull the most recent
        // list of excluded directories from BazelDirectoryIndexExcludePolicy immediately.
        // Otherwise, LVCS can start iterating over Bazel's symlinks, effectively freezing the IDE.
        // TODO consulo: WorkspaceFileIndexEx.indexData.resetCustomContributors() has no public Consulo counterpart
        scheduleRefreshWorkspaceModel();
    }

    private void scheduleRefreshWorkspaceModel() {
        synchronized (myRefreshLock) {
            ScheduledFuture<?> pendingRefresh = myPendingRefresh;
            if (pendingRefresh != null) {
                pendingRefresh.cancel(false);
            }
            // batch bulk symlink updates
            myPendingRefresh = AppExecutorUtil.getAppScheduledExecutorService().schedule(this::refreshWorkspaceModel, 1, TimeUnit.SECONDS);
        }
    }

    public void refreshWorkspaceModel() {
        LOG.info("Refreshing workspace model with excluded symlinks");
        List<String> newSymlinksUrls = mySymlinksToExclude.stream().map(it -> VirtualFileUtil.pathToUrl(it.toString())).toList();
        // consulo: upstream WorkspaceModel.update on BazelProjectDirectoriesEntity; the entity is project data of BazelProjectModelService
        BazelProjectModelService modelService = BazelProjectModelService.getInstance(myProject);
        BazelProjectDirectoriesEntity bazelProjectDirectoriesEntity = modelService.getProjectData(BazelProjectDirectoriesEntity.class);
        if (bazelProjectDirectoriesEntity == null) {
            return;
        }
        Set<String> excludedRoots = new LinkedHashSet<>(bazelProjectDirectoriesEntity.excludedRoots());
        excludedRoots.addAll(newSymlinksUrls);
        modelService.putProjectData(
            BazelProjectDirectoriesEntity.class,
            new BazelProjectDirectoriesEntity(
                bazelProjectDirectoriesEntity.projectRoot(),
                bazelProjectDirectoriesEntity.includedRoots(),
                List.copyOf(excludedRoots),
                bazelProjectDirectoriesEntity.indexPatterns(),
                bazelProjectDirectoriesEntity.indexableRecursiveRoots(),
                bazelProjectDirectoriesEntity.indexableNonRecursiveRoots()
            )
        );
    }

    @Override
    public void dispose() {
        synchronized (myRefreshLock) {
            ScheduledFuture<?> pendingRefresh = myPendingRefresh;
            if (pendingRefresh != null) {
                pendingRefresh.cancel(false);
            }
        }
    }

    public static BazelSymlinkExcludeService getInstance(Project project) {
        return project.getInstance(BazelSymlinkExcludeService.class);
    }
}
