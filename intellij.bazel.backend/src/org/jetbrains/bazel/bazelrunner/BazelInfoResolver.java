package org.jetbrains.bazel.bazelrunner;

import consulo.logging.Logger;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.server.sync.configuration;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BazelInfoResolver {
    // consulo: upstream private top-level constants of BazelInfoResolver.kt
    private static final String RELEASE = "release";
    private static final String EXECUTION_ROOT = "execution_root";
    private static final String OUTPUT_BASE = "output_base";
    private static final String WORKSPACE = "workspace";
    private static final String BAZEL_BIN = "bazel-bin";
    private static final String STARLARK_SEMANTICS = "starlark-semantics";

    private final Path myWorkspaceRoot;

    public BazelInfoResolver(Path workspaceRoot) {
        myWorkspaceRoot = workspaceRoot;
    }

    public Path getWorkspaceRoot() {
        return myWorkspaceRoot;
    }

    public CompletableFuture<BazelInfo> resolveBazelInfo(BazelRunner bazelRunner, ProjectView projectView, @Nullable TaskId taskId) {
        BazelCommand command =
            bazelRunner.buildBazelCommand(projectView, builder ->
                builder.info(info -> {
                    info.getOptions().addAll(List.of(RELEASE, EXECUTION_ROOT, OUTPUT_BASE, WORKSPACE, BAZEL_BIN, STARLARK_SEMANTICS));
                })
            );
        return bazelRunner
            .runBazelCommand(command, taskId)
            .waitAndGetResult()
            .thenApply(processResult -> {
                if (processResult.isNotSuccess()) {
                    throw new IllegalStateException(
                        "Querying bazel info failed.\n" +
                            "stdout: " + String.join("\n", processResult.getStdoutLines()) + "\n" +
                            "stderr: " + String.join("\n", processResult.getStderrLines())
                    );
                }
                return parseBazelInfo(processResult.getStdoutLines(), processResult.getStderrLines());
            });
    }

    private BazelInfo parseBazelInfo(List<String> stdoutLines, List<String> stderrLines) {
        Map<String, String> outputMap = new HashMap<>();
        for (String line : stdoutLines) {
            Matcher matcher = INFO_LINE_PATTERN.matcher(line);
            if (matcher.matches()) {
                outputMap.put(matcher.group(1), matcher.group(2));
            }
        }

        Function<String, String> extract = name -> {
            @Nullable String value = outputMap.get(name);
            if (value == null) {
                throw new IllegalStateException(
                    "Failed to resolve " + name + " from bazel info in " + myWorkspaceRoot + ". " +
                        "Bazel Info output: '" + String.join("\n", stderrLines) + "'"
                );
            }
            return value;
        };

        BazelRelease bazelReleaseVersion = BazelInfo.orFallbackVersion(
            BazelInfo.orFromBazelVersionFile(BazelRelease.fromReleaseString(extract.apply(RELEASE)), myWorkspaceRoot)
        );

        Path reportedWorkspaceRoot = Paths.get(extract.apply(WORKSPACE));
        if (!reportedWorkspaceRoot.equals(myWorkspaceRoot)) {
            LOG.error("Workspace path mismatch: given " + myWorkspaceRoot + ", Bazel reported " + reportedWorkspaceRoot);
        }

        StarlarkSemantics starlarkSemantics = parseStarlarkSemantics(extract.apply(STARLARK_SEMANTICS), bazelReleaseVersion);

        return new BazelInfo(
            Paths.get(extract.apply(EXECUTION_ROOT)),
            Paths.get(extract.apply(OUTPUT_BASE)),
            myWorkspaceRoot,
            Paths.get(extract.apply(BAZEL_BIN)),
            bazelReleaseVersion,
            starlarkSemantics.isBzlModEnabled(),
            starlarkSemantics.isWorkspaceEnabled(),
            starlarkSemantics.externalAutoloads(),
            configuration.isConfigurationSupportEnabled(bazelReleaseVersion)
        );
    }

    private record StarlarkSemantics(
        boolean isBzlModEnabled,
        boolean isWorkspaceEnabled,
        List<String> externalAutoloads
    ) {
    }

    // Idea taken from https://github.com/bazelbuild/bazel/issues/21303#issuecomment-2007628330
    private StarlarkSemantics parseStarlarkSemantics(String starlarkSemantics, BazelRelease bazelReleaseVersion) {
        boolean isBzlModEnabled;
        if (starlarkSemantics.contains("enable_bzlmod=true")) {
            isBzlModEnabled = true;
        }
        else if (starlarkSemantics.contains("enable_bzlmod=false")) {
            isBzlModEnabled = false;
        }
        else {
            isBzlModEnabled = bazelReleaseVersion.major() >= 7;
        }
        boolean isWorkspaceEnabled;
        if (starlarkSemantics.contains("enable_workspace=true")) {
            isWorkspaceEnabled = true;
        }
        else if (starlarkSemantics.contains("enable_workspace=false")) {
            isWorkspaceEnabled = false;
        }
        else {
            isWorkspaceEnabled = bazelReleaseVersion.major() <= 7;
        }
        boolean autoloadsDisabled = (
            (starlarkSemantics.contains("incompatible_disable_autoloads_in_main_repo=true"))
                || ((bazelReleaseVersion.major() >= 9) && (!starlarkSemantics.contains("incompatible_disable_autoloads_in_main_repo=false")))
        );
        // https://github.com/bazelbuild/bazel/issues/23043
        // https://bazel.build/reference/command-line-reference#flag--incompatible_autoload_externally
        List<String> externalAutoloads;
        if (autoloadsDisabled) {
            externalAutoloads = List.of();
        }
        else {
            @Nullable List<String> parsedExternalAutoloads = parseExternalAutoloads(starlarkSemantics);
            if (parsedExternalAutoloads != null) {
                externalAutoloads = parsedExternalAutoloads;
            }
            else if (bazelReleaseVersion.major() == 8) {
                // Bazel 8 autoloads several rules by default to ease migration.
                // This can be turned off via e.g. --incompatible_autoload_externally=""
                externalAutoloads = List.of("rules_python", "rules_java", "rules_android");
            }
            else {
                // Bazel 9 and higher will not autoload rules by default; Bazel 7 had bundled rules instead of autoloading.
                externalAutoloads = List.of();
            }
        }

        return new StarlarkSemantics(
            isBzlModEnabled,
            isWorkspaceEnabled,
            externalAutoloads
        );
    }

    private @Nullable List<String> parseExternalAutoloads(String starlarkSemantics) {
        int paramBegin = starlarkSemantics.indexOf(INCOMPATIBLE_AUTOLOADS_PARAM);
        if (paramBegin == -1) {
            return null;
        }
        int autoloadsBegin = paramBegin + INCOMPATIBLE_AUTOLOADS_PARAM.length();
        int autoloadsEnd = starlarkSemantics.indexOf(']', autoloadsBegin);
        if (autoloadsEnd == -1) {
            return null;
        }
        String autoloadsText = starlarkSemantics.substring(autoloadsBegin, autoloadsEnd);

        return Arrays.stream(autoloadsText.split(",", -1))
            .map(it -> removePrefix(removePrefix(it.trim(), "+"), "@"))
            .filter(it -> !it.isEmpty())
            .toList();
    }

    // consulo: Kotlin String.removePrefix
    private static String removePrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static final Pattern INFO_LINE_PATTERN = Pattern.compile("([\\w-]+): (.*)");
    private static final String INCOMPATIBLE_AUTOLOADS_PARAM = "incompatible_autoload_externally=[";

    private static final Logger LOG = Logger.getInstance(BazelInfoResolver.class);
}
