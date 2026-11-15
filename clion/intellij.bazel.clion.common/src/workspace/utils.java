package org.jetbrains.bazel.clion.workspace;

import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

// consulo: upstream Kotlin top-level (extension) functions of utils.kt
public final class utils {
    private utils() {
    }

    public static String presentable(WorkspaceTargetKey receiver) {
        String checksum = receiver.configuration().getShortChecksum();
        return receiver.label().toString() + (checksum != null ? " (" + checksum + ")" : "");
    }

    public static @Nullable Path resolveOutputLocation(CcImportContext receiver, OutputLocation location) {
        return receiver.getOutputResolver().resolve(location, RepoMapping.getLocalRepositories(receiver.getSnapshot().repoMapping()));
    }

    public static @Nullable Path resolveExecutionRootPath(CcImportContext receiver, String path) {
        return resolveOutputLocation(receiver, receiver.getOutputParser().parseExecrootPath(path));
    }
}
