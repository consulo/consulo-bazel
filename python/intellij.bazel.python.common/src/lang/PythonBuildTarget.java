package org.jetbrains.bazel.python.lang;

import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

/**
 * @param imports          imports is the attribute in bazel python rules
 *                         which specify a list of runfiles relative paths which will be included in PYTHONPATH
 * @param generatedSources Not used after full sync. Mapping is saved in {@code PythonResolveIndexService}
 */
public record PythonBuildTarget(
    @Nullable String version,
    @Nullable OutputLocation interpreter,
    StringsCollection imports,
    OutputLocationCollection generatedSources,
    OutputLocationCollection externalSources,
    @Nullable OutputLocation mainFile,
    @Nullable String mainModule,
    @Nullable Path runnerScript,
    StringsCollection targetArgs,
    Map<OutputLocation, OutputLocation> generatedSourceOverrides
) implements BuildTargetData {
    // consulo: overload for the upstream default arguments
    public PythonBuildTarget(@Nullable String version, @Nullable OutputLocation interpreter, StringsCollection imports) {
        this(
            version,
            interpreter,
            imports,
            OutputLocationCollection.EMPTY,
            OutputLocationCollection.EMPTY,
            null,
            null,
            null,
            StringsCollection.EMPTY,
            Map.of()
        );
    }

    // consulo: upstream top-level function of PythonBuildTarget.kt
    public static @Nullable PythonBuildTarget extractPythonBuildTarget(BuildTarget target) {
        return BuildTarget.extractData(target, PythonBuildTarget.class);
    }
}
