package org.jetbrains.bazel.sync;

import consulo.project.Project;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks;

/**
 * hardlinks don't need to be watched by fsnotifier, because we already handle refresh ourselves.
 * See `targetHardLinkAttributes.lastModifiedTime() == realFile.getLastModifiedTime()` inside
 * {@link org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks}
 * Moreover fsnotifier fails on two files that share the same inode (which is the whole point of hardlinking).
 */
// TODO consulo: WatchRootExcludePolicy (<watchRootExcludePolicy>) is missing in Consulo,
//  the class implements no platform interface and is not registered
class BazelWatchRootExcludePolicy {
    public boolean isApplicable(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project) && BazelFeatureFlags.getHardLinkOutputFiles();
    }

    public boolean shouldExclude(String watchRootPath) {
        return watchRootPath.contains("/" + DefaultBazelOutputFileHardLinks.HARDLINKS_DIR_NAME + "/");
    }
}
