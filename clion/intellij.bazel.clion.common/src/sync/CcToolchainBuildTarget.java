package org.jetbrains.bazel.clion.sync;

import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public record CcToolchainBuildTarget(
    String targetName,
    String compilerName,
    StringsCollection cppOption,
    StringsCollection cOption,
    OutputLocation cCompiler,
    OutputLocation cppCompiler,
    OutputLocationCollection builtInIncludeDirectories,
    @Nullable OutputLocation sysroot,
    Map<String, String> cEnvironment,
    Map<String, String> cppEnvironment,
    @Nullable XcodeInfo xcodeInfo
) implements BuildTargetData {

    public record XcodeInfo(
        String developerDir,
        String sdkRoot,
        String xcodeVersion
    ) {
    }
}
