package org.jetbrains.bazel.clion.workspace;

import consulo.process.cmd.GeneralCommandLine;
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// consulo: upstream Kotlin top-level declarations of buildCompilerSettings.kt; the data class CcCompilerInfo is in
// CcCompilerInfo.java
public final class buildCompilerSettings {
    private buildCompilerSettings() {
    }

    private static final String PROC_SELF_CWD = "/proc/self/cwd";

    // consulo: upstream `context(ctx: CcImportContext) suspend fun buildCompilerSettings()`; the context parameter is the
    // first parameter, the coroutine scope of the Xcode locator is replaced by its futures
    public static CompletableFuture<Map<WorkspaceTargetKey, CcCompilerInfo>> buildCompilerSettings(CcImportContext ctx) {
        Map<WorkspaceTargetKey, CcCompilerInfo> result = new LinkedHashMap<>();

        CcCompilerResolver compilerResolver = new CcCompilerResolver(ctx);
        CcXcodeLocator xcodeLocator = new CcXcodeLocator(ctx);

        // consulo: the loop awaits the Xcode of every toolchain in turn, as upstream does
        CompletableFuture<Void> loop = CompletableFuture.completedFuture(null);
        for (BuildTarget target : ctx.getSnapshot().targets().allTargets().toList()) {
            CcToolchainBuildTarget toolchainInfo = BuildTarget.extractData(target, CcToolchainBuildTarget.class);
            if (toolchainInfo == null) {
                continue;
            }

            loop = loop.thenCompose(ignored -> xcodeLocator.resolve(toolchainInfo.xcodeInfo())).thenAccept(xcode -> {
                Map<String, String> environment = createEnvironment(
                    ctx,
                    toolchainInfo.cEnvironment(),
                    toolchainInfo.cppEnvironment(),
                    toMap(xcode)
                );

                CcToolEnvironment toolEnvironment = createToolEnvironment(environment);

                CcCompilerResolver.Result cCompiler = compilerResolver.resolve(toolchainInfo.cCompiler(), toolEnvironment);
                if (cCompiler == null) {
                    return;
                }
                CcCompilerResolver.Result cppCompiler = compilerResolver.resolve(toolchainInfo.cppCompiler(), toolEnvironment);
                if (cppCompiler == null) {
                    return;
                }

                CcCompilerInfo compilerInfo = new CcCompilerInfo(
                    cCompiler.path(),
                    cCompiler.kind(),
                    cppCompiler.path(),
                    cppCompiler.kind(),
                    toolchainInfo.cOption().asList(),
                    toolchainInfo.cppOption().asList(),
                    toolchainInfo.compilerName(),
                    environment,
                    toolchainInfo.builtInIncludeDirectories(),
                    toolchainInfo.sysroot()
                );

                result.put(target.getKey(), compilerInfo);
            });
        }

        return loop.thenCompose(ignored -> {
            compilerResolver.reportProblems();
            return xcodeLocator.reportProblems();
        }).thenApply(ignored -> result);
    }

    @SafeVarargs
    private static Map<String, String> createEnvironment(CcImportContext ctx, Map<String, String>... environments) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (Map<String, String> environment : environments) {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                merged.put(entry.getKey(), resolveProcSelfCwd(ctx, entry.getValue()));
            }
        }

        return merged;
    }

    private static String resolveProcSelfCwd(CcImportContext ctx, String value) {
        if (!value.startsWith(PROC_SELF_CWD)) {
            return value;
        }

        Path resolved = utils.resolveExecutionRootPath(ctx, value);
        if (resolved == null) {
            return value;
        }
        return resolved.toString();
    }

    static CcToolEnvironment createToolEnvironment(Map<String, String> environment) {
        // consulo: upstream returns a plain CidrToolEnvironment for an empty environment
        return new CcToolEnvironment(environment);
    }

    private static Map<String, String> toMap(CcXcodeLocator.@Nullable Result receiver) {
        if (receiver == null) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("DEVELOPER_DIR", receiver.developerDir().toString());
        result.put("SDKROOT", receiver.sdkRoot().toString());
        return result;
    }
}

/**
 * TODO consulo: CIDR {@code CidrToolEnvironment} (the environment the compiler resolver and the compiler info
 * collection run the compiler in) does not exist in consulo-cpp; this class only adds the toolchain environment to a
 * command line.
 */
class CcToolEnvironment {
    private final Map<String, String> myEnvironment;

    CcToolEnvironment(Map<String, String> environment) {
        myEnvironment = environment;
    }

    Map<String, String> getEnvironment() {
        return myEnvironment;
    }

    void prepare(GeneralCommandLine commandLine) {
        // TODO consulo: super.prepare(commandLine, prepareFor) of CidrToolEnvironment
        commandLine.getEnvironment().putAll(myEnvironment);
    }
}
