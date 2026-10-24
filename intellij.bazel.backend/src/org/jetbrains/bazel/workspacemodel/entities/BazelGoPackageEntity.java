package org.jetbrains.bazel.workspacemodel.entities;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetKey;

import java.util.List;
import java.util.Set;

/**
 * Represents a Go target, like `go_library` or `go_test`, with a caveat:
 * targets with the same `importpath` are grouped together into one package.
 * <p>
 * The reason for this grouping is that `rules_go` allows embedding targets with the same `importpath` into one another via `embed`
 * <a href="https://github.com/bazel-contrib/rules_go/blob/master/docs/go/core/embedding.md">(see docs)</a>.
 * In this case the sources are compiled together as if they were one target.
 * That means we cannot know in which context a source file is used in:
 * in the context of the target where the source is actually defined, or in another target with an `embed` attribute.
 * <p>
 * consulo: classic data instead of a workspace entity; {@code sources} are VFS urls.
 */
public record BazelGoPackageEntity(
    String importPath,
    List<String> sources,
    Set<String> directDepsImportPaths
) {
    public ImportPathId getSymbolicId() {
        return new ImportPathId(importPath);
    }

    public record ImportPathId(String importPath) {
        public String getPresentableName() {
            return importPath;
        }
    }

    public record BazelGoTargetEntityId(WorkspaceModelTargetKey targetKey) {
        public String getPresentableName() {
            return toString();
        }
    }

    public record BazelGoTargetEntity(
        WorkspaceModelTargetKey _targetKey,
        ImportPathId importPath
    ) {
        public BazelGoTargetEntityId getSymbolicId() {
            return new BazelGoTargetEntityId(_targetKey);
        }

        public WorkspaceTargetKey getTargetKey() {
            return _targetKey.toWorkspaceTarget();
        }
    }
}
