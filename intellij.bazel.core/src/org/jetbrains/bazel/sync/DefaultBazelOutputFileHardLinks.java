package org.jetbrains.bazel.sync.workspace.mapper.normal;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.NioFiles;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.sync.BazelOutFileHardLinks;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class DefaultBazelOutputFileHardLinks implements BazelOutFileHardLinks {
    public static final String HARDLINKS_DIR_NAME = "intellij-hardlinks";

    private final Project myProject;
    private final Path myBazelOutputBase;

    /**
     * We can only create hard links in the same filesystem and same partition as Bazel's output base (BAZEL-3113).
     * To guarantee this, we create hard links in Bazel's output base itself, see
     * <a href="https://bazel.build/remote/output-directories#layout-diagram">Bazel docs</a>.
     * A side effect of this is that upon `bazel clean --expunge` in a workspace, the respective hard links will be deleted,
     * thereby preventing overly high disk usage. However, in that case the IDE will get red code.
     * Regular `bazel clean` or `--remote_download_minimal` won't have an effect as they only affect execroot, not the whole output base.
     */
    private final Path myCacheDir;
    private final ConcurrentHashMap<Path, CompletableFuture<HardLink>> myHardLinksDuringSync = new ConcurrentHashMap<>();
    private final Set<Path> myMissingFilesDuringSync = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean mySyncRunning = new AtomicBoolean(false);

    private volatile boolean myAllHardLinksCreatedSuccessfully = true;

    public DefaultBazelOutputFileHardLinks(
        Project project,
        BazelInfo bazelInfo
    ) {
        myProject = project;
        myBazelOutputBase = bazelInfo.outputBase();
        myCacheDir = myBazelOutputBase.resolve(HARDLINKS_DIR_NAME);
    }

    public Path getCacheDir() {
        return myCacheDir;
    }

    @Override
    public boolean getAllHardLinksCreatedSuccessfully() {
        return myAllHardLinksCreatedSuccessfully;
    }

    /**
     * @param path the hard link, or the original file when {@code failed} is true
     */
    private record HardLink(Path path, boolean failed) {
        HardLink(Path path) {
            this(path, false);
        }
    }

    @Override
    public CompletableFuture<List<Path>> createOutputFileHardLinks(Collection<Path> files) {
        if (files.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        if (!mySyncRunning.get()) {
            return CompletableFuture.completedFuture(List.copyOf(files));
        }

        // consulo: upstream coroutineScope { files.associateWith { async(limitedDispatcher) { ... } } }
        Map<Path, CompletableFuture<@Nullable Path>> realPaths = new LinkedHashMap<>();
        for (Path originalFile : files) {
            realPaths.put(originalFile, CompletableFuture.<@Nullable Path>supplyAsync(() -> {
                if (myHardLinksDuringSync.containsKey(originalFile)) {
                    return null;  // avoid duplicate computation
                }
                BasicFileAttributes attributes = readAttributesOrNull(originalFile);
                if (attributes == null) {  // file doesn't exist
                    return null;
                }
                else if (!attributes.isSymbolicLink()) {
                    return originalFile;
                }
                else {
                    // We can be missing directory symlinks here by only resolving the last path segment. But this is much quicker than toRealPath on macOS.
                    // This is OK for C++ virtual headers that only symlink files (see, e.g., VirtualIncludesTest), but can be problematic in the future.
                    return PathUtil.readSymbolicLinkTarget(originalFile);
                }
            }, limitedDispatcher()));
        }

        return CompletableFuture.allOf(realPaths.values().toArray(CompletableFuture[]::new)).thenCompose(ignored -> {
            List<Path> retainedPaths = null;
            List<CompletableFuture<HardLink>> hardLinkedPaths = null;

            Path rootDirPath = org.jetbrains.bazel.config.project.getRootDir(myProject).toNioPath();
            for (Path originalFile : files) {
                CompletableFuture<HardLink> existing = myHardLinksDuringSync.get(originalFile);
                if (existing != null) {
                    if (hardLinkedPaths == null) {
                        hardLinkedPaths = new ArrayList<>();
                    }
                    hardLinkedPaths.add(existing);
                    continue;
                }
                CompletableFuture<@Nullable Path> realPathFuture = realPaths.get(originalFile);
                Path realFile = realPathFuture != null ? realPathFuture.join() : null;
                if (realFile == null) {
                    myMissingFilesDuringSync.add(originalFile);
                    continue;
                }

                // Hardlink only Bazel output files
                if (!originalFile.startsWith(myBazelOutputBase)) {
                    if (retainedPaths == null) {
                        retainedPaths = new ArrayList<>();
                    }
                    retainedPaths.add(originalFile);
                    continue;
                }

                Path bazelOutRelativePath = myBazelOutputBase.relativize(originalFile);

                /*
                 * Don't recreate the hard link unnecessarily to avoid spamming the file watcher (and because creating a link is expensive).
                 * Also, the hard link may exist on disk, but if we delete the original file
                 * and recreate it, then the link will point to the old version!
                 */
                // Use await() on a Deferred instead of a blocking computeIfAbsent to avoid thread starvation (BAZEL-3095)
                CompletableFuture<HardLink> hardLink = myHardLinksDuringSync.computeIfAbsent(originalFile, it ->
                    BazelCoroutineService.getInstance(myProject).startAsync(() ->
                        createHardLink(bazelOutRelativePath, rootDirPath, realFile, it)
                    )
                );
                if (hardLinkedPaths == null) {
                    hardLinkedPaths = new ArrayList<>();
                }
                hardLinkedPaths.add(hardLink);
            }

            if (hardLinkedPaths == null) {
                return CompletableFuture.completedFuture(retainedPaths != null ? retainedPaths : List.of());
            }

            List<Path> retained = retainedPaths != null ? retainedPaths : List.of();
            List<CompletableFuture<HardLink>> hardLinked = hardLinkedPaths;
            return awaitAll(hardLinked).thenApply(hardLinks -> {
                List<Path> result = new ArrayList<>(retained);
                for (HardLink it : hardLinks) {
                    result.add(it.path());
                }
                return result;
            });
        });
    }

    // consulo: the body of upstream `withContext(limitedDispatcher) { ... }` in createOutputFileHardLinks
    private CompletableFuture<HardLink> createHardLink(Path bazelOutRelativePath, Path rootDirPath, Path realFile, Path originalFile) {
        Path targetHardLink = myCacheDir.resolve(bazelOutRelativePath);
        return CompletableFuture
            .supplyAsync(() -> readAttributesOrNull(targetHardLink), limitedDispatcher())
            .thenCompose(targetHardLinkAttributes ->
                createHardLinkOrSymbolicLink(targetHardLink, targetHardLinkAttributes, rootDirPath, realFile, originalFile)
            )
            .thenApply(ignored -> new HardLink(targetHardLink))
            .exceptionally(error -> {
                Throwable e = unwrap(error);
                rethrowControlFlowException(e);
                LOG.warn("Failed to create hard link for " + realFile, e);
                myAllHardLinksCreatedSuccessfully = false;
                return new HardLink(realFile, /* failed = */ true);
            });
    }

    private CompletableFuture<Void> createHardLinkOrSymbolicLink(
        Path targetHardLink,
        @Nullable BasicFileAttributes targetHardLinkAttributes,
        Path rootDirPath,
        Path realFile,
        Path originalFile
    ) {
        try {
            if (shouldCreateSymLink(realFile, rootDirPath)) {
                if (targetHardLinkAttributes != null && targetHardLinkAttributes.isSymbolicLink() &&
                    realFile.equals(toRealPathOrNull(targetHardLink))) {
                    return CompletableFuture.completedFuture(null);  // The symlink stayed the same
                }
                try {
                    deleteRecursively(targetHardLink);
                    NioFiles.createParentDirectories(targetHardLink);
                    Files.createSymbolicLink(targetHardLink, realFile);
                    return CompletableFuture.completedFuture(null);
                }
                catch (IOException e) {
                    LOG.warn("Failed to create symlink, Windows without Developer Mode?", e);
                    // fallthrough to hardlinking
                }
            }
            if (Files.isDirectory(originalFile)) {
                if (targetHardLinkAttributes != null && !targetHardLinkAttributes.isDirectory()) {
                    deleteRecursively(targetHardLink);
                }
                // consulo: Kotlin Path.walk(PathWalkOption.FOLLOW_LINKS) lists the files only
                List<Path> walked;
                try (Stream<Path> stream = Files.walk(originalFile, FileVisitOption.FOLLOW_LINKS)) {
                    walked = stream.filter(it -> !Files.isDirectory(it)).toList();
                }
                return createOutputFileHardLinks(walked).<Void>thenApply(ignored -> null);
            }
            else {
                // Hard link. Bazel always deletes and recreates a file when modifying it,
                // meaning the hard link is gonna point to a deleted file with an older timestamp in that case.
                if (targetHardLinkAttributes != null && !targetHardLinkAttributes.isSymbolicLink() && !targetHardLinkAttributes.isDirectory()
                    && targetHardLinkAttributes.lastModifiedTime().equals(Files.getLastModifiedTime(realFile))) {
                    return CompletableFuture.completedFuture(null);  // The hard link is up to date
                }
                deleteRecursively(targetHardLink);
                NioFiles.createParentDirectories(targetHardLink);
                Files.createLink(targetHardLink, realFile);
                return CompletableFuture.completedFuture(null);
            }
        }
        catch (IOException | UncheckedIOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private boolean shouldCreateSymLink(Path realFile, Path rootDirPath) {
        // If realFile is a source file, that means the originalFile was a symlink into the source tree.
        // In that case we should try to create a symlink instead, so that we know where to find the original file (used by CLion: CPP-52156)
        return realFile.startsWith(rootDirPath);
    }

    @Override
    public void onBeforeSync() {
        myHardLinksDuringSync.clear();
        myMissingFilesDuringSync.clear();
        mySyncRunning.set(true);
        myAllHardLinksCreatedSuccessfully = true;
    }

    @Override
    public CompletableFuture<Void> onBeforeProjectModelUpdate() {
        if (!mySyncRunning.get()) {
            return CompletableFuture.completedFuture(null);
        }
        return awaitAll(new ArrayList<>(myHardLinksDuringSync.values())).thenCompose(ignored -> refreshCacheDir());
    }

    @Override
    public CompletableFuture<Void> onAfterSync(boolean fullProjectModelUpdated) {
        if (mySyncRunning.compareAndSet(true, false)) {
            CompletableFuture<Void> result;
            try {
                result = awaitAll(new ArrayList<>(myHardLinksDuringSync.values()))
                    .thenCompose(hardLinks -> {
                        if (fullProjectModelUpdated) {
                            // If sync failed and project model wasn't updated, the user will still see outputs from the previous sync and code won't be red.
                            return deleteUnusedHardLinks(hardLinks);
                        }
                        return CompletableFuture.completedFuture(null);
                    })
                    .thenCompose(ignored -> refreshCacheDir());
            }
            catch (Throwable e) {
                result = CompletableFuture.failedFuture(e);
            }
            // consulo: upstream `finally { }`
            return result.whenComplete((ignored, error) -> {
                myHardLinksDuringSync.clear();
                myMissingFilesDuringSync.clear();
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Updates the VFS with all changes in {@link #getCacheDir()} in one refresh session.
     * The file watcher does not watch {@link #getCacheDir()} (BAZEL-3612), so no file in it is dirty.
     * A recursive refresh skips a file that is not dirty, so {@link #getCacheDir()} is marked dirty first.
     */
    private CompletableFuture<Void> refreshCacheDir() {
        // consulo: upstream withContext(Dispatchers.IO) { }
        return CompletableFuture
            .<@Nullable VirtualFile>supplyAsync(() -> VirtualFileManager.getInstance().refreshAndFindFileByNioPath(myCacheDir), ioDispatcher())
            .thenCompose(cacheDirFile -> {
                if (cacheDirFile == null) {
                    return CompletableFuture.completedFuture(null);
                }
                VirtualFileUtil.markDirty(/* recursive = */ true, /* reloadChildren = */ false, cacheDirFile);
                // consulo: upstream suspending RefreshQueue.refresh(recursive = true, files), completed by the finish runnable here
                CompletableFuture<Void> refreshed = new CompletableFuture<>();
                RefreshQueue.getInstance().refresh(true, /* recursive = */ true, () -> refreshed.complete(null), List.of(cacheDirFile));
                return refreshed;
            });
    }

    @Override
    public Path resolveCachedPath(Path fileOrDir) {
        if (!fileOrDir.startsWith(myBazelOutputBase)) {
            return fileOrDir;
        }
        Path bazelOutRelativePath = myBazelOutputBase.relativize(fileOrDir);
        return myCacheDir.resolve(bazelOutRelativePath);
    }

    @Override
    public CompletableFuture<@Nullable Path> findPreparedOutputFile(Path originalFile) {
        CompletableFuture<HardLink> hardLink = myHardLinksDuringSync.get(originalFile);
        if (hardLink != null) {
            return hardLink.<@Nullable Path>thenApply(HardLink::path);
        }
        return CompletableFuture.completedFuture(myMissingFilesDuringSync.contains(originalFile) ? originalFile : null);
    }

    /**
     * Deletes each file and directory in {@link #getCacheDir()} that the sync did not use.
     * <p>
     * A used path is a hard link of this sync, or a directory that contains one.
     * The directories must count as used, because the cleanup would delete them together with the hard links in them.
     * Each unused path is then a child of a used directory, so the cleanup lists the children of the used directories only.
     * It deletes an unused directory with all its contents.
     */
    private CompletableFuture<Void> deleteUnusedHardLinks(List<HardLink> hardLinks) {
        // consulo: upstream withContext(Dispatchers.IO) { }
        return CompletableFuture.runAsync(() -> {
            Set<Path> pathsUsedDuringSync = new HashSet<>();
            for (HardLink hardLink : hardLinks) {
                if (hardLink.failed()) {
                    continue;
                }
                Path it = hardLink.path();
                while (it != null && !it.equals(myCacheDir)) {
                    pathsUsedDuringSync.add(it);
                    it = it.getParent();
                }
            }
            pathsUsedDuringSync.add(myCacheDir);  // plus() would add the name elements of the path, because Path is Iterable

            List<Path> usedDirectories = new ArrayList<>();
            for (Path it : pathsUsedDuringSync) {
                if (Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS)) {
                    usedDirectories.add(it);
                }
            }
            List<Path> unusedPaths = new ArrayList<>();
            for (Path directory : usedDirectories) {
                for (Path it : listDirectoryEntries(directory)) {
                    if (!pathsUsedDuringSync.contains(it)) {
                        unusedPaths.add(it);
                    }
                }
            }
            for (Path it : unusedPaths) {
                try {
                    deleteRecursively(it);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            // Drop the old cache directories because the name is changed. Delete this code in 26.2
            // consulo: the IntelliJ project data directories (project.getProjectDataPath) were never created by the Consulo port
            // NioFiles.deleteRecursively(project.getProjectDataPath("bazelOutputFilesHardLinks"))
            // NioFiles.deleteRecursively(project.getProjectDataPath("bazel-out-hardlink"))
        }, ioDispatcher());
    }

    private static final Logger LOG = Logger.getInstance(DefaultBazelOutputFileHardLinks.class);

    // consulo: Dispatchers.IO.limitedParallelism(8), created lazily because it needs the application
    private static final LazyValue<ExecutorService> LIMITED_DISPATCHER = LazyValue.atomicNotNull(() -> {
        ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
        return concurrency.createBoundedApplicationPoolExecutor("Bazel Output File Hard Links", concurrency.executor(), 8);
    });

    private static Executor limitedDispatcher() {
        return LIMITED_DISPATCHER.get();
    }

    // consulo: Dispatchers.IO
    private static Executor ioDispatcher() {
        return Application.get().getInstance(ApplicationConcurrency.class).executor();
    }

    // consulo: Kotlin awaitAll()
    private static <T> CompletableFuture<List<T>> awaitAll(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<T> result = new ArrayList<>(futures.size());
            for (CompletableFuture<T> it : futures) {
                result.add(it.join());
            }
            return result;
        });
    }

    // consulo: runCatching { readAttributes<BasicFileAttributes>(LinkOption.NOFOLLOW_LINKS) }.getOrNull()
    private static @Nullable BasicFileAttributes readAttributesOrNull(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
        catch (Exception e) {
            return null;
        }
    }

    // consulo: runCatching { toRealPath() }.getOrNull()
    private static @Nullable Path toRealPathOrNull(Path path) {
        try {
            return path.toRealPath();
        }
        catch (Exception e) {
            return null;
        }
    }

    // consulo: Kotlin Path.deleteRecursively(), does nothing for a missing path and does not follow symlinks
    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (!FileUtil.delete(path)) {
            throw new IOException("Failed to delete " + path);
        }
    }

    // consulo: Kotlin Path.listDirectoryEntries()
    private static List<Path> listDirectoryEntries(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.toList();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // consulo: IntelliJ rethrowControlFlowException()
    private static void rethrowControlFlowException(Throwable e) {
        if (e instanceof ControlFlowException || e instanceof CancellationException) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (e instanceof Error error) {
                throw error;
            }
            throw new CompletionException(e);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable result = throwable;
        while ((result instanceof CompletionException || result instanceof ExecutionException) && result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
