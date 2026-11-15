package org.jetbrains.bazel.fastbuild;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bsp.protocol.JvmToolchainInfo;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.bazel.fastbuild.ToolchainInfoSyncHook.logger;

// consulo: upstream ToolchainInfoSyncHook.JvmToolchainInfoService (nested in ToolchainInfoSyncHook.kt); own file because nested
// @ServiceImpl classes are not supported by the annotation processor
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class JvmToolchainInfoService {
    private final Map<Label, JvmToolchainInfo> myPerTargetToolchainInfo = new HashMap<>();

    public @Nullable JvmToolchainInfo getJvmToolchainInfo(Label target) {
        return myPerTargetToolchainInfo.get(target);
    }

    public void setJvmToolchainInfo(JvmToolchainInfo toolchainInfo, Label target) {
        myPerTargetToolchainInfo.put(target, toolchainInfo);
    }

    public void clearPerTargetCache() {
        myPerTargetToolchainInfo.clear();
    }

    public JvmToolchainInfo getOrQueryJvmToolchainInfo(Project project, Label target) {
        // first try to get cached toolchain info
        JvmToolchainInfo cachedInfo = getJvmToolchainInfo(target);
        if (cachedInfo != null) {
            logger.info("Using cached toolchain for " + target.getTargetName());
            return cachedInfo;
        }

        // query the server for per-target toolchain info
        // consulo: upstream runBlocking { }
        JvmToolchainInfo toolchainInfo =
            BazelServerConnection.getConnection(project).runWithServer(server -> {
                logger.info("Fetching toolchain for " + target.getTargetName());
                return server.jvmToolchainInfoForTarget(target);
            }).join();

        // cache the result
        logger.info("Storing toolchain for " + target.getTargetName());
        setJvmToolchainInfo(toolchainInfo, target);
        return toolchainInfo;
    }

    public static JvmToolchainInfoService getInstance(Project project) {
        return project.getInstance(JvmToolchainInfoService.class);
    }
}
