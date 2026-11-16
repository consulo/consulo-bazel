package org.jetbrains.bazel.sync.environment;

import consulo.platform.CpuArchitecture;
import consulo.platform.PlatformOperatingSystem;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * The forced Bazelisk download of {@link BazelApplicationContextService#getForcedBazeliskDownload()}.
 * <p>
 * A forced download skips the lookup of Bazel on `PATH`.
 * {@link Off} forces no download: the plugin looks for Bazel on `PATH` first, and downloads Bazelisk from {@link GitHub} only when
 * the lookup finds nothing.
 */
// consulo: IntelliJ OS / CpuArch are Consulo PlatformOperatingSystem / CpuArchitecture
public interface BazeliskDownload {
    /**
     * Returns the Bazelisk file for {@code os} and {@code arch}, or {@code null} to look for Bazel on `PATH` first.
     */
    @Nullable BazeliskFile bazeliskFile(PlatformOperatingSystem os, CpuArchitecture arch);

    /**
     * A Bazelisk file to download: the Bazelisk {@code version}, the download {@code url}, and the SHA-256 checksum {@code sha256} of
     * the file.
     */
    record BazeliskFile(String version, String url, String sha256) {
    }

    /**
     * Forces no download.
     */
    final class Off implements BazeliskDownload {
        public static final Off INSTANCE = new Off();

        private Off() {
        }

        @Override
        public @Nullable BazeliskFile bazeliskFile(PlatformOperatingSystem os, CpuArchitecture arch) {
            return null;
        }
    }

    /**
     * Forces the download of Bazelisk {@link #VERSION} from the GitHub releases.
     * When you change {@link #VERSION}, also change the checksums.
     * The plugin keeps the checksums in its code, because a checksum from GitHub gives no protection against a changed GitHub release.
     */
    final class GitHub implements BazeliskDownload {
        public static final GitHub INSTANCE = new GitHub();

        public static final String VERSION = "1.30.0";

        private static final Map<String, String> SHA256 = Map.of(
            "bazelisk-darwin-amd64", "a0e6468b0c6b812f782d8cbfb05824121f6980c0bd70aaa1774fb8272bb7b2ad",
            "bazelisk-darwin-arm64", "555d2ff912ccd336d0060bdb5251ef2f62629c6f8fbadb59005a45d5625793ec",
            "bazelisk-linux-amd64", "a248ae2b2b62b5baebb748f6fba7203bff1a27e5d01b55399a8406744cb539e8",
            "bazelisk-linux-arm64", "b2bcc040c213b44586b121bda6d983c35df121b917c3a7a66fb16f2fa0df1a4f",
            "bazelisk-windows-amd64.exe", "5d14a31e9d8a363c48ba1900dbd56764d730835232eb0d113cce255856ffe3ef",
            "bazelisk-windows-arm64.exe", "cb832b1d157e0dc5f89108da426f9c6fe087f5b15873e072480ec49cc5afe488"
        );

        private GitHub() {
        }

        @Override
        public @Nullable BazeliskFile bazeliskFile(PlatformOperatingSystem os, CpuArchitecture arch) {
            String assetName = assetName(os, arch);
            if (assetName == null) {
                return null;
            }
            String sha256 = SHA256.get(assetName);
            if (sha256 == null) {
                return null;
            }
            return new BazeliskFile(VERSION, "https://github.com/bazelbuild/bazelisk/releases/download/v" + VERSION + "/" + assetName, sha256);
        }

        private static @Nullable String assetName(PlatformOperatingSystem os, CpuArchitecture arch) {
            String platform;
            if (os.isWindows()) {
                platform = "windows";
            }
            else if (os.isMac()) {
                platform = "darwin";
            }
            else if (os.isLinux()) {
                platform = "linux";
            }
            else {
                return null;
            }
            String archName;
            if (CpuArchitecture.X86_64.equals(arch)) {
                archName = "amd64";
            }
            else if (CpuArchitecture.AARCH64.equals(arch)) {
                archName = "arm64";
            }
            else {
                return null;
            }
            String name = "bazelisk-" + platform + "-" + archName;
            return os.isWindows() ? name + ".exe" : name;
        }
    }
}
