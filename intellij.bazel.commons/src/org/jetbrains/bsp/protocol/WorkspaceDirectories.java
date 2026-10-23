package org.jetbrains.bsp.protocol;

import java.nio.file.Path;
import java.util.List;

public final class WorkspaceDirectories {
    private WorkspaceDirectories() {
    }

    public record WorkspaceDirectoriesResult(
        List<Path> includedDirectories,
        List<Path> excludedDirectories
    ) {
    }
}
