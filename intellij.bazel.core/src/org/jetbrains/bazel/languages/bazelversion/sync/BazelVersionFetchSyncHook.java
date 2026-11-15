package org.jetbrains.bazel.languages.bazelversion.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral;
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService;
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionWorkspaceResolver;
import org.jetbrains.bazel.sync.ProjectPostSyncHook;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.jetbrains.bazel.config.project.getRootDir;

@ExtensionImpl
class BazelVersionFetchSyncHook implements ProjectPostSyncHook {
    public BazelVersionFetchSyncHook() {
    }

    @Override
    public CompletableFuture<Void> onPostSync(ProjectPostSyncHook.ProjectPostSyncHookEnvironment environment) {
        Path workspaceDir = getRootDir(environment.project()).toNioPath();
        BazelVersionLiteral currentVersion = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(workspaceDir);
        return environment.project()
            .getInstance(BazelVersionCheckerService.class)
            .refreshLatestBazelVersion(environment.project(), currentVersion);
    }
}
