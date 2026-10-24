package org.jetbrains.bsp.protocol;

import com.google.devtools.intellij.aspect.Common;
import consulo.util.io.FileUtil;
import org.jetbrains.bsp.protocol.OutputLocation.External;
import org.jetbrains.bsp.protocol.OutputLocation.Host;
import org.jetbrains.bsp.protocol.OutputLocation.Output;
import org.jetbrains.bsp.protocol.OutputLocation.OutputRoot;
import org.jetbrains.bsp.protocol.OutputLocation.Workspace;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class OutputLocationParserWithoutHardlink {
    // marker Bazel emits for a path relative to the execution root
    private static final String PROC_SELF_CWD = "/proc/self/cwd";
    private static final String BAZEL_OUT = "bazel-out";
    private static final String EXTERNAL = "external";
    private static final String EXTERNAL_PARENT = "..";

    private OutputLocationParserWithoutHardlink() {
    }

    /**
     * Use {@link OutputLocationParser#parseExecrootPath} instead. This method is only intended to be used in unit tests.
     */
    public static OutputLocation parseExecrootPath(String raw) {
        // strip bazel /proc/self/cwd marker
        String path;
        if (raw.equals(PROC_SELF_CWD)) {
            path = "";
        }
        else if (raw.startsWith(PROC_SELF_CWD + "/")) {
            path = raw.substring((PROC_SELF_CWD + "/").length());
        }
        else {
            path = raw;
        }
        // TODO consulo: switch to consulo.util.io.OSAgnosticPathUtil.isAbsolute (ported to the platform, not yet in the published snapshot)
        if (FileUtil.isAbsolutePlatformIndependent(path)) {
            return new Host(path);
        }

        List<String> segments = splitNonEmpty(path);
        @Nullable String first = segments.isEmpty() ? null : segments.get(0);
        // build artifact, it accumulates under the execution root, independent of the symlink settings
        if (BAZEL_OUT.equals(first)) {
            return outputLocation(segments.subList(1, segments.size()));
        }
        // file in an external workspace. `external/<repo>/...` is the classic layout and
        // `../<repo>/...` is the sibling repository layout.
        else if ((EXTERNAL.equals(first) || EXTERNAL_PARENT.equals(first)) && segments.size() >= 2) {
            return new External(
                segments.get(1),
                String.join("/", segments.subList(2, segments.size())),
                EXTERNAL_PARENT.equals(first)
            );
        }
        // else, in the main workspace, execution root mirrors the workspace at the top level
        else {
            return new Workspace(String.join("/", segments));
        }
    }

    /**
     * Use {@link OutputLocationParser#parse} instead. This method is only intended to be used in unit tests.
     */
    public static OutputLocation parse(Common.ArtifactLocation location) {
        List<String> rootSegments = splitNonEmpty(location.getRootPath());
        // generated file, likely from `bazel-out/k8-opt/bin` or similar path
        if (!rootSegments.isEmpty() && rootSegments.get(0).equals(BAZEL_OUT)) {
            return outputLocation(rootSegments.subList(1, rootSegments.size()), location.getRelativePath());
        }
        // external repo file
        else if (location.getIsExternal()) {
            return parseExternalSource(rootSegments, location);
        }
        // TODO: delete after `Common.ArtifactLocation` cannot return execroot path
        // `from_execroot` result
        else if (!location.getIsSource()) {
            return parseExecrootPath(location.getRelativePath());
        }
        // source file from main repo
        else {
            return new Workspace(location.getRelativePath());
        }
    }

    /**
     * Extracts the canonical repository name from external location.
     * <p>
     * The name embedded in a path is already canonical, so no repo mapping is needed.
     * Every aspect constructor emits an external root of exactly two segments:
     * {@code external/<repo>} or, in the sibling repository layout, {@code ../<repo>}.
     */
    private static OutputLocation parseExternalSource(List<String> rootSegments, Common.ArtifactLocation location) {
        if (rootSegments.size() == 2 && (rootSegments.get(0).equals(EXTERNAL_PARENT) || rootSegments.get(0).equals(EXTERNAL))) {
            return new External(
                rootSegments.get(1),
                location.getRelativePath(),
                rootSegments.get(0).equals(EXTERNAL_PARENT)
            );
        }
        else {
            return new Workspace(location.getRelativePath());
        }
    }

    private static Output outputLocation(List<String> afterBazelOut) {
        return outputLocation(afterBazelOut, "");
    }

    private static Output outputLocation(List<String> afterBazelOut, String relativePath) {
        // take e.g., `k8-opt/bin`
        List<String> rootSegments = List.copyOf(afterBazelOut.subList(0, Math.min(2, afterBazelOut.size())));
        // remaining is leftover
        String leftover = String.join("/", afterBazelOut.subList(Math.min(2, afterBazelOut.size()), afterBazelOut.size()));
        return new Output(OutputRoot.of(rootSegments), OutputLocation.joinNonEmpty(leftover, relativePath));
    }

    // consulo: Kotlin `split('/').filter { it.isNotEmpty() }`
    private static List<String> splitNonEmpty(String path) {
        return Arrays.stream(path.split("/")).filter(it -> !it.isEmpty()).toList();
    }
}
