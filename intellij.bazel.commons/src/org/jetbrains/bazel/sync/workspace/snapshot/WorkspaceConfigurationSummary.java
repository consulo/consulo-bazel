package org.jetbrains.bazel.sync.workspace.snapshot;

/**
 * Bazel configuration summary emitted from BEP message
 *
 * @param hash         Full configuration hash
 * @param mnemonic     Platform mnemonic
 * @param platformName Platform name
 * @param cpu          Platform cpu
 * @param isTool       Whether this configuration is used for building tools
 */
public record WorkspaceConfigurationSummary(
    String hash,
    String mnemonic,
    String platformName,
    String cpu,
    boolean isTool
) {
}
