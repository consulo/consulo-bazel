// consulo: upstream server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/languages/rust/RustModule.kt (removed in
// 4699fc65a). Upstream RustModule was the LanguageData of the BSP server model, here it is the BuildTargetData of the workspace
// snapshot.
package org.jetbrains.bazel.rust.sync;

import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * consulo: {@code crateRoot} (upstream a URI) and {@code procMacroArtifacts} (upstream exec root paths) are {@link OutputLocation}s,
 * which the importer resolves. Upstream {@code var isExternalModule} (set by the BSP project mapper, never read by the Rust code) is
 * dropped: the snapshot data is immutable.
 * <p>
 * consulo: {@code outDir} (the build script output directory, upstream sent by the aspect but never read) and {@code cfgOptions}
 * (the {@code --cfg} options of the crate's Rustc action, a field of the Consulo aspect) are Consulo-only, consulo-rust reads them.
 */
public record RustModule(
    String crateId,
    RustCrateLocation location,
    boolean fromWorkspace,
    String name,
    String kind,
    String edition,
    List<String> crateFeatures,
    List<String> dependenciesCrateIds,
    OutputLocation crateRoot,
    String version,
    List<OutputLocation> procMacroArtifacts,
    // consulo: Consulo-only, see above
    @Nullable OutputLocation outDir,
    List<String> cfgOptions
) implements BuildTargetData {
    // consulo: upstream top-level enum class RustCrateLocation of RustModule.kt
    public enum RustCrateLocation {
        WORKSPACE_DIR,
        EXEC_ROOT,
    }

    // consulo: like JvmBuildTarget.extractJvmBuildTarget
    public static @Nullable RustModule extractRustModule(BuildTarget target) {
        return BuildTarget.extractData(target, RustModule.class);
    }
}
