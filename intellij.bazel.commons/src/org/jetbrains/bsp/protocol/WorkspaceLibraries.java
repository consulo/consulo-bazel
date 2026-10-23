package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceTargetKey;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WorkspaceLibraries {
    private WorkspaceLibraries() {
    }

    public record LibraryItem(
        WorkspaceTargetKey key,
        List<Path> ijars,
        List<Path> jars,
        List<Path> sourceJars,
        @Nullable MavenCoordinates mavenCoordinates,
        boolean containsInternalJars
    ) {
    }

    public static List<Path> getAllJars(LibraryItem receiver) {
        List<Path> result = new ArrayList<>(receiver.ijars().size() + receiver.jars().size() + receiver.sourceJars().size());
        result.addAll(receiver.ijars());
        result.addAll(receiver.jars());
        result.addAll(receiver.sourceJars());
        return result;
    }

    public record MavenCoordinates(
        String groupId,
        String artifactId,
        String version
    ) {
    }
}
