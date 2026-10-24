package org.jetbrains.bazel.workspacemodel.entities;

import java.util.Set;

/**
 * consulo: classic data instead of a workspace entity.
 *
 * @param relativePathsInsideJarToExclude Paths of the project source files whose compiled classes and copies have to be excluded
 *                                        from internal jars. Package of a file inside a jar is approximated by its relative path,
 *                                        see `CompiledSourceFileIndex`.
 */
public record CompiledSourceCodeInsideJarExcludeEntity(
    Set<String> relativePathsInsideJarToExclude,
    Set<String> librariesFromInternalTargetsUrls,
    CompiledSourceCodeInsideJarExcludeId excludeId
) {
    public CompiledSourceCodeInsideJarExcludeId getSymbolicId() {
        return excludeId;
    }

    public record CompiledSourceCodeInsideJarExcludeId(int id) {
        public String getPresentableName() {
            return toString();
        }
    }

    /**
     * consulo: {@code libraryId} is the project library name.
     */
    public record LibraryCompiledSourceCodeInsideJarExcludeEntity(
        String libraryId,
        CompiledSourceCodeInsideJarExcludeId compiledSourceCodeInsideJarExcludeId
    ) {
    }
}
