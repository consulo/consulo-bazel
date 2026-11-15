package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.workspace.CcWorkspaceModel;

import java.util.List;
import java.util.Objects;

// consulo: upstream Kotlin top-level functions of collectCompilerInfo.kt
public final class collectCompilerInfo {
    private collectCompilerInfo() {
    }

    // consulo: upstream `context(ctx: CcImportContext) internal fun collectCompilerInfo(model, configurations)`
    public static void collectCompilerInfo(CcImportContext ctx, CcWorkspaceModel model, List<CcResolveConfiguration> configurations) {

        // TODO consulo: CIDR CompilerInfoCache runs the compiler of every configuration to collect its compiler info
        //  (predefined macros, builtin include directories, language standard). consulo-cpp has no compiler info
        //  collection, so the configurations are only checked here.
        //  // TODO: can we use a real indicator here?
        //  val session = CompilerInfoCache().createSession<String>(EmptyProgressIndicator())
        //  try {
        //    for (config in configurations) { ...session.schedule(identifier, configModel, config.shared.compilerSettings.toolEnvironment, ctx.execroot.toString()) }
        //    val messages = MultiMap<String, CompilerInfoCache.Message>()
        //    session.waitForAll(messages)
        //    reportProblems(messages)
        //  }
        //  finally {
        //    session.dispose()
        //  }
        for (CcResolveConfiguration config : configurations) {
            String identifier = buildEquivalenceClasses.encode(config.id());
            CcWorkspaceModel.Configuration configModel = Objects.requireNonNull(model.getConfigurationById(identifier));

            // TODO consulo: session.schedule(identifier, configModel, config.shared.compilerSettings.toolEnvironment, ctx.execroot.toString())
        }
    }

    // TODO consulo: reportProblems(problems: MultiMap<String, CompilerInfoCache.Message>) reports the messages of the CIDR
    //  compiler info collection, which consulo-cpp does not have:
    //  if (problems.isEmpty()) return
    //  val description = problems.entrySet().filter { it.value.isNotEmpty() }.joinToString("\n") { (configId, messages) ->
    //    messages.joinToString("\n") { problem -> "$configId: ${problem.type}: ${problem.text.trim()}" }
    //  }
    //  ctx.reportEvent(message = BazelCLionCommonLocalize.ccCompilerInfoFailed(problems.keySet().size), description = description,
    //    severity = MessageEvent.Kind.WARNING)
}
