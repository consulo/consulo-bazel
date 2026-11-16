package org.jetbrains.bazel.flow.vcs;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.VcsRootError;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.symlinks.BazelConvenienceSymlinksScanner;
import org.jetbrains.bazel.flow.exclude.BazelSymlinkExcludeService;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * <a href="https://youtrack.jetbrains.com/issue/BAZEL-948">BAZEL-948</a>: Bazel's symlink forest plants a `.git` symlink into the
 * execution root, so the execution root, and the Bazel convenience symlinks pointing into it, look like Git repositories.
 * Git running over such a root processes the whole Bazel output tree, which can freeze a large project.
 * <p>
 * The invariant: the Bazel plugin keeps VCS roots out of the Bazel output directories, and only out of them.
 * A Bazel output directory is one under a Bazel convenience symlink of the workspace (`bazel-bin`, `bazel-out`, `bazel-&lt;workspace&gt;`, ...),
 * or one that really lies in the tree such a symlink or the execution root points to.
 * Every other VCS root is left to the platform VCS root detection, as if there was no Bazel.
 * <p>
 * As a {@code VcsRootErrorFilter}, it keeps the VCS root detection from reporting, and thus auto-registering, roots in the Bazel output directories.
 * Detection reaches them through a Bazel symlink that isn't excluded yet,
 * or by walking up from a content root in the execution root, e.g., a Python import root under `bazel-bin`.
 * Only the unregistered roots in the Bazel output directories are dropped; every other error is left to the platform.
 * The mappings persisted before are removed by {@link #removeBazelOutputMappings}.
 */
// TODO consulo: VcsRootErrorFilter is missing in Consulo (consulo.versionControlSystem.VcsRootChecker cannot filter the detected
//  root errors), so the class implements no platform interface and is not registered; removeBazelOutputMappings is still usable
public class BazelVcsRootErrorFilter {
    private static final Logger LOG = Logger.getInstance(BazelVcsRootErrorFilter.class);

    public Collection<VcsRootError> filterErrors(Project project, Collection<VcsRootError> errors) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project) ||
            errors.stream().noneMatch(it -> it.getType() == VcsRootError.Type.UNREGISTERED_ROOT)) {
            return errors;
        }
        // consulo: upstream runBlockingMaybeCancellable { }
        WorkspaceSnapshot snapshot = WorkspaceHelpers.currentWorkspaceSnapshotOrNull(project).join();
        IsBazelOutputDirectoryPredicate isBazelOutputDirectory = bazelOutputDirectoryPredicate(project, snapshot);
        List<VcsRootError> result = new ArrayList<>();
        for (VcsRootError it : errors) {
            if (!(it.getType() == VcsRootError.Type.UNREGISTERED_ROOT && isBazelOutputDirectory.invoke(it.getMapping().getDirectory()))) {
                result.add(it);
            }
        }
        return result;
    }

    public interface IsBazelOutputDirectoryPredicate {
        boolean invoke(String directory);
    }

    /**
     * Returns whether a VCS root directory lies in a Bazel output directory.
     * <p>
     * The real output trees come from the Bazel symlinks {@link BazelSymlinkExcludeService} has already collected and from the execution root.
     * A directory under a Bazel symlink is recognized even before that collection finishes, by checking its own path.
     */
    public static IsBazelOutputDirectoryPredicate bazelOutputDirectoryPredicate(Project project, @Nullable WorkspaceSnapshot snapshot) {
        VirtualFile projectRootDir = BazelProjectInfoService.getBazelProjectInfo(project).getProjectRootDir();
        @Nullable Path workspace = projectRootDir != null ? toNioPathOrNull(projectRootDir) : null;
        @Nullable BazelConvenienceSymlinksScanner symlinksScanner =
            workspace != null ? new BazelConvenienceSymlinksScanner(workspace) : null;
        Set<Path> knownSymlinks = BazelSymlinkExcludeService.getInstance(project).getBazelSymlinksToExclude();
        List<Path> candidates = new ArrayList<>(knownSymlinks);
        if (snapshot != null) {
            candidates.add(snapshot.bazelInfo().execRoot());
        }
        Set<Path> outputTrees = new HashSet<>();
        for (Path it : candidates) {
            Path realPath = PathUtil.toRealPathOrNull(it);
            if (realPath != null) {
                outputTrees.add(realPath);
            }
        }

        return directory -> {
            Path path = toNioPathOrNull(directory);
            if (path == null) {
                return false;
            }
            if (knownSymlinks.stream().anyMatch(path::startsWith)) {
                return true;
            }
            // VCS root detection can run before BazelSymlinkExcludeStartupActivity finishes collecting the symlinks
            if (symlinksScanner != null && symlinksScanner.isUnderBazelSymlink(path)) {
                return true;
            }
            if (outputTrees.isEmpty()) {
                return false;
            }
            Path realPath = PathUtil.toRealPathOrNull(path);
            if (realPath == null) {
                return false;
            }
            return outputTrees.stream().anyMatch(realPath::startsWith);
        };
    }

    /**
     * Removes the VCS mappings in the Bazel output directories, e.g., the ones an earlier session persisted in `vcs.xml`.
     * Called at project initialization, before the VCS initialization activates Git for them;
     * afterwards, {@link BazelVcsRootErrorFilter} keeps new ones from being auto-registered.
     */
    public static CompletableFuture<?> removeBazelOutputMappings(Project project) {
        ProjectLevelVcsManager manager = ProjectLevelVcsManager.getInstance(project);
        List<VcsDirectoryMapping> mappings = manager.getDirectoryMappings();
        if (mappings.isEmpty() || mappings.stream().allMatch(VcsDirectoryMapping::isDefaultMapping)) {
            return CompletableFuture.completedFuture(null);
        }

        // consulo: upstream withContext(Dispatchers.IO) { }
        return WorkspaceHelpers.currentWorkspaceSnapshotOrNull(project)
            .thenApplyAsync(snapshot -> {
                IsBazelOutputDirectoryPredicate isBazelOutputDirectory = bazelOutputDirectoryPredicate(project, snapshot);
                List<VcsDirectoryMapping> bazelOutputMappings = new ArrayList<>();
                List<VcsDirectoryMapping> otherMappings = new ArrayList<>();
                for (VcsDirectoryMapping it : mappings) {
                    if (!it.isDefaultMapping() && isBazelOutputDirectory.invoke(it.getDirectory())) {
                        bazelOutputMappings.add(it);
                    }
                    else {
                        otherMappings.add(it);
                    }
                }
                return new MappingsPartition(bazelOutputMappings, otherMappings);
            }, Application.get().getInstance(ApplicationConcurrency.class).executor())
            .thenAccept(partition -> {
                List<VcsDirectoryMapping> bazelOutputMappings = partition.bazelOutputMappings();
                List<VcsDirectoryMapping> otherMappings = partition.otherMappings();
                if (bazelOutputMappings.isEmpty()) {
                    return;
                }

                LOG.info("Removing VCS mappings in Bazel output directories: " +
                    bazelOutputMappings.stream().map(VcsDirectoryMapping::getDirectory).toList());
                manager.setDirectoryMappings(otherMappings);
            });
    }

    // consulo: Kotlin `partition` result
    private record MappingsPartition(List<VcsDirectoryMapping> bazelOutputMappings, List<VcsDirectoryMapping> otherMappings) {
    }

    // consulo: IntelliJ String.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(String path) {
        try {
            return Path.of(path);
        }
        catch (InvalidPathException e) {
            return null;
        }
    }

    // consulo: IntelliJ VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
