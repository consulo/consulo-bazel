package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.util.Pool;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.index.io.CorruptedException;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.lazy.LazyValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.IntegerDataType;
import org.h2.mvstore.type.LongDataType;
import org.h2.mvstore.type.StringDataType;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions.BuildTargetLoadHint;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions.TargetSection;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.HeavyWorkspaceTarget;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.PartialWorkspaceTarget;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.WorkspaceTargetDeps;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.WorkspaceTargetToSave;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotKryo.KRYO_POOL_INSTANCES;
import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotKryo.SNAPSHOT_FORMAT_VERSION;
import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotRefTable.REF_TABLE_READ_KEY;
import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotRefTable.REF_TABLE_WRITE_KEY;
import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStringTable.STRING_TABLE_READ_KEY;
import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStringTable.STRING_TABLE_WRITE_KEY;

/**
 * Storage is split into {@link SnapshotGeneration}, each generation is set of MVStore maps
 * that together are used to reconstruct original {@link BuildTarget} based on loading conditions {@link TargetLoadOptions},
 * <p>
 * We must always keep previous snapshot generation in order to have fallback in case of update failure/cancellation,
 * this might cause storage file on disk be 2x larger than actual data, but the good news is that is barely impact read/write
 * performance - only larger file on disk.
 * <p>
 * Storage maps are keyed by stable target integer ID, those IDs are calculated on each storage save from scratch
 * and saved to blob metadata, that allow for relatively fast traversals and insertions.
 * Used maps work in "single writer" mode, that means we can access underlying B-Tree in faster way using {@link MVMap#append},
 * also for even bigger speedup we're always sorting keys before insertion to allow MVStore to predictibly write pages.
 * <p>
 * Storage uses {@link ValueFrame} to store actual object payloads,
 * this allows for simple copies of map values (very important for future incremental storage updates),
 * also is means easy parallel deserialization/serialization of underlying data which again improve write/read
 * speeds - most impact for current monolithic updates.
 * <p>
 * Some custom Kryo serializers use global string table (only for selected types) to avoid duplication enormous amount of data,
 * for ultimate case it caused near double reduction ~110mb -> ~60mb in on disk database size, and overall read/write speed improvements
 * for full materializations.
 * The global ref table applies the same idea to objects of any type, e.g. output locations.
 * Each ref value frame keeps the type of the object.
 */
public class SnapshotStorage {
    private static final int INITIAL_VALUE_BUFFER_SIZE = 8 * 1024;
    private static final int ENCODE_CHUNK_SIZE = 256;

    private static final HeavyWorkspaceTarget EMPTY_FILE_SETS = new HeavyWorkspaceTarget(
        OutputLocationCollection.EMPTY,
        OutputLocationCollection.EMPTY
    );

    // limit frame encoder threads to avoid lock contention on kryo instance pool
    // consulo: Dispatchers.IO.limitedParallelism(KRYO_POOL_INSTANCES), created lazily because it needs the application
    private static final LazyValue<ExecutorService> ENCODE_DISPATCHER = LazyValue.atomicNotNull(() -> {
        ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
        return concurrency.createBoundedApplicationPoolExecutor("Workspace Snapshot Encoder", concurrency.executor(), KRYO_POOL_INSTANCES);
    });

    private static final Pool<Output> KRYO_OUTPUT_POOL =
        new Pool<>(/* threadSafe = */ true, /* softReferences = */ false, /* maximumCapacity = */ 16) {
            @Override
            protected Output create() {
                return new Output(INITIAL_VALUE_BUFFER_SIZE, -1);
            }
        };

    private static final Logger LOG = Logger.getInstance(SnapshotStorage.class);

    private static final List<String> GENERATION_MAP_SUFFIXES =
        List.of("id2PartialTarget", "id2TargetDeps", "id2HeavyTarget", "id2TargetData", "file2KeyIds", "executables", "strings", "refs");

    private static String mapName(int generation, String suffix) {
        return "snapshot.g" + generation + "." + suffix;
    }

