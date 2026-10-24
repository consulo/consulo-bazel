package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput;
import com.esotericsoftware.kryo.kryo5.io.ByteBufferOutput;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.persist.SettingsSavingComponent;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.Mutex;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.lazy.LazyValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.WorkspaceTargetToSave;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotKryo.SnapshotTypeUniverse;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStorage.SnapshotGeneration;
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndex.ExecutableTargetsIndexBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.InMemoryFileToTargetMap;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.CommonWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSnapshotMetadata;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

// TODO: implement `CachesInvalidator`

/**
 * Lazy-loadeable {@link WorkspaceSnapshot} storage engine.
 * <p>
 * Storage itself is split into two parts: lightweight on disk blob storage (refered as `blob`)
 * and heavy MVStore database (refered as `database`). Blob store lightweight data that is not worth to lazy load,
 * it also serve as authority when it comes to generation selection and also carry schema manifest.
 * MVStore serve as partial storage for targets and related data, each target can be loaded depending on user needs.
 * <p>
 * Storage engine is backed by Kryo binary serializer, optimized for minimal data duplication and overall speed.
 * It allows for easy and efficient serialization of arbitrary java/kotlin objects.
 * <p>
 * Storage invalidation is based on closed-world assumption of serializable universe.
 * Each type that will appear inside serializable snapshot must be previously specified inside `WorkspaceTypeContributor`,
 * It allows storage engine to reason when to perform invalidation based on type universe and type definitions itself (mainly fields).
 * Besides that blob also carry global {@link SnapshotKryo#SNAPSHOT_FORMAT_VERSION} which define compatibility
 * level of low-level kryo codecs and MVStore data types, each modification of those must be followed by
 * {@link SnapshotKryo#SNAPSHOT_FORMAT_VERSION} bump.
 * <p>
 * At this point in time snapshot updates should be considered heavy and only done when absolutely necessary,
 * current implementation will schedule heavy storage update each time update is performed regardless of snapshot changes.
 * Overall goal should be to make it only update targets that actually have changes.
 */
public class PersistentWorkspaceSnapshotService implements WorkspaceSnapshotService, SettingsSavingComponent, Disposable {
    private static final Logger LOG = Logger.getInstance(PersistentWorkspaceSnapshotService.class);
    private static final String BLOB_FILE_NAME = "snapshot_blob.data";
    private static final String DB_FILE_NAME = "snapshot_db.data";

    private static final int COMPACT_TIME_BUDGET_MILLIS = 5_000;

    private final Project myProject;
    private final Path myBlobFile;
    private final Path myDbFile;
    private final MutableObservableValue<WorkspaceSnapshot> myCommits = MutableObservableValue.of(WorkspaceSnapshot.EMPTY);
    private final SnapshotKryo myKryo;
    // consulo: Kotlin `lazy { }` with isInitialized(), getStoredValue() is null until the value is created
    private final LazyValue<SnapshotStorage> myStorageLazy;
    private final Mutex myUpdateLock = new Mutex();
    // guards every storage access and the storage close
    private final Mutex myStoreLock = new Mutex();

    private volatile boolean myClosing = false;

    private volatile WorkspaceSnapshot myLastStoredSnapshot = WorkspaceSnapshot.EMPTY;

    private volatile WorkspaceSnapshot myLastStoredSource = WorkspaceSnapshot.EMPTY;

    // consulo: StateFlow replaced by ObservableValue, `commits.asStateFlow()`
    private final ObservableValue<WorkspaceSnapshot> mySnapshot = myCommits;

    private final CompletableFuture<@Nullable Void> myInitialLoad;

    // consulo: state of the upstream `commits.dropWhile { }.debounce(1.seconds).collect { store(it) }` collector
    private volatile boolean myDroppingEmptyCommits = true;
    private final AtomicReference<@Nullable ScheduledFuture<?>> myPendingStore = new AtomicReference<>();
    private final Runnable myStoreCollectorRemover;

    // consulo: the asynchronous part of the last save(), dispose() waits for it like the platform awaits the upstream suspend save()
    private volatile CompletableFuture<@Nullable Void> myPendingSave = CompletableFuture.completedFuture(null);

