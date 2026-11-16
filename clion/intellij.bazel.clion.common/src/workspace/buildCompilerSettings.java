package org.jetbrains.bazel.clion.workspace;

import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget;
import org.jetbrains.bazel.clion.workspace.environment.BazelEnvironmentProvider;
import org.jetbrains.bazel.clion.workspace.environment.BazelEnvironmentProvider.CcToolEnvironment;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// consulo: upstream Kotlin top-level declarations of buildCompilerSettings.kt; the data class CcCompilerInfo is in
// CcCompilerInfo.java
public final class buildCompilerSettings {
    private buildCompilerSettings() {
    }

    private static final String PROC_SELF_CWD = "/proc/self/cwd";

    // placeholders that the apple_support compiler wrapper replaces
    private static final String EXECUTION_ROOT_PLACEHOLDER = "__BAZEL_EXECUTION_ROOT__";
    private static final String EXECUTION_ROOT_CANONICAL_PLACEHOLDER = "__BAZEL_EXECUTION_ROOT_CANONICAL__";
    private static final String XCODE_DEVELOPER_DIR_PLACEHOLDER = "__BAZEL_XCODE_DEVELOPER_DIR__";
    private static final String XCODE_SDKROOT_PLACEHOLDER = "__BAZEL_XCODE_SDKROOT__";

    // consulo: upstream `context(ctx: CcImportContext) suspend fun buildCompilerSettings()`; the context parameter is the
    // first parameter. Nothing suspends any more, the future is completed
    public static CompletableFuture<Map<WorkspaceTargetKey, CcCompilerInfo>> buildCompilerSettings(CcImportContext ctx) {
        Map<WorkspaceTargetKey, CcCompilerInfo> result = new LinkedHashMap<>();

        CcCompilerResolver compilerResolver = new CcCompilerResolver(ctx);
        Map<String, String> executionRootPlaceholders = createExecutionRootPlaceholders(ctx);

        for (BuildTarget target : ctx.getSnapshot().targets().allTargets().toList()) {
            CcToolchainBuildTarget toolchainInfo = BuildTarget.extractData(target, CcToolchainBuildTarget.class);
            if (toolchainInfo == null) {
                continue;
            }
            Map<String, String> placeholders = new LinkedHashMap<>(executionRootPlaceholders);
            placeholders.putAll(createXcodePlaceholders(toolchainInfo.xcodeInfo()));

            Map<String, String> environment = createEnvironment(
                ctx,
                toolchainInfo.cEnvironment(),
                toolchainInfo.cppEnvironment(),
                createXcodeEnvironment(toolchainInfo)
            );

            CcToolEnvironment toolEnvironment = BazelEnvironmentProvider.createToolEnvironment(environment);

            CcCompilerResolver.Result cCompiler = compilerResolver.resolve(toolchainInfo.cCompiler(), toolEnvironment);
            if (cCompiler == null) {
                continue;
            }
            CcCompilerResolver.Result cppCompiler = compilerResolver.resolve(toolchainInfo.cppCompiler(), toolEnvironment);
            if (cppCompiler == null) {
                continue;
            }

            CcCompilerInfo compilerInfo = new CcCompilerInfo(
                cCompiler.path(),
                cCompiler.kind(),
                cppCompiler.path(),
                cppCompiler.kind(),
                expandPlaceholders(toolchainInfo.cOption().asList(), placeholders),
                expandPlaceholders(toolchainInfo.cppOption().asList(), placeholders),
                toolchainInfo.compilerName(),
                environment,
                toolchainInfo.builtInIncludeDirectories(),
                toolchainInfo.sysroot()
            );

            result.put(target.getKey(), compilerInfo);
        }

        compilerResolver.reportProblems();

        return CompletableFuture.completedFuture(result);
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

    private static Map<String, String> createXcodeEnvironment(CcToolchainBuildTarget info) {
        CcToolchainBuildTarget.@Nullable XcodeInfo xcodeInfo = info.xcodeInfo();
        if (xcodeInfo == null) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("DEVELOPER_DIR", xcodeInfo.developerDir());
        result.put("SDKROOT", xcodeInfo.sdkRoot());
        return result;
    }

    // consulo: upstream `context(ctx: CcImportContext)`, the context parameter is the first parameter
    private static Map<String, String> createExecutionRootPlaceholders(CcImportContext ctx) {
        Path canonical;
        try {
            canonical = ctx.getExecroot().toRealPath();
        }
        catch (IOException e) {
            canonical = ctx.getExecroot();
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put(EXECUTION_ROOT_PLACEHOLDER, ctx.getExecroot().toString());
        result.put(EXECUTION_ROOT_CANONICAL_PLACEHOLDER, canonical.toString());
        return result;
    }

    private static Map<String, String> createXcodePlaceholders(CcToolchainBuildTarget.@Nullable XcodeInfo info) {
        if (info == null) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put(XCODE_DEVELOPER_DIR_PLACEHOLDER, info.developerDir());
        result.put(XCODE_SDKROOT_PLACEHOLDER, info.sdkRoot());
        return result;
    }

    /**
     * Replaces each placeholder in the options. A placeholder can be a part of an option.
     */
    private static List<String> expandPlaceholders(List<String> options, Map<String, String> placeholders) {
        List<String> result = new ArrayList<>(options.size());
        for (String option : options) {
            String expanded = option;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                expanded = expanded.replace(entry.getKey(), entry.getValue());
            }
            result.add(expanded);
        }
        return result;
    }
}
