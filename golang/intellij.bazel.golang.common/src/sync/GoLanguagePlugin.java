package org.jetbrains.bazel.golang.sync;

import com.google.devtools.intellij.aspect.Common;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.annotation.component.ExtensionImpl;
import consulo.logging.Logger;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.golang.GoLanguageClassProvider.GoLanguageClass;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelper;
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams;
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams.BazelResolveLocalToRemoteResult;
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams.BazelResolveRemoteToLocalParams;
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams.BazelResolveRemoteToLocalResult;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class GoLanguagePlugin implements LanguagePlugin {
    @Override
    public Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes() {
        return Set.of(GoBuildTarget.class);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguages() {
        return Set.of(GoLanguageClass.GO);
    }

    @Override
    public List<LanguageClass> collectUsedLanguages(TargetIdeInfo target) {
        if (target.hasGoTargetInfo()) {
            return List.of(GoLanguageClass.GO);
        }
        return List.of();
    }

    @Override
    public CompletableFuture<List<BuildTargetData>> mapBuildTargetData(
        BazelServerFacade server,
        TargetIdeInfo target
    ) {
        if (!target.hasGoTargetInfo()) {
            return CompletableFuture.completedFuture(List.of());
        }
        IntellijIdeInfo.GoTargetInfo goTarget = target.getGoTargetInfo();
        // consulo: the suspend calls of upstream run sequentially, in the same order
        return calculateSdkPath(server, goTarget.getSdkHomePath()).thenCompose(sdkHomePath ->
            OutputLocationCollectionBuilder.build(goTarget.getSourcesList(), server.getOutputParser()).thenApply(sources ->
                List.<BuildTargetData>of(
                    new GoBuildTarget(
                        sdkHomePath,
                        goTarget.getImportPath(),
                        sources,
                        goTarget.getEmbedList().stream().map(WorkspaceHelper::toWorkspaceTargetKey).toList()
                    )
                )
            )
        );
    }

    // the aspect gives the `go` binary, the SDK home is two directories above it: `<home>/bin/go`
    private static CompletableFuture<@Nullable OutputLocation> calculateSdkPath(
        BazelServerFacade server,
        Common.@Nullable ArtifactLocation sdk
    ) {
        if (sdk == null || sdk.getRelativePath().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<String> segments = Arrays.asList(sdk.getRelativePath().split("/", -1));
        String homePath = String.join("/", segments.subList(0, Math.max(segments.size() - 2, 0)));
        return server.getOutputParser().parse(sdk.toBuilder().setRelativePath(homePath).build()).thenApply(it -> it);
    }

    private static final Logger logger = Logger.getInstance(GoLanguagePlugin.class);

    /**
     * Converts local absolute paths to remote Bazel paths.
     */
    public static BazelResolveLocalToRemoteResult resolveLocalToRemote(
        BazelPathsResolver pathsResolver,
        BazelResolveLocalToRemoteParams params
    ) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String local : params.localPaths()) {
            try {
                Path localAbs = Paths.get(local).toAbsolutePath().normalize();
                Path workspaceRoot = pathsResolver.workspaceRoot();
                if (localAbs.startsWith(workspaceRoot)) {
                    // If inside the main workspace, get a relative Bazel path:
                    String relative = pathsResolver.getWorkspaceRelativePath(localAbs);
                    mapping.put(local, relative);
                }
                else {
                    // If outside, keep absolute path (but unify slashes):
                    mapping.put(local, localAbs.toString().replace("\\", "/"));
                }
            }
            catch (Exception e) {
                logger.warn("Failed to resolve local path '" + local + "': " + e.getMessage(), e);
                mapping.put(local, local);
            }
        }
        return new BazelResolveLocalToRemoteResult(mapping);
    }

    /**
     * Converts remote Bazel paths to local absolute paths.
     */
    public static BazelResolveRemoteToLocalResult resolveRemoteToLocal(
        BazelPathsResolver pathsResolver,
        BazelResolveRemoteToLocalParams params
    ) {
        Map<String, Path> mapping = new LinkedHashMap<>();
        for (String remote : params.remotePaths()) {
            try {
                Path normalizedRemotePath = Path.of(normalizeRemotePath(remote, params.goRoot()));
                Path localPath = pathsResolver.resolve(normalizedRemotePath).toAbsolutePath().normalize();
                mapping.put(remote, localPath);
            }
            catch (Exception e) {
                logger.warn("Failed to resolve remote path '" + remote + "': " + e.getMessage(), e);
            }
        }
        return new BazelResolveRemoteToLocalResult(mapping);
    }

    private static String normalizeRemotePath(String path, String goRoot) {
        if (path.startsWith("/build/work/")) {
            return afterNthSlash(path, 5);
        }
        else if (path.startsWith("/tmp/go-build-release/buildroot/")) {
            return afterNthSlash(path, 4);
        }
        else if (path.startsWith("GOROOT/")) {
            String suffix = afterNthSlash(path, 1);
            return (goRoot.endsWith("/") ? goRoot.substring(0, goRoot.length() - 1) : goRoot) + "/" + suffix;
        }
        else {
            return path;
        }
    }

    /**
     * Returns the substring after the Nth slash.
     * If the path has fewer than N slashes, returns the original path.
     */
    private static String afterNthSlash(String path, int n) {
        int index = 0;
        for (int i = 0; i < n; i++) {
            index = path.indexOf('/', index) + 1;
            if (index == 0) {
                return path;
            }
        }
        return index < path.length() ? path.substring(index) : "";
    }
}