    // consulo: the upstream service coroutine scope is the project coroutine context
    public PersistentWorkspaceSnapshotService(Project project) {
        myProject = project;
        myBlobFile = getProjectDataPath(project, BLOB_FILE_NAME);
        myDbFile = getProjectDataPath(project, DB_FILE_NAME);
        myKryo = new SnapshotKryo(SnapshotTypeUniverse.buildFromProject(project));
        myStorageLazy = LazyValue.atomicNotNull(() -> new SnapshotStorage(myDbFile, myKryo));

        long loadStart = System.nanoTime();
        myInitialLoad = withLock(myStoreLock, () -> {
            try {
                return CompletableFuture.completedFuture(load());
            }
            catch (IOException e) {
                return CompletableFuture.failedFuture(e);
            }
        }).handle((loaded, error) -> {
            if (error != null) {
                Throwable cause = unwrap(error);
                if (cause instanceof CancellationException cancellation) {
                    throw cancellation;
                }
                LOG.error("Error loading workspace snapshot", cause);
                return null;
            }
            LOG.info("`WorkspaceSnapshot` initial load time: " + formatMillis(System.nanoTime() - loadStart));
            if (loaded != WorkspaceSnapshot.EMPTY) {
                myLastStoredSnapshot = loaded;
                myLastStoredSource = loaded;
                myCommits.compareAndSet(WorkspaceSnapshot.EMPTY, loaded);
            }
            return null;
        });

        // consulo: commits.dropWhile { it === EMPTY }.debounce(1.seconds).flowOn(Dispatchers.Default).collect { store(it) }
        myStoreCollectorRemover = myCommits.addListener(this::onCommit);
    }

    // consulo: IntelliJ Project.getProjectDataPath(name)
    private static Path getProjectDataPath(Project project, String name) {
        return ProjectUtil.getProjectCachePath(project, "projects").resolve(name);
    }

    private SnapshotStorage getStorage() {
        return myStorageLazy.get();
    }

    private boolean isStorageInitialized() {
        return myStorageLazy.getStoredValue() != null;
    }

