// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustWorkspaceResult.kt (removed in 4699fc65a).
// Upstream RustWorkspaceParams (the BSP request) has no counterpart: the importer computes the result from the snapshot.
package org.jetbrains.bazel.rust.protocol;

import org.jetbrains.bazel.label.Label;

import java.util.List;
import java.util.Map;

public record RustWorkspaceResult(
    List<RustPackage> packages,
    Map<String, List<RustRawDependency>> rawDependencies,
    Map<String, List<RustDependency>> dependencies,
    List<Label> resolvedTargets
) {
}
