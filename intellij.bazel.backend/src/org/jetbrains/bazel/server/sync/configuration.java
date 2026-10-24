package org.jetbrains.bazel.server.sync;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationSummary;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// consulo: holder class for the top-level declarations of upstream configuration.kt; the class keeps the upstream file name
public final class configuration {
    private configuration() {
    }

    // sentinel values from aspect
    public static final WorkspaceConfigurationId FALLBACK_CONFIG = WorkspaceConfigurationId.of("00000f1");
    public static final WorkspaceConfigurationId FALLBACK_EXEC_CONFIG = WorkspaceConfigurationId.of("00000f2");

    public static boolean isConfigurationSupportEnabled(BazelRelease receiver) {
        // this one is dictated by whenever bazel support `short_id` on `BuildConfigurationApi`
        int major = receiver.major();
        int minor = receiver.minor();
        if (major >= 9) {
            return true;
        }
        else if (major == 8 && minor >= 5) {
            return true;
        }
        else if (major == 7 && minor >= 7) {
            return true;
        }
        else {
            return false;
        }
    }

    // obtained through: `bazel config --dump_all --output=json`
    // consulo: kotlinx.serialization @Serializable -> Gson (record component names are the JSON keys)
    public record BazelConfiguration(
        String skyKey,
        String configHash,
        String mnemonic,
        boolean isExec,
        List<BazelConfigurationFragment> fragments,
        List<BazelConfigurationFragmentOptions> fragmentOptions
    ) {
    }

    public record BazelConfigurationFragment(
        String name,
        List<String> fragmentOptions
    ) {
    }

    public record BazelConfigurationFragmentOptions(
        String name,
        Map<String, String> options
    ) {
    }

    public static WorkspaceConfiguration toWorkspaceConfiguration(BazelConfiguration receiver) {
        Map<String, String> options = null;
        for (BazelConfigurationFragmentOptions it : receiver.fragmentOptions()) {
            if (it.name().contains("CoreOptions")) {
                options = it.options();
                break;
            }
        }
        if (options == null) {
            throw new IllegalStateException("`CoreOptions` options not found");
        }
        String cpu = options.get("cpu");
        if (cpu == null) {
            throw new IllegalStateException("missing `cpu` options");
        }
        String mode = options.get("compilation_mode");
        if (mode == null) {
            throw new IllegalStateException("missing `compilation_mode` option");
        }
        String platformSuffix = options.get("platform_suffix");

        StringBuilder platformNameBuilder = new StringBuilder();
        platformNameBuilder.append(cpu);
        platformNameBuilder.append("-");
        platformNameBuilder.append(mode);

        if (platformSuffix != null && !platformSuffix.isBlank() && !platformSuffix.equals("null")) {
            platformNameBuilder.append("-").append(platformSuffix);
        }
        String platformName = platformNameBuilder.toString();

        return new WorkspaceConfiguration(
            WorkspaceConfigurationId.of(receiver.configHash()),
            new WorkspaceConfigurationSummary(
                receiver.configHash(),
                receiver.mnemonic(),
                platformName,
                cpu,
                receiver.isExec()
            ),
            List.of() // can be filled by parsing options
        );
    }

    /**
     * consulo: upstream returns {@code Result<List<WorkspaceConfiguration>>}; here a failure of {@code runCatching} (the parsing)
     * completes the returned future exceptionally.
     */
    /**
     * consulo: upstream returns {@code Result<List<WorkspaceConfiguration>>} which only captures the parse failure;
     * a failure to run the command propagates (the future completes exceptionally).
     */
    public record ConfigurationsResult(@Nullable List<WorkspaceConfiguration> configurations, @Nullable Throwable failure) {
    }

    public static CompletableFuture<ConfigurationsResult> fetchConfigurationsFromAnalysisCache(
        ProjectView projectView,
        BazelRunner runner,
        @Nullable TaskId taskId
    ) {
        BazelCommand command = runner.buildBazelCommand(projectView, builder ->
            builder.config(config -> {
                config.getOptions().addAll(List.of("--dump_all", "--output=json"));
            })
        );
        return runner.runBazelCommand(command, taskId)
            .waitAndGetResult()
            .thenApply(result -> {
                try {
                    String json = new String(result.getStdout(), StandardCharsets.UTF_8);
                    Type type = TypeToken.getParameterized(List.class, BazelConfiguration.class).getType();
                    List<BazelConfiguration> configurations = new Gson().fromJson(json, type);
                    return new ConfigurationsResult(
                        configurations
                            .stream()
                            .map(configuration::toWorkspaceConfiguration)
                            .toList(),
                        null
                    );
                }
                catch (Throwable e) {
                    return new ConfigurationsResult(null, e);
                }
            });
    }
}
