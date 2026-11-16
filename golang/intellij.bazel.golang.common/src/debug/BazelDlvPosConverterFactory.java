package org.jetbrains.bazel.golang.debug;

import com.goide.sdk.GoSdkService;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.golang.sync.GoLanguagePlugin;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams;
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams.BazelResolveRemoteToLocalParams;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO consulo: not registered (upstream com.goide dlv.positionConverterFactory, order="first"). The Delve debugger of
//  consulo-google-go (GoDlvDebugProcess, a consulo-native-dev DAP debug process) has no position converter extension point like
//  com.goide.dlv.location.DlvPositionConverterFactory / DlvPositionConverter: Delve maps the paths itself by the prefix rules of its
//  `substitutePath` launch argument, which BazelDlvDebuggerSetup derives from this algorithm. Kept as the reference of the
//  per-path resolution (GoLanguagePlugin.resolveRemoteToLocal / resolveLocalToRemote) until such an API exists
public class BazelDlvPosConverterFactory /* : DlvPositionConverterFactory */ {
    public @Nullable BspDlvPositionConverter createPositionConverter(
        Project project,
        @Nullable Module module,
        Set<String> remotePaths
    ) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }
        // consulo: GoSdkService.getSdk(module).homePath -> GoSdkService.getSdkHomePath(module)
        String goRoot = GoSdkService.getInstance(project).getSdkHomePath(module);
        return new BspDlvPositionConverter(project, remotePaths, goRoot != null ? goRoot : "");
    }
}

class BspDlvPositionConverter /* : DlvPositionConverter */ {
    // consulo: upstream private top-level val of BazelDlvPosConverterFactory.kt
    private static final Logger logger = Logger.getInstance(BspDlvPositionConverter.class);

    private final Project myProject;
    private final Set<String> myRemotePaths;
    private final String myGoRoot;

    private final Map<String, String> myLocalToRemoteCache = new HashMap<>();
    private final Map<String, VirtualFile> myRemoteToLocalCache = new HashMap<>();

    BspDlvPositionConverter(
        Project project,
        Set<String> remotePaths,
        String goRoot
    ) {
        myProject = project;
        myRemotePaths = remotePaths;
        myGoRoot = goRoot;

        if (!myRemotePaths.isEmpty()) {
            Map<String, Path> resolvedMap = resolveRemoteToLocalOnServer(List.copyOf(myRemotePaths));
            resolvedMap.forEach((remote, localPath) -> {
                VirtualFile vf = VirtualFileManager.getInstance().findFileByNioPath(localPath);
                if (vf != null && vf.isValid()) {
                    myRemoteToLocalCache.put(remote, vf);
                }
            });
        }
    }

    /**
     * Converts a local file path (IDE) to a remote debugger path.
     * Checks cache first or makes a server call if not found.
     * Saves the result in the cache.
     */
    public String toRemotePath(VirtualFile localFile) {
        String localPath = localFile.getPath();

        String cached = myLocalToRemoteCache.get(localPath);
        if (cached != null) {
            return cached;
        }

        Map<String, String> resolvedMap = resolveLocalToRemoteOnServer(List.of(localPath));

        String remotePath = resolvedMap.get(localPath);
        if (remotePath == null) {
            logger.warn("Server could not resolve local path '" + localPath + "' to remote path. Using as-is.");
            remotePath = localPath;
        }

        // Cache the result
        myLocalToRemoteCache.put(localPath, remotePath);
        return remotePath;
    }

    /**
     * Converts a remote debugger path to a local VirtualFile.
     * Checks cache first or makes a server call if not found.
     * Saves the result in the cache.
     */
    public @Nullable VirtualFile toLocalFile(String remotePath) {
        VirtualFile cachedVf = myRemoteToLocalCache.get(remotePath);
        if (cachedVf != null) {
            if (cachedVf.isValid()) {
                return cachedVf;
            }
        }

        Map<String, Path> resolvedMap = resolveRemoteToLocalOnServer(List.of(remotePath));

        Path localAbsolute = resolvedMap.get(remotePath);
        if (localAbsolute == null) {
            logger.warn("Server could not resolve remote path '" + remotePath + "' to local path.");
            return null;
        }

        VirtualFile vf = VirtualFileManager.getInstance().findFileByNioPath(localAbsolute);
        if (vf != null && vf.isValid()) {
            myRemoteToLocalCache.put(remotePath, vf);
            return vf;
        }
        return null;
    }

    private Map<String, String> resolveLocalToRemoteOnServer(List<String> localPaths) {
        BazelResolveLocalToRemoteParams params =
            new BazelResolveLocalToRemoteParams(
                localPaths
            );

        BazelInfo bazelInfo = myProject.getInstance(WorkspaceSnapshotService.class).getSnapshot().get().bazelInfo();
        BazelPathsResolver bazelPathsResolver = new BazelPathsResolver(bazelInfo);
        return GoLanguagePlugin
            .resolveLocalToRemote(bazelPathsResolver, params)
            .resolvedPaths();
    }

    private Map<String, Path> resolveRemoteToLocalOnServer(List<String> remotePaths) {
        BazelResolveRemoteToLocalParams params =
            new BazelResolveRemoteToLocalParams(
                remotePaths,
                myGoRoot
            );

        BazelInfo bazelInfo = myProject.getInstance(WorkspaceSnapshotService.class).getSnapshot().get().bazelInfo();
        BazelPathsResolver bazelPathsResolver = new BazelPathsResolver(bazelInfo);
        return GoLanguagePlugin
            .resolveRemoteToLocal(bazelPathsResolver, params)
            .resolvedPaths();
    }
}
