package org.jetbrains.bazel.workspacemodel.entities;

import java.util.List;

/**
 * consulo: classic data instead of a workspace entity (published with
 * {@code BazelModelBuilder.putProjectData}); all roots are VFS urls.
 *
 * @param indexPatterns Patterns used to compute {@code indexableRecursiveRoots} and {@code indexableNonRecursiveRoots}.
 *                      We need to preserve them to properly process file events and keep
 *                      {@code indexableNonRecursiveRoots} up to date.
 */
public record BazelProjectDirectoriesEntity(
    String projectRoot,
    List<String> includedRoots,
    List<String> excludedRoots,
    List<String> indexPatterns,
    List<String> indexableRecursiveRoots,
    List<String> indexableNonRecursiveRoots
) {
}
