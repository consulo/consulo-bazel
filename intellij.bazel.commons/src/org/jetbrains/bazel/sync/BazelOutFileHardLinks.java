package org.jetbrains.bazel.sync;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BazelOutFileHardLinks {
    void onBeforeSync();

    /**
     * Makes the hard links of the current sync visible in the VFS.
     * The sync calls this function before the project model update, so that the project model can find the hard links.
     */
    default CompletableFuture<Void> onBeforeProjectModelUpdate() {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> onAfterSync(boolean fullProjectModelUpdated);

    /**
     * Creates a hard link to Bazel's outputs during sync (e.g., jars).
     * <p>
     * The reason is that Bazel with `--remote_download_minimal` can garbage collect jars it thinks are "not needed"
     * from the output base (see https://youtrack.jetbrains.com/issue/BAZEL-3049), so we cannot rely on them always being there.
     * Using hard links also makes sure fsnotifier isn't spammed by file events whenever Bazel builds a target.
     * <p>
     * NOTE: The operation is expensive, so consider async execution
     *
     * @return the created hard link, {@code originalFile} itself if it isn't an output file (e.g. a for source file), or `null` if
     * {@code originalFile} doesn't exist
     */
    default CompletableFuture<@Nullable Path> createOutputFileHardLink(Path originalFile) {
        return createOutputFileHardLinks(List.of(originalFile)).thenApply(paths -> paths.isEmpty() ? null : paths.get(0));
    }

    /**
     * Creates hard links to Bazel's outputs during sync (e.g., jars) for a batch of files.
     * <p>
     * The reason is that Bazel with `--remote_download_minimal` can garbage collect jars it thinks are "not needed"
     * from the output base (see https://youtrack.jetbrains.com/issue/BAZEL-3049), so we cannot rely on them always being there.
     * Using hard links also makes sure fsnotifier isn't spammed by file events whenever Bazel builds a target.
     * <p>
     * This is the batched entry point; {@link #createOutputFileHardLink} delegates to it for single-file callers.
     * <p>
     * NOTE: The operation is expensive, so consider async execution
     *
     * @param files the original paths to link.
     * @return a list of resulting paths, where each entry is either the created hard link, the {@link Path} itself if it isn't an
     * output file (e.g. a source file), or omitted if the corresponding original file doesn't exist. The size and order of
     * the result are not guaranteed to match {@code files}.
     */
    CompletableFuture<List<Path>> createOutputFileHardLinks(Collection<Path> files);

    /**
     * Returns the result of a hardlink operation from the current sync, including a fallback path.
     * Returns null if this sync has no operation for {@code originalFile}. Does not create a hardlink.
     */
    default CompletableFuture<@Nullable Path> findPreparedOutputFile(Path originalFile) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * {@link #createOutputFileHardLink} only supports files, not directories.
     * If you hardlinked an output file and want to resolve its parent directory from another place in the code, use this function.
     * May return a non-existent path if none of the transitive children of {@code fileOrDir} were ever hardlinked.
     */
    Path resolveCachedPath(Path fileOrDir);

    boolean getAllHardLinksCreatedSuccessfully();

    BazelOutFileHardLinks NONE = new BazelOutFileHardLinks() {
        @Override
        public void onBeforeSync() {
        }

        @Override
        public CompletableFuture<Void> onAfterSync(boolean fullProjectModelUpdated) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<@Nullable Path> createOutputFileHardLink(Path originalFile) {
            return CompletableFuture.completedFuture(originalFile);
        }

        @Override
        public CompletableFuture<List<Path>> createOutputFileHardLinks(Collection<Path> files) {
            return CompletableFuture.completedFuture(List.copyOf(files));
        }

        @Override
        public Path resolveCachedPath(Path fileOrDir) {
            return fileOrDir;
        }

        @Override
        public boolean getAllHardLinksCreatedSuccessfully() {
            return false;
        }
    };
}