    private static @Nullable Integer generationOf(String mapName) {
        if (!mapName.startsWith("snapshot.g")) {
            return null;
        }
        String withoutPrefix = mapName.substring("snapshot.g".length());
        int dot = withoutPrefix.indexOf('.');
        String generation = dot == -1 ? withoutPrefix : withoutPrefix.substring(0, dot);
        // consulo: Kotlin String.toIntOrNull()
        try {
            return Integer.parseInt(generation);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private final Path myDbFile;
    private final SnapshotKryo myKryo;

    private final MVStore myStore;
    private final ConcurrentHashMap<String, MVMap<?, ?>> myOpenedMaps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, List<WeakReference<SnapshotGeneration>>> myGenerationRefMap = new ConcurrentHashMap<>();

    private final IntList myAllBuildTargetIds;

    public SnapshotStorage(
        Path dbFile,
        SnapshotKryo kryo
    ) {
        myDbFile = dbFile;
        myKryo = kryo;
        myStore = MVStoreHelper.createOrResetMvStore(LOG, dbFile);
        myAllBuildTargetIds = new IntArrayList(
            kryo.getUniverse().getRegistrations()
                .stream()
                .filter(it -> !it.type().isInterface() && BuildTargetData.class.isAssignableFrom(it.type()))
                .mapToInt(SnapshotKryo.SnapshotTypeRegistration::registrationId)
                .sorted()
                .toArray()
        );
    }

    SnapshotKryo getKryo() {
        return myKryo;
    }

    IntList getAllBuildTargetIds() {
        return myAllBuildTargetIds;
    }

    public boolean isClosed() {
        return myStore.isClosed();
    }

    // safely check, we can consider generation valid only if all required maps are present
    public boolean hasGeneration(int generation) {
        return GENERATION_MAP_SUFFIXES.stream().allMatch(it -> myStore.hasMap(mapName(generation, it)));
    }

    public @Nullable SnapshotGeneration openGeneration(int generation) {
        if (!hasGeneration(generation)) {
            dropGeneration(generation);
            return null;
        }
        try {
            return openGenerationMaps(generation);
        }
        catch (Exception e) {
            LOG.warn("Cannot open snapshot generation " + generation + ", the generation will be removed", e);
            dropGeneration(generation);
            return null;
        }
    }

    public SnapshotGeneration createGeneration(int generation) {
        dropGeneration(generation);
        return openGenerationMaps(generation);
    }

    public void sweepGenerationsExcept(Set<Integer> keep) {
        Set<Integer> generations = new HashSet<>();
        for (String name : myStore.getMapNames()) {
            Integer generation = generationOf(name);
            if (generation != null) {
                generations.add(generation);
            }
        }
        for (int generation : generations) {
            // decide once per generation, a reference cleared in the middle must not split the generation
            if (keep.contains(generation) || isGenerationReferenced(generation)) {
                continue;
            }
            dropGeneration(generation);
        }
    }

    private SnapshotGeneration openGenerationMaps(int generation) {
        SnapshotGeneration handle = new SnapshotGeneration(
            generation,
            this,
            openFrameMap(mapName(generation, "id2PartialTarget"), IntegerDataType.INSTANCE),
            openFrameMap(mapName(generation, "id2TargetDeps"), IntegerDataType.INSTANCE),
            openFrameMap(mapName(generation, "id2HeavyTarget"), IntegerDataType.INSTANCE),
            openFrameMap(mapName(generation, "id2TargetData"), LongDataType.INSTANCE),
            openMap(mapName(generation, "file2KeyIds"), LongDataType.INSTANCE, IntListDataType.INSTANCE),
            openMap(mapName(generation, "executables"), IntegerDataType.INSTANCE, IntListDataType.INSTANCE),
            openMap(mapName(generation, "strings"), IntegerDataType.INSTANCE, StringDataType.INSTANCE),
            openFrameMap(mapName(generation, "refs"), IntegerDataType.INSTANCE)
        );
        myGenerationRefMap.computeIfAbsent(generation, it -> new CopyOnWriteArrayList<>())
            .add(new WeakReference<>(handle));
        return handle;
    }

    private void dropGeneration(int generation) {
        for (String name : new ArrayList<>(myStore.getMapNames())) {
            Integer nameGeneration = generationOf(name);
            if (nameGeneration != null && nameGeneration == generation) {
                MVMap<?, ?> opened = myOpenedMaps.remove(name);
                myStore.removeMap(opened != null ? opened : openUnknownMap(name));
            }
        }
        myGenerationRefMap.remove(generation);
    }

    private boolean isGenerationReferenced(int generation) {
        List<WeakReference<SnapshotGeneration>> handles = myGenerationRefMap.get(generation);
        if (handles == null) {
            return false;
        }
        handles.removeIf(it -> it.get() == null);
        if (handles.isEmpty()) {
            myGenerationRefMap.remove(generation, handles);
            return false;
        }
        return true;
    }

    private MVMap<byte[], byte[]> openUnknownMap(String name) {
        MVMap.Builder<byte[], byte[]> builder = new MVMap.Builder<>();
        builder.setKeyType(UnknownDataType.INSTANCE);
        builder.setValueType(UnknownDataType.INSTANCE);
        builder.singleWriter();
        return myStore.openMap(name, builder);
    }

    public void compactFile(int maxCompactTimeMillis) {
        myStore.compactFile(maxCompactTimeMillis);
    }

    public void rollback() {
        myStore.rollback();
    }

    public void commit() {
        myStore.commit();
    }

    public void tryCommit() {
        if (myStore.hasUnsavedChanges()) {
            myStore.tryCommit();
        }
    }

    public void close() {
        try {
            myStore.close();
        }
        catch (Throwable e) {
            LOG.warn("MVStore close failed, closing immediately", e);
            myStore.closeImmediately();
        }
    }

    ValueFrame encodeFrame(Kryo kryo, Output output, Object value) {
        return encodeFrame(kryo, output, value, null, null);
    }

    ValueFrame encodeFrame(Kryo kryo, Output output, Object value, @Nullable StringTableWriter stringTable) {
        return encodeFrame(kryo, output, value, stringTable, null);
    }

    @SuppressWarnings("unchecked")
    ValueFrame encodeFrame(
        Kryo kryo,
        Output output,
        Object value,
        @Nullable StringTableWriter stringTable,
        @Nullable RefTableWriter refTable
    ) {
        try {
            output.reset();
            // cleared by kryo's auto-reset after the top-level write
            if (stringTable != null) {
                kryo.getGraphContext().put(STRING_TABLE_WRITE_KEY, stringTable);
            }
            if (refTable != null) {
                kryo.getGraphContext().put(REF_TABLE_WRITE_KEY, refTable);
            }
            kryo.writeClassAndObject(output, value);
            return new ValueFrame.Present(SNAPSHOT_FORMAT_VERSION, output.toBytes());
        }
        catch (Throwable e) {
            LOG.error("Error serializing snapshot value", e);
            return ValueFrame.Error.INSTANCE;
        }
    }

    <T> T decodeFrame(ValueFrame frame, Class<T> type) {
        return decodeFrame(frame, type, null, null);
    }

    // consulo: CorruptedException is a checked IOException in Java, it is thrown wrapped into UncheckedIOException
    @SuppressWarnings("unchecked")
    <T> T decodeFrame(
        ValueFrame frame,
        Class<T> type,
        @Nullable StringTableReader stringTable,
        @Nullable RefTableReader refTable
    ) {
        if (!(frame instanceof ValueFrame.Present present)) {
            throw corrupted(new CorruptedException(type + " frame corrupted"));
        }
        if (present.getVersion() != SNAPSHOT_FORMAT_VERSION) {
            // this is illegal state, MVStore database is guarded by on-disk blob
            throw corrupted(new CorruptedException(type + " frame version mismatch"));
        }
        Object obj;
        try {
            ByteBufferInput input = new ByteBufferInput(ByteBuffer.wrap(present.getPayload()));
            // a nested decode of a ref value obtains another kryo, the pool creates one when it is empty
            obj = KryoHelper.use(myKryo.getValuePool(), k -> {
                // cleared by kryo's auto-reset after the top-level read
                if (stringTable != null) {
                    k.getGraphContext().put(STRING_TABLE_READ_KEY, stringTable);
                }
                if (refTable != null) {
                    k.getGraphContext().put(REF_TABLE_READ_KEY, refTable);
                }
                return k.readClassAndObject(input);
            });
        }
        catch (Throwable e) {
            throw corrupted(new CorruptedException("Frame decode error", e));
        }
        // frames are encoded from non-null values only
        if (!type.isInstance(obj)) {
            throw corrupted(new CorruptedException(type + " frame holds " + (obj == null ? null : obj.getClass())));
        }
        return type.cast(obj);
    }

    private static UncheckedIOException corrupted(CorruptedException e) {
        return new UncheckedIOException(e);
    }

    private <K> MVMap<K, ValueFrame> openFrameMap(String name, DataType<K> keyType) {
        MVMap.Builder<K, ValueFrame> builder = new MVMap.Builder<>();
        builder.setKeyType(keyType);
        builder.setValueType(KryoDataType.createFrameDataType());
        builder.singleWriter();
        MVMap<K, ValueFrame> map = myStore.openMap(name, builder);
        myOpenedMaps.put(name, map);
        return map;
    }

    private <K, V> MVMap<K, V> openMap(String name, DataType<K> keyType, DataType<V> valueType) {
        MVMap.Builder<K, V> builder = new MVMap.Builder<>();
        builder.setKeyType(keyType);
        builder.setValueType(valueType);
        builder.singleWriter();
        MVMap<K, V> map = myStore.openMap(name, builder);
        myOpenedMaps.put(name, map);
        return map;
    }

    public static class SnapshotGeneration {
        private final int myGeneration;
        private final SnapshotStorage myStorage;
        private final MVMap<Integer, ValueFrame> myId2PartialTarget;
        private final MVMap<Integer, ValueFrame> myId2TargetDeps;
        private final MVMap<Integer, ValueFrame> myId2HeavyTarget;

        // key: (keyId, buildTargetId) packed into long
        private final MVMap<Long, ValueFrame> myId2TargetData;
        private final MVMap<Long, IntArrayList> myFile2KeyIds;
        private final MVMap<Integer, IntArrayList> myExecutables;
        private final MVMap<Integer, String> myStrings;
        private final MVMap<Integer, ValueFrame> myRefs;

        // consulo: Caffeine `Cache` with weak values, a concurrent weak value map
        private final ConcurrentMap<WorkspaceTargetKey, BuildTarget> myFullTargetCache =
            ContainerUtil.createConcurrentWeakValueMap(); // TODO: check if cleaning cache after sync is better

        // global generation string table, currently used for efficient serialization
        // of paths and labels, which are very likely to duplicate across isolated targets
        // inside spread maps, due to MVStore `DataType` serialization separation (`ValueFrame`),
        // we can access this table on the fly while reading/writing objects
        private final StringTableReader myStringTable;

        // global generation object table, same idea as the string table but for objects of any type
        private final RefTableReader myRefTable;

        SnapshotGeneration(
            int generation,
            SnapshotStorage storage,
            MVMap<Integer, ValueFrame> id2PartialTarget,
            MVMap<Integer, ValueFrame> id2TargetDeps,
            MVMap<Integer, ValueFrame> id2HeavyTarget,
            MVMap<Long, ValueFrame> id2TargetData,
            MVMap<Long, IntArrayList> file2KeyIds,
            MVMap<Integer, IntArrayList> executables,
            MVMap<Integer, String> strings,
            MVMap<Integer, ValueFrame> refs
        ) {
            myGeneration = generation;
            myStorage = storage;
            myId2PartialTarget = id2PartialTarget;
            myId2TargetDeps = id2TargetDeps;
            myId2HeavyTarget = id2HeavyTarget;
            myId2TargetData = id2TargetData;
            myFile2KeyIds = file2KeyIds;
            myExecutables = executables;
            myStrings = strings;
            myRefs = refs;
            myStringTable = new StringTableReader(myStrings);
            myRefTable = new RefTableReader(myRefs, frame -> decodeValue(frame, Object.class));
        }

        public int getGeneration() {
            return myGeneration;
        }

        boolean isLive() {
            return !myId2PartialTarget.isClosed();
        }

        private <T> T decodeValue(ValueFrame frame, Class<T> type) {
            return myStorage.decodeFrame(frame, type, myStringTable, myRefTable);
        }

        private @Nullable BuildTarget composeNewWorkspaceTarget(
            PersistentWorkspaceSnapshot partialSnapshot,
            WorkspaceTargetKey key,
            TargetLoadOptions options
        ) {
            int keyId = partialSnapshot.getKeyId2Target().getReverseOrDefault(key, -1);
            if (keyId < 0) {
                // TODO: assert
                return null;
            }
            ValueFrame partialFrame = myId2PartialTarget.get(keyId);
            if (partialFrame == null) {
                return null;
            }
            PartialWorkspaceTarget partialData = decodeValue(partialFrame, PartialWorkspaceTarget.class);
            return composeFromFrames(
                key,
                options,
                partialData,
                () -> frameOf(myId2TargetDeps, keyId),
                () -> frameOf(myId2HeavyTarget, keyId),
                typeId -> frameOf(myId2TargetData, BuildTargetPack.pack(keyId, typeId))
            );
        }

        private BuildTarget composeFromFrames(
            WorkspaceTargetKey key,
            TargetLoadOptions options,
            PartialWorkspaceTarget partialData,
            Supplier<@Nullable ValueFrame> depsFrame,
            Supplier<@Nullable ValueFrame> heavyFrame,
            // invoked in ascending typeId order (both id sources below are sorted), so the bulk scan may
            // back this with a forward-only cursor over the packed-key map
            IntFunction<@Nullable ValueFrame> dataFrameForType
        ) {
            LazyTargetSection<List<DependencyLabel>> depsSection =
                new LazyTargetSection<>(options.sections().contains(TargetSection.DEPS), () -> decodeDeps(depsFrame.get()));
            LazyTargetSection<HeavyWorkspaceTarget> fileSetsSection =
                new LazyTargetSection<>(options.sections().contains(TargetSection.FILE_SETS), () -> decodeFileSets(heavyFrame.get()));
            BuildTargetLoadHint hint = options.targetData();
            IntList dataTypeIds;
            if (hint instanceof BuildTargetLoadHint.Subset subset) {
                dataTypeIds = typeIdsOf(subset.data());
            }
            else {
                dataTypeIds = myStorage.getAllBuildTargetIds();
            }
            LazyTargetSection<List<BuildTargetData>> dataSection = new LazyTargetSection<>(
                !options.targetData().equals(BuildTargetLoadHint.None.INSTANCE),
                () -> decodeData(dataTypeIds, dataFrameForType)
            );
            Set<TargetSection> loadedSections = new LinkedHashSet<>(options.sections());
            loadedSections.add(TargetSection.INFO);
            return new LazyWorkspaceTarget(
                key,
                partialData.getKind(),
                partialData.getGeneratorName(),
                partialData.isTestOnly(),
                partialData.getTags(),
                partialData.getWorkspaceName(),
                new TargetLoadOptions(loadedSections, options.targetData()),
                depsSection,
                fileSetsSection,
                dataSection
            );
        }

        // an absent frame is a valid state, e.g. a retired generation, and decodes to an empty section
        private List<DependencyLabel> decodeDeps(@Nullable ValueFrame frame) {
            return frame == null ? List.of() : decodeValue(frame, WorkspaceTargetDeps.class).getDependencies();
        }

        private HeavyWorkspaceTarget decodeFileSets(@Nullable ValueFrame frame) {
            return frame == null ? EMPTY_FILE_SETS : decodeValue(frame, HeavyWorkspaceTarget.class);
        }

        private List<BuildTargetData> decodeData(IntList typeIds, IntFunction<@Nullable ValueFrame> dataFrameForType) {
            List<BuildTargetData> loadedData = new ArrayList<>(typeIds.size());
            for (int n = 0; n < typeIds.size(); n++) {
                // a target has frames only for the data types it holds
                ValueFrame frame = dataFrameForType.apply(typeIds.getInt(n));
                if (frame == null) {
                    continue;
                }
                loadedData.add(decodeValue(frame, BuildTargetData.class));
            }
            return loadedData;
        }

        private IntList typeIdsOf(Set<Class<? extends BuildTargetData>> types) {
            IntArrayList typeIds = new IntArrayList(types.size());
            for (Class<? extends BuildTargetData> type : types) {
                int typeId = myStorage.getKryo().getUniverse().getType2Id().getOrDefault(type, -1);
                if (typeId < 0) {
                    // TODO: assert
                    continue;
                }
                typeIds.add(typeId);
            }
            // ascending, per the dataFrameForType ordering contract
            typeIds.sort((IntComparator) null);
            return typeIds;
        }

        // guarded frame read
        private <K> @Nullable ValueFrame frameOf(MVMap<K, ValueFrame> map, K mapKey) {
            if (!isLive()) {
                return null;
            }
            return map.get(mapKey);
        }

        // consulo: Kotlin sequence { } -> lazily evaluated stream, the body runs when the terminal operation starts
        public Stream<BuildTarget> scanAllTargets(PersistentWorkspaceSnapshot partialSnapshot, TargetLoadOptions options) {
            return StreamSupport.stream(
                () -> Spliterators.spliteratorUnknownSize(scanAllTargetsIterator(partialSnapshot, options), Spliterator.ORDERED | Spliterator.NONNULL),
                Spliterator.ORDERED | Spliterator.NONNULL,
                false
            );
        }

        private Iterator<BuildTarget> scanAllTargetsIterator(PersistentWorkspaceSnapshot partialSnapshot, TargetLoadOptions options) {
            if (!isLive()) {
                return List.<BuildTarget>of().iterator();
            }
            boolean skipDeps = !options.sections().contains(TargetSection.DEPS);
            boolean skipFileSets = !options.sections().contains(TargetSection.FILE_SETS);
            boolean skipData = options.targetData().equals(BuildTargetLoadHint.None.INSTANCE);
            PeekingEntryCursor<Integer, ValueFrame> depsCursor = skipDeps ? null : new PeekingEntryCursor<>(myId2TargetDeps, it -> it.longValue());
            PeekingEntryCursor<Integer, ValueFrame> heavyCursor = skipFileSets ? null : new PeekingEntryCursor<>(myId2HeavyTarget, it -> it.longValue());
            PeekingEntryCursor<Long, ValueFrame> dataCursor = skipData ? null : new PeekingEntryCursor<>(myId2TargetData, it -> it);

            Iterator<Map.Entry<Integer, ValueFrame>> entries = myId2PartialTarget.entrySet().iterator();
            return new AbstractIterator<>() {
                @Override
                protected @Nullable BuildTarget computeNext() {
                    while (entries.hasNext()) {
                        Map.Entry<Integer, ValueFrame> entry = entries.next();
                        int keyId = entry.getKey();
                        ValueFrame partialFrame = entry.getValue();
                        WorkspaceTargetKey key = partialSnapshot.getKeyId2Target().get(keyId);
                        if (key == null) {
                            continue;
                        }
                        PartialWorkspaceTarget partialData = decodeValue(partialFrame, PartialWorkspaceTarget.class);

                        ValueFrame depsFrame = depsCursor != null ? depsCursor.advanceTo(keyId) : null;
                        ValueFrame heavyFrame = heavyCursor != null ? heavyCursor.advanceTo(keyId) : null;

                        // composeFromFrames requests typeIds ascending and keyIds ascend across iterations, so the
                        // packed keys form one ascending pass over the data map, stale rows of skipped keyIds are
                        // discarded raw by the next advanceTo
                        return composeFromFrames(
                            key,
                            options,
                            partialData,
                            () -> depsFrame != null ? depsFrame : frameOf(myId2TargetDeps, keyId),
                            () -> heavyFrame != null ? heavyFrame : frameOf(myId2HeavyTarget, keyId),
                            typeId -> {
                                long packedKey = BuildTargetPack.pack(keyId, typeId);
                                if (dataCursor == null) {
                                    return frameOf(myId2TargetData, packedKey);
                                }
                                else {
                                    return dataCursor.advanceTo(packedKey);
                                }
                            }
                        );
                    }
                    return endOfData();
                }
            };
        }

        public @Nullable BuildTarget findOrLoadTarget(PersistentWorkspaceSnapshot partialSnapshot, WorkspaceTargetKey key, TargetLoadOptions options) {
            if (!isLive()) {
                return null;
            }
            // cached full target satisfies any request
            BuildTarget cached = myFullTargetCache.get(key);
            if (cached != null) {
                return cached;
            }
            if (options.equals(TargetLoadOptions.ALL)) {
                return myFullTargetCache.computeIfAbsent(key, it -> composeNewWorkspaceTarget(partialSnapshot, it, TargetLoadOptions.ALL));
            }
            // partial loads are the memory-light bulk path: composed fresh, never cached
            return composeNewWorkspaceTarget(partialSnapshot, key, options);
        }

        // TODO: assert not partial
        public void saveTarget(WorkspaceTargetToSave target) {
            EncodedWorkspaceTarget encoded = KryoHelper.use(
                myStorage.getKryo().getValuePool(),
                kryo -> KryoHelper.use(KRYO_OUTPUT_POOL, output -> encodeTarget(target, kryo, output))
            );
            appendTarget(encoded);
        }

        public CompletableFuture<Void> saveTargets(List<WorkspaceTargetToSave> targets) {
            StringTableWriter stringTable = new StringTableWriter();
            RefTableWriter refTable = new RefTableWriter();
            return encodeInChunks(targets, (kryo, output, target) -> encodeTarget(target, kryo, output, stringTable, refTable))
                .thenCompose(encodedChunks -> {
                    for (List<EncodedWorkspaceTarget> chunk : encodedChunks) {
                        for (EncodedWorkspaceTarget target : chunk) {
                            appendTarget(target);
                        }
                    }

                    // ref values are encoded without the ref table, so a ref inside a ref value is written inline
                    return encodeInChunks(
                        refTable.sortedEntries(),
                        (kryo, output, entry) -> Pair.create(entry.getFirst(), myStorage.encodeFrame(kryo, output, entry.getSecond(), stringTable))
                    );
                })
                .thenAccept(encodedRefs -> {
                    for (List<Pair<Integer, ValueFrame>> chunk : encodedRefs) {
                        for (Pair<Integer, ValueFrame> ref : chunk) {
                            myRefs.append(ref.getFirst(), ref.getSecond());
                        }
                    }

                    // persisted in the same commit as the frames that reference it,
                    // written last because the ref values can add strings
                    for (Pair<Integer, String> entry : stringTable.sortedEntries()) {
                        myStrings.append(entry.getFirst(), entry.getSecond());
                    }

                    myFullTargetCache.clear();
                });
        }

        @FunctionalInterface
        private interface ChunkEncoder<T, R> {
            R encode(Kryo kryo, Output output, T item);
        }

        // result chunks keep the order of `items`
        // consulo: withContext(ENCODE_DISPATCHER) { chunks.map { async { } }.awaitAll() }
        private <T, R> CompletableFuture<List<List<R>>> encodeInChunks(List<T> items, ChunkEncoder<T, R> encode) {
            List<CompletableFuture<List<R>>> chunks = new ArrayList<>();
            for (List<T> chunk : Lists.partition(items, ENCODE_CHUNK_SIZE)) {
                chunks.add(CompletableFuture.supplyAsync(() ->
                    // use single kryo for batch to avoid pool lock contention
                    KryoHelper.use(myStorage.getKryo().getValuePool(), kryo ->
                        KryoHelper.use(KRYO_OUTPUT_POOL, output -> {
                            List<R> encoded = new ArrayList<>(chunk.size());
                            for (T it : chunk) {
                                encoded.add(encode.encode(kryo, output, it));
                            }
                            return encoded;
                        })
                    ), ENCODE_DISPATCHER.get()));
            }
            return CompletableFuture.allOf(chunks.toArray(new CompletableFuture<?>[0]))
                .thenApply(unused -> chunks.stream().map(CompletableFuture::join).toList());
        }

        private EncodedWorkspaceTarget encodeTarget(
            WorkspaceTargetToSave target,
            Kryo kryo,
            Output output
        ) {
            return encodeTarget(target, kryo, output, null, null);
        }

        private EncodedWorkspaceTarget encodeTarget(
            WorkspaceTargetToSave target,
            Kryo kryo,
            Output output,
            @Nullable StringTableWriter stringTable,
            @Nullable RefTableWriter refTable
        ) {
            BuildTarget raw = target.getTarget();

            PartialWorkspaceTarget partialTarget = new PartialWorkspaceTarget(
                raw.getKind(),
                raw.getGeneratorName(),
                raw.isTestOnly(),
                raw.getTags(),
                raw.getWorkspaceName()
            );
            WorkspaceTargetDeps targetDeps = new WorkspaceTargetDeps(raw.getDependencies());
            HeavyWorkspaceTarget heavyTarget = new HeavyWorkspaceTarget(
                raw.getSources(),
                raw.getResources()
            );

            // packed keys must stay ascending for append, sort by data id within the target
            List<Pair<Integer, BuildTargetData>> sortedData = raw.getData().stream().map(data -> {
                int dataId = myStorage.getKryo().getUniverse().getType2Id().getOrDefault(data.getClass(), -1);
                if (dataId < 0) {
                    throw new IllegalStateException("Unrecognized `" + BuildTargetData.class + "` type `" + data.getClass() + "`");
                }
                else {
                    return Pair.create(dataId, data);
                }
            }).sorted(Comparator.comparingInt(it -> it.getFirst())).toList();

            List<Pair<Integer, ValueFrame>> dataFrames = new ArrayList<>(sortedData.size());
            for (Pair<Integer, BuildTargetData> entry : sortedData) {
                dataFrames.add(Pair.create(entry.getFirst(), myStorage.encodeFrame(kryo, output, entry.getSecond(), stringTable, refTable)));
            }
            return new EncodedWorkspaceTarget(
                target.getKeyId(),
                myStorage.encodeFrame(kryo, output, partialTarget, stringTable, refTable),
                myStorage.encodeFrame(kryo, output, targetDeps, stringTable, refTable),
                myStorage.encodeFrame(kryo, output, heavyTarget, stringTable, refTable),
                dataFrames
            );
        }

        // the single-writer: appends only, must stay on one thread
        private void appendTarget(EncodedWorkspaceTarget encoded) {
            int keyId = encoded.getKeyId();
            myId2PartialTarget.append(keyId, encoded.getPartialFrame());
            myId2TargetDeps.append(keyId, encoded.getDepsFrame());
            myId2HeavyTarget.append(keyId, encoded.getHeavyFrame());
            for (Pair<Integer, ValueFrame> entry : encoded.getDataFrames()) {
                myId2TargetData.append(BuildTargetPack.pack(keyId, entry.getFirst()), entry.getSecond());
            }
        }

        // <path hash> -> <list of key IDs>
        public void saveFileToTargetMap(Long2ObjectMap<IntArrayList> map) {
            long[] keys = map.keySet().toLongArray();
            Arrays.sort(keys);
            for (long hash : keys) {
                IntArrayList keyIds = map.get(hash);
                if (keyIds == null) {
                    continue;
                }
                myFile2KeyIds.append(hash, keyIds);
            }
        }

        public Stream<WorkspaceTargetKey> findTargetsByFile(PersistentWorkspaceSnapshot partialSnapshot, Path file) {
            if (!isLive()) {
                return Stream.empty();
            }
            IntArrayList keyIds = myFile2KeyIds.get(PersistentFileToTargetMap.hashFilePath(file));
            if (keyIds == null) {
                return Stream.empty();
            }
            return keyIds.intStream()
                .mapToObj(it -> partialSnapshot.getKeyId2Target().get(it))
                .filter(Objects::nonNull);
        }

        public void putFileMapping(long hash, IntArrayList keyIds) {
            if (!isLive()) {
                return;
            }
            // in contrast, we're using put() here
            myFile2KeyIds.put(hash, keyIds);
        }

        public void removeFileMapping(long hash) {
            if (!isLive()) {
                return;
            }
            myFile2KeyIds.remove(hash);
        }

        public int fileCount() {
            return isLive() ? myFile2KeyIds.size() : 0;
        }

        public int refCount() {
            return isLive() ? myRefs.size() : 0;
        }

        // <label id> -> <list of label id>
        public void saveExecutableTargets(Int2ObjectMap<IntArrayList> map) {
            int[] keys = map.keySet().toIntArray();
            Arrays.sort(keys);
            for (int labelId : keys) {
                IntArrayList keyIds = map.get(labelId);
                if (keyIds == null) {
                    continue;
                }
                myExecutables.append(labelId, keyIds);
            }
        }

        public List<Label> findExecutableTargets(PersistentWorkspaceSnapshot partialSnapshot, Label label) {
            if (!isLive()) {
                return List.of();
            }
            int labelId = partialSnapshot.getLabelId2Label().getReverseOrDefault(label, -1);
            if (labelId < 0) {
                return List.of();
            }
            IntArrayList executables = myExecutables.get(labelId);
            if (executables == null) {
                return List.of();
            }
            List<Label> executableLabels = new ArrayList<>(executables.size());
            for (int n = 0; n < executables.size(); n++) {
                Label executableLabel = partialSnapshot.getLabelId2Label().get(executables.getInt(n));
                if (executableLabel == null) {
                    continue;
                }
                executableLabels.add(executableLabel);
            }
            return executableLabels;
        }
    }
}

final class EncodedWorkspaceTarget {
    private final int myKeyId;
    private final ValueFrame myPartialFrame;
    private final ValueFrame myDepsFrame;
    private final ValueFrame myHeavyFrame;

    // (dataId, frame) sorted ascending by dataId
    // TODO: maybe Int2ArrayMap<...> here?
    private final List<Pair<Integer, ValueFrame>> myDataFrames;

    EncodedWorkspaceTarget(
        int keyId,
        ValueFrame partialFrame,
        ValueFrame depsFrame,
        ValueFrame heavyFrame,
        List<Pair<Integer, ValueFrame>> dataFrames
    ) {
        myKeyId = keyId;
        myPartialFrame = partialFrame;
        myDepsFrame = depsFrame;
        myHeavyFrame = heavyFrame;
        myDataFrames = dataFrames;
    }

    int getKeyId() {
        return myKeyId;
    }

    ValueFrame getPartialFrame() {
        return myPartialFrame;
    }

    ValueFrame getDepsFrame() {
        return myDepsFrame;
    }

    ValueFrame getHeavyFrame() {
        return myHeavyFrame;
    }

    List<Pair<Integer, ValueFrame>> getDataFrames() {
        return myDataFrames;
    }
}

final class BuildTargetPack {
    private BuildTargetPack() {
    }

    static long pack(int keyId, int buildTargetId) {
        return (((long) keyId) << 32) | (((long) buildTargetId) & 0xFFFF_FFFFL);
    }
}
