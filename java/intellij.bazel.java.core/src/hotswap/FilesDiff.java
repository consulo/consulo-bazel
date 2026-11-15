package org.jetbrains.bazel.hotswap;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A data class representing the diff between two sets of files.
 * <p>
 * The type of file in the two sets can be different (F vs S), with a one-way F to S mapping
 * provided in {@link #diffFiles}.
 *
 * @param newFileState maps a jar file to its last modified time
 * @param updatedFiles provides a list of updated jar files, which will be used for constructing the class manifest
 * @param removedFiles keeps track of the list of removed jar files, this is used for writing unit test to check for correctness.
 */
public record FilesDiff(
    Map<Path, FileTime> newFileState,
    List<Path> updatedFiles,
    Set<Path> removedFiles
) {
    /**
     * Diffs a collection of files based on timestamps.
     */
    public static FilesDiff diffFileTimestamps(@Nullable Map<Path, FileTime> oldState, Collection<Path> files) {
        Map<Path, FileTime> newState = new LinkedHashMap<>();
        for (Path it : files) {
            if (Files.exists(it)) {
                try {
                    newState.put(it, Files.getLastModifiedTime(it));
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return diffFiles(oldState, newState);
    }

    public static FilesDiff diffFiles(@Nullable Map<Path, FileTime> oldState, Map<Path, FileTime> newState) {
        // Find changed/new files
        Map<Path, FileTime> previous = oldState != null ? oldState : Map.of();
        List<Path> updated = new ArrayList<>();
        for (Map.Entry<Path, FileTime> entry : newState.entrySet()) {
            if (!Objects.equals(entry.getValue(), previous.get(entry.getKey()))) {
                updated.add(entry.getKey());
            }
        }

        // Find removed files
        Set<Path> removed = new HashSet<>(previous.keySet());
        for (Path it : newState.keySet()) {
            removed.remove(it);
        }

        return new FilesDiff(
            /* newFileState = */ newState,
            /* updatedFiles = */ updated,
            /* removedFiles = */ removed
        );
    }
}
