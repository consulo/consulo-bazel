package org.jetbrains.bazel.languages.starlark.repomapping;

import consulo.project.Project;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;

import java.nio.file.Path;
import java.util.Map;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class BazelRepoMappingProvider {
    private BazelRepoMappingProvider() {
    }

    // consulo: WorkspaceSnapshotService.snapshot (StateFlow) is replaced by its current value
    public static RepoMapping getRepoMapping(Project receiver) {
        return receiver.getInstance(WorkspaceSnapshotService.class).getSnapshot().get().repoMapping();
    }

    public static Map<String, Path> getCanonicalRepoNameToPath(Project receiver) {
        Path workspaceRoot = receiver.getInstance(WorkspaceSnapshotService.class).getSnapshot().get().bazelInfo().workspaceRoot();
        return RepoMapping.canonicalRepoNameToPath(getRepoMapping(receiver), workspaceRoot);
    }
}
