package org.jetbrains.bazel.bazelisk;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.bazel.bazelisk.localize.BazelBazeliskLocalize;
import consulo.component.ProcessCanceledException;
import consulo.container.boot.ContainerPathManager;
import consulo.http.HttpRequests;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.io.NioFiles;
import org.jetbrains.bazel.sync.BazelEnvironmentService;
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService;
import org.jetbrains.bazel.sync.environment.BazeliskDownload;
import org.jetbrains.bazel.workspace.BazelExecutableProvider;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Finds Bazel on `PATH`, or downloads Bazelisk.
 * <p>
 * {@link BazelApplicationContextService#getForcedBazeliskDownload()} can force the download and skip the lookup on `PATH`.
 * The plugin keeps the download in {@code <system>/bazel-plugin/bazelisk-<version>}.
 */
@ExtensionImpl
public class BazeliskDownloadingBazelExecutableProvider implements BazelExecutableProvider {
    private static final Logger LOG = Logger.getInstance(BazeliskDownloadingBazelExecutableProvider.class);
    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 2000L;

    @Override
    public CompletableFuture<@Nullable Path> computeBazelExecutable(Project project) {
        BazeliskDownload.@Nullable BazeliskFile forcedFile = Application.get().getInstance(BazelApplicationContextService.class)
            .getForcedBazeliskDownload()
            .bazeliskFile(Platform.current().os(), Platform.current().jvm().arch());
        CompletableFuture<@Nullable Path> inPath = forcedFile == null
            ? BazelEnvironmentService.getInstance(project).findInPath("bazel", "bazelisk")
            : CompletableFuture.completedFuture(null);
        return inPath.thenCompose(found -> {
            if (found != null) {
                return CompletableFuture.completedFuture(found);
            }
            BazeliskDownload.@Nullable BazeliskFile bazeliskFile = forcedFile != null
                ? forcedFile
                : BazeliskDownload.GitHub.INSTANCE.bazeliskFile(Platform.current().os(), Platform.current().jvm().arch());
            if (bazeliskFile == null) {
                LOG.error("Unsupported platform or architecture for bazelisk download");
                return CompletableFuture.completedFuture(null);
            }
            // consulo: upstream withContext(Dispatchers.IO)
            return CompletableFuture.supplyAsync(
                () -> downloadBazelisk(project, bazeliskFile),
                Application.get().getInstance(ApplicationConcurrency.class).executor()
            );
        });
    }

    private @Nullable Path downloadBazelisk(Project project, BazeliskDownload.BazeliskFile file) {
        String expectedSha256 = file.sha256();
        String downloadUrl = file.url();

        // Use IntelliJ's system cache directory
        Path cacheDir = Path.of(ContainerPathManager.get().getSystemPath(), "bazel-plugin");
        try {
            Files.createDirectories(cacheDir);
        }
        catch (Exception e) {
            LOG.error("Could not create cache directory", e);
            return null;
        }

        Path bazeliskFile = cacheDir.resolve("bazelisk-" + file.version());
        if (Files.exists(bazeliskFile)) {
            try {
                checkFileSha256Checksum(bazeliskFile, expectedSha256);
                LOG.trace("Bazelisk already exists in the cache folder: " + bazeliskFile.toAbsolutePath());
                return bazeliskFile;
            }
            catch (Exception e) {
                LOG.warn("Bazelisk in the cache folder failed the checksum check, downloading it again", e);
            }
        }

        LOG.info("Downloading Bazelisk to the cache folder: " + bazeliskFile.toAbsolutePath());

        // Each download has its own file. The plugin moves the download to bazeliskFile only after the checksum check.
        Path downloadFile;
        try {
            downloadFile = Files.createTempFile(cacheDir, bazeliskFile.getFileName() + "-", ".download");
        }
        catch (IOException e) {
            LOG.error("Could not create a temporary file for the Bazelisk download", e);
            return null;
        }
        try {
            // consulo: upstream withBackgroundProgress + reportSequentialProgress -> a background task, the steps set the
            // progress text and fraction; this method already runs on a pooled thread, so it waits for the task
            CompletableFuture<Void> task = new CompletableFuture<>();
            Task.Backgroundable.queue(project, BazelBazeliskLocalize.bazelProjectDownloadingBazelisk(), true, indicator -> {
                try {
                    indicator.setText(BazelBazeliskLocalize.bazelProjectDownloadingBazeliskBinary());
                    indicator.setIndeterminate(false);
                    downloadWithRetry(downloadUrl, downloadFile, indicator);

                    indicator.setFraction(0.95);
                    indicator.setText(BazelBazeliskLocalize.bazelProjectDownloadingBazeliskSetting_permissions());
                    checkFileSha256Checksum(downloadFile, expectedSha256);
                    NioFiles.setExecutable(downloadFile);
                    Files.move(downloadFile, bazeliskFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    indicator.setFraction(1.0);
                    task.complete(null);
                }
                catch (Throwable e) {
                    task.completeExceptionally(e);
                }
            });
            try {
                task.join();
            }
            catch (CompletionException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
            LOG.info("Downloaded bazelisk successfully");
            return bazeliskFile;
        }
        catch (Throwable e) {
            try {
                Files.deleteIfExists(downloadFile);
            }
            catch (IOException ignored) {
                // consulo: Kotlin has no checked exceptions, upstream propagates this one
            }
            // consulo: IntelliJ rethrowControlFlowException(e)
            if (e instanceof ProcessCanceledException processCanceledException) {
                throw processCanceledException;
            }
            LOG.error("Failed to download bazelisk", e);
            return null;
        }
    }

    private void downloadWithRetry(String downloadUrl, Path bazeliskFile, @Nullable ProgressIndicator indicator) throws IOException {
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

    /**
     * Throws an {@link IOException} when the SHA-256 checksum of {@code file} is not {@code expectedSha256}.
     */
    public static void checkFileSha256Checksum(Path file, String expectedSha256) throws IOException {
        String actualSha256 = sha256Hex(file);
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            throw new IOException(BazelBazeliskLocalize.bazelBazeliskChecksumMismatch(file, actualSha256, expectedSha256).get());
        }
    }

    // consulo: IntelliJ com.intellij.util.io.sha256Hex(Path)
    private static String sha256Hex(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
