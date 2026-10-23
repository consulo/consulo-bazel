package org.jetbrains.bazel.commons;

import consulo.util.lang.SemVer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record BazelInfo(
    Path execRoot,
    Path outputBase,
    Path workspaceRoot,
    Path bazelBin,
    BazelRelease release,
    boolean isBzlModEnabled,
    boolean isWorkspaceEnabled,
    /*
     * https://bazel.build/reference/command-line-reference#flag--incompatible_autoload_externally
     * May include rule names and/or provider names.
     */
    List<String> externalAutoloads,

    /* `true` when plugin should support configurations  */
    boolean isConfigurationSupportEnabled
) {
    public BazelInfo(
        Path execRoot,
        Path outputBase,
        Path workspaceRoot,
        Path bazelBin,
        BazelRelease release,
        boolean isBzlModEnabled,
        boolean isWorkspaceEnabled,
        List<String> externalAutoloads
    ) {
        this(execRoot, outputBase, workspaceRoot, bazelBin, release, isBzlModEnabled, isWorkspaceEnabled, externalAutoloads, false);
    }

    public static final BazelInfo DEFAULT = new BazelInfo(
        Path.of("execRoot"),
        Path.of("outputBase"),
        Path.of("workspace"),
        Path.of("bazel-bin"),
        new BazelRelease(7),
        true,
        true,
        List.of()
    );

    public record BazelRelease(int major, int minor, int patch) {
        public BazelRelease(int major) {
            this(major, 0);
        }

        public BazelRelease(int major, int minor) {
            this(major, minor, 0);
        }

        public static @Nullable BazelRelease fromReleaseString(String versionString) {
            Matcher matcher = VERSION_REGEX.matcher(versionString);
            return matcher.find() ? toBazelRelease(matcher) : null;
        }

        public static @Nullable BazelRelease fromVersionString(String versionString) {
            String[] parts = versionString.split("\\.", -1);
            Integer major = parts.length > 0 ? toIntOrNull(parts[0]) : null;
            if (major == null) {
                return null;
            }
            Integer minor = parts.length > 1 ? toIntOrNull(parts[1]) : null;
            Integer patch = parts.length > 2 ? toIntOrNull(parts[2]) : null;
            return new BazelRelease(major, minor != null ? minor : 0, patch != null ? patch : 0);
        }

        public static @Nullable BazelRelease fromBazelVersionFile(Path workspacePath) {
            Path versionFile = workspacePath.resolve(".bazelversion");
            String versionString;
            try {
                versionString = Files.isReadable(versionFile) ? Files.readString(versionFile) : "";
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Matcher matcher = BAZEL_VERSION_REGEX.matcher(versionString);
            return matcher.find() ? toBazelRelease(matcher) : null;
        }

        private static BazelRelease toBazelRelease(Matcher receiver) {
            Integer patch = receiver.group(3) != null ? toIntOrNull(receiver.group(3)) : null;
            return new BazelRelease(Integer.parseInt(receiver.group(1)), Integer.parseInt(receiver.group(2)), patch != null ? patch : 0);
        }

        // consulo: Kotlin String.toIntOrNull()
        private static @Nullable Integer toIntOrNull(String value) {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }

        private static final Pattern BAZEL_VERSION_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

        private static final Pattern VERSION_REGEX = Pattern.compile("(?:release )?(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?=[0-9.]*)");

        public static final BazelRelease FALLBACK_VERSION = new BazelRelease(7, 5);

        static final int OLDEST_SUPPORTED_MAJOR = 7;

        static final Map<Integer, Integer> MINIMAL_MINOR_VERSION = Map.of(7, 5, 8, 2);

        /**
         * Return a string hinting at the appropriate minimal version to update to, if a deprecated bazel version is used. Returns null if the version is not deprecated.
         */
        public @Nullable String deprecated() {
            if (major < OLDEST_SUPPORTED_MAJOR) {
                return "Bazel major version " + major + " is deprecated; the oldest supported version is " + OLDEST_SUPPORTED_MAJOR + "."
                    + MINIMAL_MINOR_VERSION.getOrDefault(OLDEST_SUPPORTED_MAJOR, 0) + ".";
            }

            if (minor < MINIMAL_MINOR_VERSION.getOrDefault(major, 0)) {
                return "Bazel-" + major + " versions older than " + major + "." + MINIMAL_MINOR_VERSION.getOrDefault(major, 0)
                    + " are unsupported.";
            }

            return null;
        }
    }

    public static String toVersionString(BazelRelease receiver) {
        return receiver.major() + "." + receiver.minor() + "." + receiver.patch();
    }

    public static @Nullable BazelRelease orFromBazelVersionFile(@Nullable BazelRelease receiver, Path workspace) {
        return receiver != null ? receiver : BazelRelease.fromBazelVersionFile(workspace);
    }

    public static BazelRelease orFallbackVersion(@Nullable BazelRelease receiver) {
        return receiver != null ? receiver : BazelRelease.FALLBACK_VERSION;
    }

    public static SemVer toSemVer(BazelRelease receiver) {
        return new SemVer(
            receiver.major() + "." + receiver.minor() + "." + receiver.patch(),
            receiver.major(),
            receiver.minor(),
            receiver.patch()
        );
    }
}
