package org.jetbrains.bazel.golang.sync;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record GoBuildTarget(
    @Nullable OutputLocation sdkHomePath,
    String importPath,
    OutputLocationCollection sources,
    List<WorkspaceTargetKey> embed
) implements BuildTargetData {
}
