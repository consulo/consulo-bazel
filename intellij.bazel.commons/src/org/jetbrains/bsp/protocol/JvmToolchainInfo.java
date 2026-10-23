package org.jetbrains.bsp.protocol;

import java.util.List;

// Underscores needed because this class is used with Gson to parse Bazel's output
public record JvmToolchainInfo(
    String java_home,
    String toolchain_path,
    List<String> jvm_opts
) {
}
