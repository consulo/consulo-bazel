// consulo: upstream protocol/src/main/kotlin/org/jetbrains/bsp/protocol/RustPackage.kt (removed in 4699fc65a)
package org.jetbrains.bazel.rust.protocol;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record RustPackage(
    String id,
    String rootUrl,
    String name,
    String version,
    String origin,
    String edition,
    List<RustTarget> resolvedTargets,
    List<RustTarget> allTargets,
    Map<String, Set<String>> features,
    Set<String> enabledFeatures,
    @Nullable String source,
    @Nullable Map<String, List<String>> cfgOptions,
    @Nullable Map<String, String> env,
    @Nullable String outDirUrl,
    @Nullable String procMacroArtifact
) {
}
