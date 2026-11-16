package org.jetbrains.bazel.workspace;

import consulo.application.Application;
import consulo.logging.Logger;
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService;

import java.nio.file.Path;
import java.util.List;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class ExcludeSymlinksFromFileWatcher {
    private static final Logger LOG = Logger.getInstance(ExcludeSymlinksFromFileWatcher.class);

    private ExcludeSymlinksFromFileWatcher() {
    }

    /**
     * WatchRootsServiceImpl doesn't check whether the symlinks it's watching are excluded.
     * Until it is fixed on the platform side, tell the service explicitly which symlinks it must not follow.
     * See:
     * https://youtrack.jetbrains.com/issue/IJPL-199364/Excluded-symlinks-are-watched-by-WatchRootsManager
     * https://youtrack.jetbrains.com/issue/BAZEL-2235/Overly-aggressive-fsnotifier
     */
    public static void excludeSymlinksFromFileWatcher(List<Path> symlinksToExclude) {
        if (symlinksToExclude.isEmpty()
            || Application.get().getInstance(BazelApplicationContextService.class).getDisableFileWatcherSymlinkExclusion()) {
            return;
        }

        // TODO consulo: Consulo has no WatchRoots / WatchRootsServiceImpl.excludeSymlinks, the symlinks stay watched
        //try {
        //    WatchRoots watchRoots = WatchRoots.getInstance() instanceof WatchRootsServiceImpl impl ? impl : null;
        //    if (watchRoots == null) {
        //        return;
        //    }
        //    List<Path> paths = new ArrayList<>(symlinksToExclude);
        //    for (Path symlink : symlinksToExclude) {
        //        Path target = BazelConvenienceSymlinksScanner.resolveSymlinkTarget(symlink);
        //        if (target != null) {
        //            paths.add(target);
        //        }
        //    }
        //    watchRoots.excludeSymlinks(paths);
        //}
        //catch (Throwable e) {
        //    LOG.error("Couldn't exclude symlinks " + symlinksToExclude + " from FileWatcher", e);
        //}
    }
}