    private void onCommit(WorkspaceSnapshot value) {
        if (myDroppingEmptyCommits) {
            if (value == WorkspaceSnapshot.EMPTY) {
                return;
            }
            myDroppingEmptyCommits = false;
        }
        ScheduledFuture<?> scheduled = Application.get().getInstance(ApplicationConcurrency.class).getScheduledExecutorService()
            .schedule(() -> {
                store(value);
            }, 1, TimeUnit.SECONDS);
        ScheduledFuture<?> previous = myPendingStore.getAndSet(scheduled);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    @Override
    public ObservableValue<WorkspaceSnapshot> getSnapshot() {
        return mySnapshot;
    }

    @Override
    public CompletableFuture<WorkspaceSnapshot> currentSnapshot() {
        // consulo: Job.join() does not rethrow the failure of the job
        return myInitialLoad.handle((result, error) -> myCommits.get());
    }

    @Override
    public <R extends @Nullable Object> CompletableFuture<Pair<WorkspaceSnapshot, R>> update(WorkspaceSnapshotUpdater<R> updater) {
        // an early updater must not derive from EMPTY
        return myInitialLoad.handle((result, error) -> null).thenCompose(ignored -> withLock(myUpdateLock, () ->
            updater.update(myCommits.get()).thenApply(pair -> {
                WorkspaceSnapshot newSnapshot = pair.getFirst();
                R returnValue = pair.getSecond();
                myCommits.set(newSnapshot);
                return Pair.create(newSnapshot, returnValue);
            })
        ));
    }

    // consulo: SettingsSavingComponent.save() is synchronous in Consulo, the upstream suspend body runs asynchronously
    @Override
    public void save() {
        WorkspaceSnapshot pending = myCommits.get();
        CompletableFuture<@Nullable Void> stored = pending != WorkspaceSnapshot.EMPTY ? store(pending) : CompletableFuture.completedFuture(null);
        myPendingSave = stored.thenCompose(ignored -> withLock(myStoreLock, () -> {
            if (!myClosing && isStorageInitialized() && !getStorage().isClosed()) {
                if (myCommits.get().fileToTarget() instanceof PersistentFileToTargetMap fileToTarget) {
                    fileToTarget.flush();
                }
                getStorage().tryCommit();
            }
            return CompletableFuture.<@Nullable Void>completedFuture(null);
        }));
    }

    private WorkspaceSnapshot load() throws IOException {
        if (myClosing || !Files.exists(myBlobFile)) {
            return WorkspaceSnapshot.EMPTY;
        }
        // TODO: can be optimized to zero-copy file channel
        ByteBufferInput input = new ByteBufferInput(ByteBuffer.wrap(Files.readAllBytes(myBlobFile)));

        int formatVersion = input.readInt();
        if (formatVersion != SnapshotKryo.SNAPSHOT_FORMAT_VERSION) {
            // delete stale database, at this point db isn't opened yet
            Files.deleteIfExists(myDbFile);
            return WorkspaceSnapshot.EMPTY;
        }

        int fingerprintLength = input.readInt();
        byte[] fingerprint = input.readBytes(fingerprintLength);
        boolean fingerprintMatches = Arrays.equals(myKryo.getManifest().fingerprint(), fingerprint);

        SnapshotManifest manifest = SnapshotManifest.readFrom(input);
        if (!fingerprintMatches) {
            LOG.info("Tried loading mismatched workspace snapshot, changes " + SnapshotManifest.describeChangesAgainst(manifest, myKryo.getManifest()));
            return WorkspaceSnapshot.EMPTY;
        }

        PersistentWorkspaceSnapshot partial = KryoHelper.use(myKryo.getPool(), kryo -> kryo.readObject(input, PersistentWorkspaceSnapshot.class));

        // the blob is the sole authority on the current generation, metadata.version IS the generation
        int generationId = partial.getMetadata().version();
        SnapshotGeneration generation = getStorage().openGeneration(generationId);
        if (generation == null) {
            LOG.info("Workspace snapshot blob refer generation " + generationId + " which is not complete in the db");
            return WorkspaceSnapshot.EMPTY;
        }

        return new WorkspaceSnapshot(
            new PersistentWorkspaceTargetMap(partial, generation),
            partial.getWorkspaceName(),
            partial.getConfigurations(),
            partial.getTargetGraph(),
            new PersistentFileToTargetMap(partial, generation),
            new PersistentExecutableTargetsIndex(partial, generation),
            partial.getSyncConfigs(),
            partial.getRepoMapping(),
            partial.getBazelInfo(),
            partial.getMetadata()
        );
    }

    private CompletableFuture<@Nullable Void> store(WorkspaceSnapshot snapshot) {
        return withLock(myStoreLock, () -> {
            if (myClosing || getStorage().isClosed()) {
                return CompletableFuture.<@Nullable Void>completedFuture(null);
            }
            if (snapshot == myLastStoredSnapshot || snapshot == myLastStoredSource) {
                return CompletableFuture.<@Nullable Void>completedFuture(null);
            }
            long start = System.nanoTime();
            CompletableFuture<@Nullable Void> stored;
            try {
                stored = storeImpl(snapshot);
            }
            catch (Throwable e) {
                stored = CompletableFuture.failedFuture(e);
            }
            return stored.handle((result, error) -> {
                if (error == null) {
                    LOG.info("`WorkspaceSnapshot` store time: " + formatMillis(System.nanoTime() - start));
                    return null;
                }
                Throwable cause = unwrap(error);
                if (cause instanceof CancellationException cancellation) {
                    throw cancellation;
                }
                LOG.error("`WorkspaceSnapshot` store failed", cause);
                return null;
            });
        });
    }

    // TODO: maybe some background progress for this thing?
    private CompletableFuture<@Nullable Void> storeImpl(WorkspaceSnapshot snapshot) {
        PersistentWorkspaceTargetMap backing = snapshot.targets() instanceof PersistentWorkspaceTargetMap map ? map : null;
        if (backing != null && !backing.getGeneration().isLive()) {
            LOG.warn("skipping store: snapshot reads through retired generation " + backing.getGeneration().getGeneration());
            return CompletableFuture.completedFuture(null);
        }

        int previousGeneration = myLastStoredSnapshot.metadata().version();
        int newGeneration = previousGeneration + 1;

        List<BuildTarget> allTargets = snapshot.targets().allTargets().toList();

        // always persist previous generation
        Integer snapshotBackingGeneration = snapshot.targets() instanceof PersistentWorkspaceTargetMap map ? map.getGeneration().getGeneration() : null;
        Set<Integer> keep = new LinkedHashSet<>();
        keep.add(previousGeneration);
        if (snapshotBackingGeneration != null) {
            keep.add(snapshotBackingGeneration);
        }
        getStorage().sweepGenerationsExcept(keep);
        SnapshotGeneration generation = getStorage().createGeneration(newGeneration);

        Int2ObjectBiMap<WorkspaceTargetKey> keyId2Target = new Int2ObjectBiMap<>();
        Int2ObjectBiMap<Label> labelId2Label = new Int2ObjectBiMap<>();
        Long2ObjectOpenHashMap<IntArrayList> hash2KeyIds = new Long2ObjectOpenHashMap<>();
        List<WorkspaceTargetToSave> targetsToSave = new ArrayList<>(allTargets.size());
        int nextKeyId = 1;
        int nextLabelId = 1;
        // index only the sources of the local workspace
        OutputLocationResolver resolver = DefaultOutputLocationResolver.createLocalWorkspaceOnly(snapshot.bazelInfo(), snapshot.repoMapping());
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        for (BuildTarget raw : allTargets) {
            // setup global key/label IDs
            WorkspaceTargetKey key = raw.getKey();
            int keyId = nextKeyId++;
            keyId2Target.set(keyId, key);

            // keep labelId2Label bijective to correctly handle duplicated labels
            if (labelId2Label.getReverseOrDefault(key.label(), -1) < 0) {
                labelId2Label.set(nextLabelId++, key.label());
            }

            // prepare saved target
            targetsToSave.add(new WorkspaceTargetToSave(keyId, raw));

            raw.getSources().getOutputLocations()
                .map(it -> resolver.resolve(it, localRepositories))
                .filter(Objects::nonNull)
                .forEach(file -> hash2KeyIds.computeIfAbsent(PersistentFileToTargetMap.hashFilePath(file), it -> new IntArrayList()).add(keyId));
        }

        ConcurrentHashMap<Long, List<WorkspaceTargetKey>> pendingDelta = computePendingFileMappingDelta(
            snapshot, hash2KeyIds, keyId2Target
        );

        int lastLabelId = nextLabelId;
        // targets are serialized in parallel on Dispatchers.Default, then appended
        // by this coroutine in keyId order
        return generation.saveTargets(targetsToSave).<@Nullable Void>thenApply(savedTargets -> {
            generation.saveFileToTargetMap(hash2KeyIds);
            saveExecutableTargetIndex(
                snapshot,
                allTargets,
                labelId2Label,
                generation,
                lastLabelId
            );

            PersistentWorkspaceSnapshot partial = new PersistentWorkspaceSnapshot(
                // copies
                snapshot.workspaceName(),
                snapshot.configurations(),
                snapshot.targetGraph(),
                snapshot.syncConfigs(),
                snapshot.repoMapping(),
                snapshot.bazelInfo(),
                new WorkspaceSnapshotMetadata(newGeneration),

                // extras
                keyId2Target,
                labelId2Label
            );

            Path blobTmp;
            try {
                blobTmp = writeBlobTmp(partial);
            }
            catch (Throwable e) {
                // rollback when blob part save failed
                getStorage().rollback();
                LOG.error("Error saving workspace snapshot", e);
                return null;
            }

            try {
                // try committing database first, only then try to persist blob
                getStorage().commit();
                try {
                    Files.move(blobTmp, myBlobFile, StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(blobTmp, myBlobFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            catch (Throwable e) {
                // commit failure, abort
                try {
                    Files.deleteIfExists(blobTmp);
                }
                catch (IOException deleteError) {
                    throw new UncheckedIOException(deleteError);
                }
                // consulo: the only checked exception of the try block is IOException
                ExceptionUtil.rethrowUnchecked(e);
                throw new UncheckedIOException((IOException) e);
            }

            LOG.info("`WorkspaceSnapshot` stored generation " + newGeneration + " (" + allTargets.size() + " targets)");

            // swap snapshot lazy-loadable fields
            WorkspaceSnapshot twin = new WorkspaceSnapshot(
                new PersistentWorkspaceTargetMap(partial, generation),
                partial.getWorkspaceName(),
                partial.getConfigurations(),
                partial.getTargetGraph(),
                new PersistentFileToTargetMap(
                    partial,
                    generation,
                    pendingDelta != null ? pendingDelta : new ConcurrentHashMap<>()
                ),
                new PersistentExecutableTargetsIndex(partial, generation),
                partial.getSyncConfigs(),
                partial.getRepoMapping(),
                partial.getBazelInfo(),
                partial.getMetadata()
            );
            // set BEFORE publishing, the debounce collector must skip the twin emission
            myLastStoredSnapshot = twin;
            myLastStoredSource = snapshot;
            myCommits.compareAndSet(snapshot, twin);

            // persist only published and new generations, do this so we don't remove generation under opened cursors
            // however in most cases newGeneration == published, they can be different only in case of concurrent writes which are rare
            Integer publishedBacking =
                myCommits.get().targets() instanceof PersistentWorkspaceTargetMap map ? map.getGeneration().getGeneration() : null;
            Set<Integer> published = new LinkedHashSet<>();
            published.add(newGeneration);
            if (publishedBacking != null) {
                published.add(publishedBacking);
            }
            getStorage().sweepGenerationsExcept(published);
            getStorage().commit();
            // compaction is optional, do not make dispose() wait for it
            if (!myClosing) {
                getStorage().compactFile(COMPACT_TIME_BUDGET_MILLIS);
            }
            return null;
        });
    }

    private void saveExecutableTargetIndex(
        WorkspaceSnapshot snapshot,
        List<BuildTarget> allTargets,
        Int2ObjectBiMap<Label> labelId2Label,
        SnapshotGeneration generation,
        int lastLabelId
    ) {
        // executable index is derived data: recompute from the targets being stored, persist as keyIds
        int importDepth = snapshot.syncConfigs().stream()
            .filter(CommonWorkspaceSyncConfig.class::isInstance)
            .map(CommonWorkspaceSyncConfig.class::cast)
            .findFirst()
            .map(CommonWorkspaceSyncConfig::importDepth)
            .orElse(-1);
        Map<Label, List<Label>> hash2ExecutableKeys = ExecutableTargetsIndexBuilder.buildExecutableTargetsIndex(
            snapshot.targetGraph(),
            importDepth,
            allTargets
        );
        Int2ObjectOpenHashMap<IntArrayList> map = new Int2ObjectOpenHashMap<>();
        int nextLabelId = lastLabelId;
        for (Map.Entry<Label, List<Label>> entry : hash2ExecutableKeys.entrySet()) {
            Label label = entry.getKey();
            List<Label> executables = entry.getValue();
            // label might contain macro/generator name which isn't present in `labelId2Label` so add it right now
            int labelId = labelId2Label.getReverseOrDefault(label, -1);
            if (labelId < 0) {
                labelId = nextLabelId++;
                labelId2Label.set(labelId, label);
            }
            IntArrayList executableList = map.computeIfAbsent(labelId, it -> new IntArrayList());
            for (Label executable : executables) {
                int executableId = labelId2Label.getReverseOrDefault(executable, -1);
                if (executableId < 0) {
                    continue;
                }
                executableList.add(executableId);
            }
        }
        generation.saveExecutableTargets(map);
    }

    // TODO: for now storage incremental updates are implemented in monolithic/naive way
    //  we should aim for providing diffing between previous and current snapshot and based on that flush only necessary object
    private @Nullable ConcurrentHashMap<Long, List<WorkspaceTargetKey>> computePendingFileMappingDelta(
        WorkspaceSnapshot snapshot,
        Long2ObjectOpenHashMap<IntArrayList> hash2KeyIds,
        Int2ObjectBiMap<WorkspaceTargetKey> keyId2Target
    ) {
        ConcurrentHashMap<Long, List<WorkspaceTargetKey>> pendingDelta;
        if (snapshot.fileToTarget() instanceof InMemoryFileToTargetMap fileToTarget) {
            pendingDelta = fileToTarget.getDelta();
        }
        else if (snapshot.fileToTarget() instanceof PersistentFileToTargetMap fileToTarget) {
            pendingDelta = fileToTarget.getDelta();
        }
        else {
            pendingDelta = null;
        }
        if (pendingDelta != null) {
            for (Map.Entry<Long, List<WorkspaceTargetKey>> entry : pendingDelta.entrySet()) {
                long hash = entry.getKey();
                List<WorkspaceTargetKey> keys = entry.getValue();
                if (keys.isEmpty()) {
                    hash2KeyIds.remove(hash);
                    continue;
                }
                IntArrayList keyIds = new IntArrayList(keys.size());
                for (WorkspaceTargetKey key : keys) {
                    int keyId = keyId2Target.getReverseOrDefault(key, -1);
                    if (keyId > 0) {
                        keyIds.add(keyId);
                    }
                }
                if (keyIds.isEmpty()) {
                    hash2KeyIds.remove(hash);
                }
                else {
                    hash2KeyIds.put(hash, keyIds);
                }
            }
        }
        return pendingDelta;
    }

    private Path writeBlobTmp(PersistentWorkspaceSnapshot partial) throws IOException {
        // TODO: can be optimized to zero-copy file channel
        ByteBufferOutput output = new ByteBufferOutput(KryoDataType.DEFAULT_BUFFER_SIZE, -1);

        // format version
        output.writeInt(SnapshotKryo.SNAPSHOT_FORMAT_VERSION);

        // fingerprint
        byte[] fingerprint = myKryo.getManifest().fingerprint();
        output.writeInt(fingerprint.length);
        output.writeBytes(fingerprint);

        // manifest itself
        myKryo.getManifest().writeTo(output);

        // last snapshot payload
        KryoHelper.use(myKryo.getPool(), kryo -> {
            kryo.writeObject(output, partial);
            return null;
        });

        Path blobTmp = myBlobFile.resolveSibling(myBlobFile.getFileName() + ".tmp");
        Files.write(blobTmp, output.toBytes());
        return blobTmp;
    }

    @Override
    public void dispose() {
        // consulo: the upstream service scope cancellation stops the debounced store collector
        myStoreCollectorRemover.run();
        ScheduledFuture<?> scheduled = myPendingStore.getAndSet(null);
        if (scheduled != null) {
            scheduled.cancel(false);
        }
        // consulo: the platform awaits the upstream suspend save() before dispose
        try {
            myPendingSave.get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (ExecutionException | CancellationException e) {
            LOG.warn(e);
        }

        myClosing = true;
        // the scope cancellation does not wait for a store in progress, so wait for it here.
        // close() must be synchronous, a reopened project cannot open the database while this store holds it
        // consulo: runBlocking { storeLock.withLock { } } is a blocking lock of the coroutine Mutex
        Object owner = new Object();
        myStoreLock.lockCancellable(owner, () -> {
        });
        try {
            if (isStorageInitialized()) {
                getStorage().close();
            }
        }
        finally {
            myStoreLock.unlock(owner);
        }
    }

    // consulo: kotlinx Mutex.withLock { } over a Consulo coroutine Mutex, the action runs in the project coroutine context
    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    private <T extends @Nullable Object> CompletableFuture<T> withLock(Mutex mutex, Supplier<CompletableFuture<T>> action) {
        Object owner = new Object();
        CoroutineScope scope = CoroutineScope.of(myProject.coroutineContext());
        return Coroutine.first(mutex.<@Nullable Object>lock(owner))
            .then(CompletableFutureStep.<@Nullable Object, T>await(ignored -> action.get()))
            .runAsync(scope, null)
            .toFuture()
            .whenComplete((result, error) -> {
                if (mutex.holdsLock(owner)) {
                    mutex.unlock(owner);
                }
            });
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    // consulo: Kotlin Duration.toString(DurationUnit.MILLISECONDS, 2)
    private static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, "%.2fms", nanos / 1_000_000.0);
    }
}
