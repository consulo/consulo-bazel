package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.localize.BazelCLionCommonLocalize;
import consulo.build.ui.event.MessageEvent;
import consulo.platform.Platform;
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bsp.protocol.RunParams;
import org.jetbrains.bsp.protocol.RunResult;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// consulo: upstream takes the CoroutineScope the lookups run in; the cached futures replace it
class CcXcodeLocator {
    private static final Label XCODE_LOCATOR = Label.parse("@bazel_tools//tools/osx:xcode-locator");

    private static final String NO_CONVENIENCE_SYMLINKS = "--experimental_convenience_symlinks=ignore";

    private final CcImportContext myCtx;

    CcXcodeLocator(CcImportContext ctx) {
        myCtx = ctx;
    }

    record Result(Path developerDir, Path sdkRoot) {
    }

    private final Map<CcToolchainBuildTarget.XcodeInfo, CompletableFuture<@Nullable Result>> myCache = new LinkedHashMap<>();

    CompletableFuture<@Nullable Result> resolve(CcToolchainBuildTarget.@Nullable XcodeInfo info) {
        if (!Platform.current().os().isMac() || info == null || info.xcodeVersion().isEmpty() || info.macosSdkVersion().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return myCache.computeIfAbsent(info, this::doResolve);
    }

    private CompletableFuture<@Nullable Result> doResolve(CcToolchainBuildTarget.XcodeInfo info) {
        return locateDeveloperDir(info).thenApply(developerDir -> {
            if (developerDir == null) {
                return null;
            }
            String sdkVersion = "MacOSX" + info.macosSdkVersion() + ".sdk";

            return new Result(
                developerDir,
                developerDir.resolve("Platforms").resolve("MacOSX.platform").resolve("Developer").resolve("SDKs").resolve(sdkVersion)
            );
        });
    }

    private CompletableFuture<@Nullable Path> locateDeveloperDir(CcToolchainBuildTarget.XcodeInfo info) {
        CompletableFuture<RunResult> result = BazelServerConnection.getConnection(myCtx.getProject()).runWithServer(server -> {
            RunParams params = new RunParams(
                myCtx.getTaskId(),
                XCODE_LOCATOR,
                true,
                List.of(info.xcodeVersion()),
                Map.of(),
                List.of(NO_CONVENIENCE_SYMLINKS),
                null,
                true
            );

            return server.buildTargetRun(params);
        });

        return result.thenApply(it -> {
            if (it.statusCode() != BazelStatus.SUCCESS) {
                return null;
            }
            String path = it.stdout();
            if (path == null) {
                return null;
            }

            try {
                return Path.of(path.trim());
            }
            catch (InvalidPathException e) {
                return null;
            }
        });
    }

    CompletableFuture<Void> reportProblems() {
        List<Map.Entry<CcToolchainBuildTarget.XcodeInfo, CompletableFuture<@Nullable Result>>> entries =
            new ArrayList<>(myCache.entrySet());
        CompletableFuture<?>[] futures = entries.stream().map(Map.Entry::getValue).toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).thenAccept(ignored -> {
            List<String> problems = new ArrayList<>();
            for (Map.Entry<CcToolchainBuildTarget.XcodeInfo, CompletableFuture<@Nullable Result>> entry : entries) {
                if (entry.getValue().join() != null) {
                    continue;
                }
                CcToolchainBuildTarget.XcodeInfo info = entry.getKey();
                problems.add(BazelCLionCommonLocalize.ccXcodeLocateFailed(info.xcodeVersion(), info.macosSdkVersion()).get());
            }

            if (problems.isEmpty()) {
                return;
            }

            myCtx.reportEvent(
                MessageEvent.Kind.WARNING,
                BazelCLionCommonLocalize.ccXcodeResolveFailed(),
                String.join("\n", problems)
            );
        });
    }
}
