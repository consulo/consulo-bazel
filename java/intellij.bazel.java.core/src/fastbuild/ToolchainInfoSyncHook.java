package org.jetbrains.bazel.fastbuild;

import consulo.annotation.component.ExtensionImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.sync.ProjectSyncHook;

import java.util.concurrent.CompletableFuture;

// consulo: the nested upstream service ToolchainInfoSyncHook.JvmToolchainInfoService has its own file (nested @ServiceImpl
// classes are not supported by the annotation processor), see JvmToolchainInfoService
@ExtensionImpl
public class ToolchainInfoSyncHook implements ProjectSyncHook {
    @Override
    public boolean isEnabled(Project project) {
        return BazelFeatureFlags.getFastBuildEnabled();
    }

    @Override
    public CompletableFuture<Void> onSync(ProjectSyncHook.ProjectSyncHookEnvironment environment) {
        JvmToolchainInfoService service = JvmToolchainInfoService.getInstance(environment.project());

        // clear per-target cache to ensure fresh toolchain info after sync
        // this is important in case javac configuration has changed
        logger.info("Clearing previously saved toolchains");
        service.clearPerTargetCache();
        return CompletableFuture.completedFuture(null);
    }

    static final Logger logger = Logger.getInstance(ToolchainInfoSyncHook.class);
}
