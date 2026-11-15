package org.jetbrains.bazel.bazelisk;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.bazel.bazelisk.localize.BazelBazeliskLocalize;
import consulo.container.boot.ContainerPathManager;
import consulo.http.HttpRequests;
import consulo.logging.Logger;
import consulo.platform.CpuArchitecture;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.project.Project;
import consulo.util.io.NioFiles;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.sync.BazelEnvironmentService;
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jetbrains.bazel.workspace.BazelExecutableProvider;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class BazeliskDownloadingBazelExecutableProvider implements BazelExecutableProvider {
    private static final Logger LOG = Logger.getInstance(BazeliskDownloadingBazelExecutableProvider.class);
    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 2000L;

    @Override
    public CompletableFuture<@Nullable Path> computeBazelExecutable(Project project) {
        Path workspaceRoot = BazelProjectInfoService.getProjectRootDirOrThrow(project.getInstance(BazelProjectInfoService.class))
            .toNioPath();
        String bazeliskVersion = readBazeliskVersion(workspaceRoot);
        if (bazeliskVersion != null) {
            // If requested a specific version, download it
            Path downloaded = downloadBazelisk(bazeliskVersion);
            if (downloaded == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Failed to download bazelisk " + bazeliskVersion));
            }
            return CompletableFuture.completedFuture(downloaded);
        }

        if (!Application.get().getInstance(BazelApplicationContextService.class).getForceBazeliskDownload()) {
            // Otherwise try to find bazel or bazelisk on PATH
            return BazelEnvironmentService.getInstance(project).findInPath("bazel", "bazelisk").thenApply(found -> {
                if (found != null) {
                    return found;
                }
                // If not found, try to download any version of bazelisk
                return downloadBazelisk(null);
            });
        }

        // If not found, try to download any version of bazelisk
        return CompletableFuture.completedFuture(downloadBazelisk(null));
    }

    /**
     * Logic specific to IntelliJ monorepo, see https://youtrack.jetbrains.com/issue/BAZEL-2573/Hermetic-workflow-.bazeliskversion
     */
    private @Nullable String readBazeliskVersion(Path workspaceRoot) {
        Path bazeliskVersionFile = workspaceRoot.resolve(Constants.BAZELISK_VERSION_FILE_NAME);
        String text;
        try {
            text = Files.readString(bazeliskVersionFile);
        }
        catch (Exception e) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private @Nullable Path downloadBazelisk(@Nullable String bazeliskVersion) {
        String downloadUrl = calculateBazeliskDownloadLink(bazeliskVersion);
        if (downloadUrl == null) {
            return null;
        }

        // Use IntelliJ's system cache directory
        Path cacheDir = Path.of(ContainerPathManager.get().getSystemPath(), "bazel-plugin");
        try {
            Files.createDirectories(cacheDir);
        }
        catch (Exception e) {
            LOG.error("Could not create cache directory", e);
            return null;
        }

        // Download bazelisk to the cache folder
        String targetFileName = bazeliskVersion == null ? "bazelisk" : "bazelisk-" + bazeliskVersion;
        Path bazeliskFile = cacheDir.resolve(targetFileName);
        if (Files.exists(bazeliskFile)) {
            LOG.info("Bazelisk already exists in the cache folder: " + bazeliskFile.toAbsolutePath());
            return bazeliskFile;
        }

        LOG.info("Downloading bazelisk to the cache folder: " + bazeliskFile.toAbsolutePath());

        // Download with progress indicator
        try {
            // consulo: upstream runs a Task.WithResult through ProgressManager.run(task)
            ProgressManager.getInstance().<Boolean, IOException>runProcessWithProgressSynchronously(
                () -> {
                    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                    if (indicator != null) {
                        indicator.setText(BazelBazeliskLocalize.bazelProjectDownloadingBazeliskBinary());
                        indicator.setIndeterminate(false);
                    }

                    downloadWithRetry(downloadUrl, bazeliskFile, indicator);

                    if (indicator != null) {
                        indicator.setText(BazelBazeliskLocalize.bazelProjectDownloadingBazeliskSetting_permissions());
                    }
                    NioFiles.setExecutable(bazeliskFile);
                    // consulo: the computation has no result, the type variable is non-null
                    return Boolean.TRUE;
                },
                BazelBazeliskLocalize.bazelProjectDownloadingBazelisk(),
                true,
                null
            );
            LOG.info("Downloaded bazelisk successfully");
            return bazeliskFile;
        }
        catch (Exception e) {
            LOG.error("Failed to download bazelisk", e);
            try {
                Files.deleteIfExists(bazeliskFile);
            }
            catch (IOException ignored) {
                // consulo: Kotlin has no checked exceptions, upstream propagates this one
            }
            return null;
        }
    }

    private void downloadWithRetry(
        String downloadUrl,
        Path bazeliskFile,
        @Nullable ProgressIndicator indicator
    ) throws IOException {
        int attempt = 1;
        while (true) {
            try {
                HttpRequests.request(downloadUrl).saveToFile(bazeliskFile, indicator);
                return;
            }
            catch (IOException e) {
                if (attempt >= MAX_DOWNLOAD_ATTEMPTS) {
                    throw e;
                }
                LOG.warn("Attempt " + attempt + " of " + MAX_DOWNLOAD_ATTEMPTS + " to download bazelisk from " + downloadUrl +
                    " failed, retrying", e);
                Files.deleteIfExists(bazeliskFile);
                try {
                    Thread.sleep(attempt * RETRY_BACKOFF_MS);
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException(ex);
                }
                attempt++;
            }
        }
    }

    private @Nullable String calculateBazeliskDownloadLink(@Nullable String bazeliskVersion) {
        String downloadSegment;
        if (bazeliskVersion != null) {
            downloadSegment = "download/v" + bazeliskVersion;
        }
        else {
            downloadSegment = "latest/download";
        }
        String base = "https://github.com/bazelbuild/bazelisk/releases/" + downloadSegment + "/bazelisk-";

        PlatformOperatingSystem os = Platform.current().os();
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
            platform = null;
        }

        CpuArchitecture cpuArch = Platform.current().jvm().arch();
        String arch;
        if (CpuArchitecture.X86_64.equals(cpuArch)) {
            arch = "amd64";
        }
        else if (CpuArchitecture.AARCH64.equals(cpuArch)) {
            arch = "arm64";
        }
        else {
            arch = null;
        }

        if (platform == null || arch == null) {
            LOG.error("Unsupported platform or architecture for bazelisk download");
            return null;
        }

        String suffix = platform + "-" + arch;
        return !os.isWindows() ? base + suffix : base + suffix + ".exe";
    }
}
