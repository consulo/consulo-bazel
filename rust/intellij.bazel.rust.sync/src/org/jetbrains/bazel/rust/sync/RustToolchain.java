package org.jetbrains.bazel.rust.sync;

import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jspecify.annotations.Nullable;

/**
 * The Rust toolchain a crate is compiled with ({@code RustCrateInfo.toolchain} of the Consulo aspect).
 * <p>
 * consulo: Consulo-only, no upstream counterpart (the BSP server did not report the toolchain of a crate). The paths are exec root
 * relative: rustc and the sysroot are files that rules_rust generates in the output tree (symlinks into the toolchain repository),
 * the importer resolves them against the execution root.
 *
 * @param rustc        the exec root path of rustc of the sysroot
 * @param cargo        the exec root path of cargo of the sysroot, {@code null} if the toolchain does not ship it
 * @param sysroot      the exec root path of the sysroot directory
 * @param targetTriple the {@code --target} of the crate: a target triple or the path of a target json
 * @param version      never reported by the aspect (rules_rust's ToolchainInfo is not available to it), consulo-rust asks rustc
 */
public record RustToolchain(
    @Nullable String rustc,
    @Nullable String cargo,
    @Nullable String sysroot,
    @Nullable String targetTriple,
    @Nullable String version
) implements BuildTargetData {
    public static @Nullable RustToolchain extractRustToolchain(BuildTarget target) {
        return BuildTarget.extractData(target, RustToolchain.class);
    }
}
