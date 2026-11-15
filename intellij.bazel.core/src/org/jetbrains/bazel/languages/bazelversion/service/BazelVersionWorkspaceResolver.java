package org.jetbrains.bazel.languages.bazelversion.service;

import consulo.platform.Platform;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BazelVersionWorkspaceResolver {
    private BazelVersionWorkspaceResolver() {
    }

    public static @Nullable BazelVersionLiteral resolveBazelVersionFromWorkspace(Path workspace) {
        BazelVersionLiteral result = resolveBazelVersionEnvVariable();
        if (result == null) {
            result = resolveBazeliskRcVersion(workspace);
        }
        if (result == null) {
            result = resolveWorkspaceBazelVersionFile(workspace);
        }
        if (result == null) {
            result = resolveFallbackBazelVersion();
        }
        return result;
    }

    private static @Nullable BazelVersionLiteral resolveWorkspaceBazelVersionFile(Path workspace) {
        String text = readTextOrNull(workspace.resolve(Constants.BAZEL_VERSION_FILE_NAME));
        return text != null ? BazelVersionLiteral.toBazelVersionLiteral(text) : null;
    }

    // consulo: EnvironmentUtil.getValue(name) -> Platform.current().os().getEnvironmentVariable(name)
    private static @Nullable BazelVersionLiteral resolveBazelVersionEnvVariable() {
        String value = Platform.current().os().getEnvironmentVariable("USE_BAZEL_VERSION");
        return value != null ? BazelVersionLiteral.toBazelVersionLiteral(value) : null;
    }

    private static @Nullable BazelVersionLiteral resolveFallbackBazelVersion() {
        String fallbackVersion = Platform.current().os().getEnvironmentVariable("USE_BAZEL_FALLBACK_VERSION");
        if (fallbackVersion == null) {
            return null;
        }
        List<String> allowedPrefixes = List.of("error:", "warn:", "silent:");
        String prefix = allowedPrefixes.stream().filter(fallbackVersion::startsWith).findFirst().orElse(null);
        if (prefix == null) {
            return BazelVersionLiteral.toBazelVersionLiteral(fallbackVersion);
        }
        else {
            return BazelVersionLiteral.toBazelVersionLiteral(fallbackVersion.substring(prefix.length()));
        }
    }

    private static @Nullable BazelVersionLiteral resolveBazeliskRcVersion(Path workspace) {
        String text = readTextOrNull(workspace.resolve(Constants.BAZELISK_RC_FILE_NAME));
        if (text == null) {
            return null;
        }
        Map<String, String> properties = BazeliskrcParser.parse(text);
        String version = properties.get("USE_BAZEL_VERSION");
        if (version == null) {
            return null;
        }
        return BazelVersionLiteral.toBazelVersionLiteral(version);
    }

    private static @Nullable String readTextOrNull(Path receiver) {
        if (!Files.isRegularFile(receiver) || !Files.exists(receiver)) {
            return null;
        }
        try {
            return Files.readString(receiver);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    // https://github.com/bazelbuild/bazelisk/blob/2ecd43c25b475cab2cd554f0fce40304f2bf3445/config/config.go#L50
    // consulo: upstream public top-level object of BazelVersionWorkspaceResolver.kt
    public static final class BazeliskrcParser {
        private BazeliskrcParser() {
        }

        public static Map<String, String> parse(String content) {
            // consulo: Kotlin lines()/split("=")/associate; split keeps trailing empty strings, associate keeps the last value
            Map<String, String> result = new LinkedHashMap<>();
            content
                .lines()
                .filter(it -> !it.startsWith("#"))
                .map(it -> it.split("=", -1))
                .filter(it -> it.length >= 2)
                .forEach(it -> result.put(it[0].trim(), it[1].trim()));
            return result;
        }
    }
}
