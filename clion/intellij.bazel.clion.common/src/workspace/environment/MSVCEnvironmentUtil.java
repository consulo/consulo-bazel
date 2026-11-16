package org.jetbrains.bazel.clion.workspace.environment;

import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MSVCEnvironmentUtil {
    private MSVCEnvironmentUtil() {
    }

    private static final Logger LOG = Logger.getInstance(MSVCEnvironmentUtil.class);

    /**
     * The environment variable used by bazel to find the Visual Studio directory.
     */
    public static final String BAZEL_VC_VAR = "BAZEL_VC";

    /**
     * Key for {@code TestModeFlags} to mock the value of BAZEL_VC.
     */
    public static final Key<String> BAZEL_VC_KEY = Key.create("BAZEL_VC_ENV");

    /**
     * Pattern to find the Visual Studio directory from the absolute path of the compiler executable.
     */
    private static final Pattern VC_DIRECTORY_PATTERN = Pattern.compile(
        "(?<path>.*)/vc/tools/msvc/.*/bin/.*/cl\\.exe".replace("/", "\\\\")
    );

    private static @Nullable String getBazelVC() {
        // TODO consulo: TestModeFlags.get(BAZEL_VC_KEY) is missing, only the environment variable is read
        return Platform.current().os().getEnvironmentVariable(BAZEL_VC_VAR);
    }

    /**
     * The MSVC toolset requires a home path. Which is the path to VisualStudio installation e.g.: {@code C:\Program
     * Files\Microsoft Visual Studio\2022\Community}
     * <p>
     * This path can be derived from the BAZEL_VC environment variable which points to: {@code C:\Program Files\Microsoft Visual
     * Studio\2022\Community\VC}
     * <p>
     * Or from the compiler path. Not sure what is more reliable, so let's do both.
     */
    public static @Nullable String getToolSetPath(@Nullable Path compiler) {
        @Nullable String bazelVC = getBazelVC();

        // try to get the path from the BAZEL_VC environment variable
        if (bazelVC != null) {
            String toolSetPath = getToolSetPathFromBazelVC(bazelVC);
            if (toolSetPath != null) {
                return toolSetPath;
            }
        }

        // if BAZEL_VC is not set or invalid, try to derive the path from the compiler path
        if (compiler == null) {
            return null;
        }
        String path = compiler.toAbsolutePath().toString().toLowerCase(Locale.ROOT);
        Matcher matcher = VC_DIRECTORY_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group("path");
    }

    private static @Nullable String getToolSetPathFromBazelVC(String bazelVC) {
        try {
            Path parent = Path.of(bazelVC).getParent();
            if (parent == null) {
                return null;
            }
            return parent.toString();
        }
        catch (InvalidPathException e) {
            LOG.warn("Invalid path in BAZEL_VC environment variable.", e);
            return null;
        }
    }
}
