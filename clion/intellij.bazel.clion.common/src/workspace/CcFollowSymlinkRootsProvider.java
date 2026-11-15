package org.jetbrains.bazel.clion.workspace;

import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * TODO consulo: CIDR {@code OCFollowSymlinkRootsProvider} (extension {@code cidr.lang.followSymlinkRootsProvider}, the
 *  roots under which CLion follows the symlinks of included files) does not exist in consulo-cpp; the class is not
 *  registered
 */
public class CcFollowSymlinkRootsProvider {
    public CcFollowSymlinkRootsProvider() {
    }

    public Collection<Path> getRoots(Project project) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return List.of();
        }
        WorkspaceSnapshot snapshot = project.getInstance(WorkspaceSnapshotService.class).getSnapshot().get();
        Path outputBase = snapshot.bazelInfo().outputBase();

        // consulo: upstream imports HARDLINKS_DIR_NAME of org.jetbrains.bazel.sync.workspace.mapper.normal
        return List.of(outputBase.resolve(DefaultBazelOutputFileHardLinks.HARDLINKS_DIR_NAME));
    }
}
