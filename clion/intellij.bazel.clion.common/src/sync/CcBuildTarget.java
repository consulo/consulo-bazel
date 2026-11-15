package org.jetbrains.bazel.clion.sync;

import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

public record CcBuildTarget(@Nullable RuleContext ruleContext, CompilationContext compilationContext) implements BuildTargetData {

    /**
     * Information collected from rule attributes directly; may not be present
     * for custom rules, since the semantic of their rule attributes is unknown.
     */
    public record RuleContext(
        OutputLocationCollection headers,
        OutputLocationCollection textualHeaders,
        StringsCollection copts,
        StringsCollection conlyopts,
        StringsCollection cxxopts,
        StringsCollection args,
        String includePrefix,
        String stripIncludePrefix
    ) {
    }

    /**
     * Information collected from the CcInfoProvider; should always be present.
     */
    public record CompilationContext(
        OutputLocationCollection headers,
        StringsCollection defines,
        OutputLocationCollection includes,
        OutputLocationCollection quoteIncludes,
        OutputLocationCollection systemIncludes
    ) {
    }
}
