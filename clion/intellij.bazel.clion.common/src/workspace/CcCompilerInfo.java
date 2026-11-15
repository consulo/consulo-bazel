package org.jetbrains.bazel.clion.workspace;

import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

// consulo: upstream declares this data class in buildCompilerSettings.kt; a public Java type needs a file of its own
public record CcCompilerInfo(
    Path cCompiler,
    CcCompilerKind cCompilerKind,
    Path cppCompiler,
    CcCompilerKind cppCompilerKind,
    List<String> cSwitches,
    List<String> cppSwitches,
    String name,
    Map<String, String> environment,
    OutputLocationCollection builtinIncludes,
    @Nullable OutputLocation sysroot
) {

    // TODO: create specialized environment for clang-cl and MSVC
    // consulo: upstream `val toolEnvironment: CidrToolEnvironment by lazy`; a record has no lazy state, the environment
    // is created on every call (cheap, it only keeps the map)
    CcToolEnvironment toolEnvironment() {
        return buildCompilerSettings.createToolEnvironment(environment);
    }
}
