package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Registration;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.OutputLocation.OutputRoot;
import org.jetbrains.bsp.protocol.StringsCollection;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotRefTable.REF_TABLE_READ_KEY;
import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotRefTable.REF_TABLE_WRITE_KEY;
import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStringTable.STRING_TABLE_READ_KEY;
import static org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStringTable.STRING_TABLE_WRITE_KEY;

// TODO: Ideas of improving overall snapshot size:
//        - using string pool for segments inside each path/label
//        - use string pool IDs instead of inline encoded string for trie serializer
//  but those are risky because, string reader/writer use ConcurrentHashMap and key values are boxed,
//  so we get boxing, atomics maybe even full monitor lock overhead for each segment which is not ideal,
//  that's why it would need extensive profiling

final class SnapshotSerializers {
    private static final byte POOLED_STRING_INLINE = 0;
    private static final byte POOLED_STRING_REF = 1;

    // consulo: file-private upstream, shared with the serializers in this file
    static final byte REF_INLINE = 0;
    static final byte REF_ID = 1;

    private SnapshotSerializers() {
    }

    static void registerBuiltinTypes(Kryo kryo) {
        // hierarchies serialized via canonical strings, look at `SnapshotClassResolver` for more context
        kryo.register(Path.class, new PathSerializer());
        kryo.register(Label.class, new LabelSerializer());

        // common collections
        kryo.register(ArrayList.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(HashMap.class);
        kryo.register(LinkedHashSet.class);
        kryo.register(HashSet.class);

        // every serializable type have to be explicitly registered
        // listOf, mapOf and similar functions produce different implementations of collection
        // based on element count, that's why we have to register each of them separately
        // consulo: upstream registers the Kotlin collection implementations (emptyList(), listOf(0), listOf(0, 0),
        // mapOf(0 to 0), setOf(0)), the Java port creates JDK collections: Collections.emptyList()/singletonList()/...,
        // Arrays.asList() and the java.util.ImmutableCollections of List.of(), Set.of(), Map.of() and Stream.toList()
        kryo.register(Collections.emptyList().getClass(), new SingletonSerializer(Collections.emptyList()));
        kryo.register(Collections.emptyMap().getClass(), new SingletonSerializer(Collections.emptyMap()));
        kryo.register(Collections.emptySet().getClass(), new SingletonSerializer(Collections.emptySet()));
        kryo.register(Arrays.asList(0, 0).getClass());
        kryo.register(Collections.singletonList(0).getClass());
        kryo.register(Collections.singletonMap(0, 0).getClass());
        kryo.register(int[].class);
        kryo.register(long[].class);
        kryo.register(String[].class);
        kryo.register(Collections.singleton(0).getClass());
        kryo.register(TreeMap.class);
        kryo.register(int[][].class);
        kryo.register(WorkspaceTargetKey[].class);
        // consulo: JDK immutable collections, Kryo has default serializers for them
        registerDistinct(kryo, List.of().getClass(), List.of(0).getClass(), List.of(0, 0, 0).getClass());
        registerDistinct(kryo, Set.of().getClass(), Set.of(0).getClass(), Set.of(0, 1, 2).getClass());
        registerDistinct(kryo, Map.of().getClass(), Map.of(0, 0).getClass(), Map.of(0, 0, 1, 1).getClass());

        // fastutil primitive maps have a single generic parameter, which breaks Kryo's default MapSerializer
        kryo.register(Object2IntOpenHashMap.class, new Object2IntOpenHashMapSerializer());
        kryo.register(Int2ObjectOpenHashMap.class, new Int2ObjectOpenHashMapSerializer());
        kryo.register(Object2ObjectOpenHashMap.class);
        kryo.register(IntOpenHashSet.class);

        // custom collections
        kryo.register(Int2ObjectBiMap.class, new Int2ObjectBiMapSerializer());

        // anonymous-object singletons cannot pass WorkspaceTypeContributor schema validation
        kryo.register(WorkspaceTargetGraph.EMPTY.getClass(), new SingletonSerializer(WorkspaceTargetGraph.EMPTY));

        kryo.register(OutputLocationCollection.EMPTY.getClass(), new SingletonSerializer(OutputLocationCollection.EMPTY));
        kryo.register(InterningOutputLocationCollection.class, new InterningOutputLocationCollectionSerializer());
        kryo.register(OutputRoot.class, new OutputRootSerializer());

        List<Class<?>> outputLocationTypes = new ArrayList<>(Arrays.asList(OutputLocation.class.getPermittedSubclasses()));
        outputLocationTypes.add(OutputLocation.class);
        outputLocationTypes.forEach(type -> kryo.register(type, new OutputLocationSerializer()));

        kryo.register(StringsCollection.class, new StringsCollectionSerializer());
    }

    // consulo: several JDK factory results share one implementation class, register each class once
    private static void registerDistinct(Kryo kryo, Class<?>... types) {
        Set<Class<?>> registered = new HashSet<>();
        for (Class<?> type : types) {
            if (registered.add(type)) {
                kryo.register(type);
            }
        }
    }

    static VersionedKryoSerializer<?> singletonSerializerFor(Class<?> type) {
        try {
            Field field = type.getDeclaredField("INSTANCE");
            return new SingletonSerializer(field.get(null));
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void rebindCollectionFieldSerializers(FieldSerializer<?> serializer) {
        for (FieldSerializer.CachedField field : serializer.getFields()) {
            Class<?> type = field.getField().getType();
            CollectionFieldSerializer fieldSerializer;
            // consulo: the empty types are the JDK empty collections registered in registerBuiltinTypes
            if (type == List.class) {
                fieldSerializer = new CollectionFieldSerializer(ArrayList.class, Collections.emptyList().getClass());
            }
            else if (type == Set.class) {
                fieldSerializer = new CollectionFieldSerializer(HashSet.class, Collections.emptySet().getClass());
            }
            else if (type == Map.class) {
                fieldSerializer = new CollectionFieldSerializer(HashMap.class, Collections.emptyMap().getClass());
            }
            else {
                continue;
            }
            field.setValueClass(type, fieldSerializer);
        }
    }

    // consulo: upstream Kotlin extension functions of Kryo

    static void writePooledString(Kryo receiver, Output output, String str) {
        StringTableWriter table = (StringTableWriter) receiver.getGraphContext().get(STRING_TABLE_WRITE_KEY);
        if (table != null) {
            output.writeByte(POOLED_STRING_REF);
            output.writeVarInt(table.idFor(str), true);
        }
        else {
            output.writeByte(POOLED_STRING_INLINE);
            output.writeString(str);
        }
    }

    static String readPooledString(Kryo receiver, Input input) {
        byte marker = input.readByte();
        return switch (marker) {
            case POOLED_STRING_REF -> {
                if (!(receiver.getGraphContext().get(STRING_TABLE_READ_KEY) instanceof StringTableReader table)) {
                    throw new IllegalStateException("string table is required to decode string ref");
                }
                yield table.get(input.readVarInt(true));
            }

            case POOLED_STRING_INLINE -> input.readString();

            default -> throw new IllegalStateException("invalid string marker: " + marker);
        };
    }

    static void writeRef(Kryo receiver, Output output, Object obj) {
        RefTableWriter table = (RefTableWriter) receiver.getGraphContext().get(REF_TABLE_WRITE_KEY);
        if (table != null) {
            output.writeByte(REF_ID);
            output.writeVarInt(table.idFor(obj), true);
        }
        else {
            output.writeByte(REF_INLINE);
            receiver.writeClassAndObject(output, obj);
        }
    }

    static Object readRef(Kryo receiver, Input input) {
        byte marker = input.readByte();
        return switch (marker) {
            case REF_ID -> {
                if (!(receiver.getGraphContext().get(REF_TABLE_READ_KEY) instanceof RefTableReader table)) {
                    throw new IllegalStateException("ref table is required to decode ref");
                }
                yield table.get(input.readVarInt(true));
            }

            case REF_INLINE -> receiver.readClassAndObject(input);

            default -> throw new IllegalStateException("invalid ref marker: " + marker);
        };
    }

    static void writeRefList(Kryo receiver, Output output, List<? extends Object> values) {
        output.writeVarInt(values.size(), true);
        if (values.isEmpty()) {
            return;
        }
        RefTableWriter table = (RefTableWriter) receiver.getGraphContext().get(REF_TABLE_WRITE_KEY);
        if (table != null) {
            output.writeByte(REF_ID);
            for (Object value : values) {
                output.writeVarInt(table.idFor(value), true);
            }
        }
        else {
            output.writeByte(REF_INLINE);
            for (Object value : values) {
                receiver.writeClassAndObject(output, value);
            }
        }
    }

    static <T> List<T> readRefList(Kryo receiver, Input input, Function<Object, T> cast) {
        int size = input.readVarInt(true);
        if (size == 0) {
            return List.of();
        }
        List<T> result = new ArrayList<>(size);
        byte marker = input.readByte();
        switch (marker) {
            case REF_ID -> {
                if (!(receiver.getGraphContext().get(REF_TABLE_READ_KEY) instanceof RefTableReader table)) {
                    throw new IllegalStateException("ref table is required to decode ref");
                }
                for (int i = 0; i < size; i++) {
                    Object obj = table.get(input.readVarInt(true));
                    result.add(cast.apply(obj));
                }
            }

            case REF_INLINE -> {
                for (int i = 0; i < size; i++) {
                    Object obj = receiver.readClassAndObject(input);
                    result.add(cast.apply(obj));
                }
            }

            default -> throw new IllegalStateException("invalid ref marker: " + marker);
        }
        return result;
    }
}

final class CollectionFieldSerializer extends Serializer<Object> {
    private final Class<?> myFallbackType;
    private final Class<?> myEmptyType;

    CollectionFieldSerializer(Class<?> fallbackType, Class<?> emptyType) {
        super(/* acceptsNull = */ true);
        myFallbackType = fallbackType;
        myEmptyType = emptyType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(Kryo kryo, Output output, @Nullable Object value) {
        if (value == null) {
            kryo.writeClass(output, null);
            return;
        }
        Registration registration = kryo.getClassResolver().getRegistration(value.getClass());
        if (registration == null) {
            registration = fallbackRegistration(kryo, value);
        }
        kryo.writeClass(output, registration.getType());
        ((Serializer<Object>) registration.getSerializer()).write(kryo, output, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Object read(Kryo kryo, Input input, Class<? extends Object> type) {
        Registration registration = kryo.readClass(input);
        if (registration == null) {
            return null;
        }
        return ((Serializer<Object>) registration.getSerializer()).read(kryo, input, (Class<? extends Object>) registration.getType());
    }

    private Registration fallbackRegistration(Kryo kryo, Object value) {
        boolean isEmpty = value instanceof Map<?, ?> map ? map.isEmpty() : ((java.util.Collection<?>) value).isEmpty();
        return kryo.getRegistration(isEmpty ? myEmptyType : myFallbackType);
    }
}

// locations go through the ref table, so equal locations are stored once per generation
final class InterningOutputLocationCollectionSerializer extends VersionedKryoSerializer<InterningOutputLocationCollection> {
    InterningOutputLocationCollectionSerializer() {
        setImmutable(true);
    }

    @Override
    public int getBinaryFormatVersion() {
        return 3;
    }

    @Override
    public void write(Kryo kryo, Output output, InterningOutputLocationCollection obj) {
        SnapshotSerializers.writeRefList(kryo, output, obj.getLocations());
    }

    @Override
    public InterningOutputLocationCollection read(
        Kryo kryo,
        Input input,
        Class<? extends InterningOutputLocationCollection> type
    ) {
        List<OutputLocation> locations = SnapshotSerializers.readRefList(kryo, input, it -> (OutputLocation) it);
        return InterningOutputLocationCollection.create(locations);
    }
}

// the whole collection goes through the ref table, so equal collections are stored once per generation,
// a ref value is written inline with pooled strings
final class StringsCollectionSerializer extends VersionedKryoSerializer<StringsCollection> {
    StringsCollectionSerializer() {
        setImmutable(true);
    }

    @Override
    public int getBinaryFormatVersion() {
        return 1;
    }

    @Override
    public void write(Kryo kryo, Output output, StringsCollection obj) {
        output.writeVarInt(obj.getSize(), true);
        if (obj.isEmpty()) {
            return;
        }
        RefTableWriter table = (RefTableWriter) kryo.getGraphContext().get(REF_TABLE_WRITE_KEY);
        if (table != null) {
            output.writeByte(SnapshotSerializers.REF_ID);
            output.writeVarInt(table.idFor(obj), true);
        }
        else {
            output.writeByte(SnapshotSerializers.REF_INLINE);
            for (String value : obj.asList()) {
                SnapshotSerializers.writePooledString(kryo, output, value);
            }
        }
    }

    @Override
    public StringsCollection read(Kryo kryo, Input input, Class<? extends StringsCollection> type) {
        int size = input.readVarInt(true);
        if (size == 0) {
            return StringsCollection.EMPTY;
        }
        byte marker = input.readByte();
        return switch (marker) {
            case SnapshotSerializers.REF_ID -> {
                if (!(kryo.getGraphContext().get(REF_TABLE_READ_KEY) instanceof RefTableReader table)) {
                    throw new IllegalStateException("ref table is required to decode ref");
                }
                yield (StringsCollection) table.get(input.readVarInt(true));
            }

            case SnapshotSerializers.REF_INLINE -> {
                List<String> strings = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    strings.add(SnapshotSerializers.readPooledString(kryo, input));
                }
                yield StringsCollection.of(strings);
            }

            default -> throw new IllegalStateException("invalid ref marker: " + marker);
        };
    }
}

final class OutputRootSerializer extends VersionedKryoSerializer<OutputRoot> {
    OutputRootSerializer() {
        setImmutable(true);
    }

    @Override
    public int getBinaryFormatVersion() {
        return 2;
    }

    @Override
    public void write(Kryo kryo, Output output, OutputRoot obj) {
        output.writeVarInt(obj.getSegments().size(), true);
        for (String segment : obj.getSegments()) {
            SnapshotSerializers.writePooledString(kryo, output, segment);
        }
    }

    @Override
    public OutputRoot read(Kryo kryo, Input input, Class<? extends OutputRoot> type) {
        int segmentCount = input.readVarInt(true);
        List<String> segments = new ArrayList<>(segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            segments.add(SnapshotSerializers.readPooledString(kryo, input));
        }
        return OutputRoot.of(segments);
    }
}

final class LabelSerializer extends VersionedKryoSerializer<Label> {
    @Override
    public int getBinaryFormatVersion() {
        return 2;
    }

    @Override
    public void write(
        Kryo kryo,
        Output output,
        Label obj
    ) {
        SnapshotSerializers.writePooledString(kryo, output, obj.toString());
    }

    @Override
    public Label read(
        Kryo kryo,
        Input input,
        Class<? extends Label> type
    ) {
        return Label.parse(SnapshotSerializers.readPooledString(kryo, input));
    }
}

@SuppressWarnings({"rawtypes", "unchecked"})
final class Object2IntOpenHashMapSerializer extends VersionedKryoSerializer<Object2IntOpenHashMap> {
    @Override
    public int getBinaryFormatVersion() {
        return 1;
    }

    @Override
    public void write(
        Kryo kryo,
        Output output,
        Object2IntOpenHashMap obj
    ) {
        output.writeVarInt(obj.size(), true);
        for (Object2IntMap.Entry<Object> entry : (Iterable<Object2IntMap.Entry<Object>>) obj.object2IntEntrySet()) {
            kryo.writeClassAndObject(output, entry.getKey());
            output.writeInt(entry.getIntValue());
        }
    }

    @Override
    public Object2IntOpenHashMap read(
        Kryo kryo,
        Input input,
        Class<? extends Object2IntOpenHashMap> type
    ) {
        int size = input.readVarInt(true);
        Object2IntOpenHashMap<Object> map = new Object2IntOpenHashMap<>(size);
        for (int i = 0; i < size; i++) {
            Object key = kryo.readClassAndObject(input);
            map.put(key, input.readInt());
        }
        return map;
    }
}

@SuppressWarnings({"rawtypes", "unchecked"})
final class Int2ObjectOpenHashMapSerializer extends VersionedKryoSerializer<Int2ObjectOpenHashMap> {
    @Override
    public int getBinaryFormatVersion() {
        return 1;
    }

    @Override
    public void write(
        Kryo kryo,
        Output output,
        Int2ObjectOpenHashMap obj
    ) {
        output.writeVarInt(obj.size(), true);
        for (Int2ObjectMap.Entry<Object> entry : (Iterable<Int2ObjectMap.Entry<Object>>) obj.int2ObjectEntrySet()) {
            output.writeInt(entry.getIntKey());
            kryo.writeClassAndObject(output, entry.getValue());
        }
    }

    @Override
    public Int2ObjectOpenHashMap read(
        Kryo kryo,
        Input input,
        Class<? extends Int2ObjectOpenHashMap> type
    ) {
        int size = input.readVarInt(true);
        Int2ObjectOpenHashMap<Object> map = new Int2ObjectOpenHashMap<>(size);
        for (int i = 0; i < size; i++) {
            int key = input.readInt();
            map.put(key, kryo.readClassAndObject(input));
        }
        return map;
    }
}

@SuppressWarnings({"rawtypes", "unchecked"})
final class Int2ObjectBiMapSerializer extends VersionedKryoSerializer<Int2ObjectBiMap> {
    @Override
    public int getBinaryFormatVersion() {
        return 1;
    }

    @Override
    public void write(
        Kryo kryo,
        Output output,
        Int2ObjectBiMap obj
    ) {
        Int2ObjectMap<Object> forward = obj.forward();
        output.writeVarInt(forward.size(), true);
        for (Int2ObjectMap.Entry<Object> entry : forward.int2ObjectEntrySet()) {
            output.writeInt(entry.getIntKey());
            kryo.writeClassAndObject(output, entry.getValue());
        }
    }

    @Override
    public Int2ObjectBiMap read(
        Kryo kryo,
        Input input,
        Class<? extends Int2ObjectBiMap> type
    ) {
        Int2ObjectOpenHashMap<Object> forward = new Int2ObjectOpenHashMap<>();
        Object2IntOpenHashMap<Object> reverse = new Object2IntOpenHashMap<>();
        int size = input.readVarInt(true);
        for (int i = 0; i < size; i++) {
            int key = input.readInt();
            Object value = kryo.readClassAndObject(input);
            forward.put(key, value);
            reverse.put(value, key);
        }
        return new Int2ObjectBiMap<>(forward, reverse);
    }

}

final class SingletonSerializer extends VersionedKryoSerializer<Object> {
    private final Object myInstance;

    SingletonSerializer(Object instance) {
        myInstance = instance;
    }

    @Override
    public int getBinaryFormatVersion() {
        return 1;
    }

    @Override
    public void write(Kryo kryo, Output output, Object obj) {
    }

    @Override
    public Object read(Kryo kryo, Input input, Class<?> type) {
        return myInstance;
    }
}

final class PathSerializer extends VersionedKryoSerializer<Path> {
    PathSerializer() {
        setImmutable(true);
    }

    @Override
    public int getBinaryFormatVersion() {
        return 4;
    }

    @Override
    public void write(Kryo kryo, Output output, Path path) {
        SnapshotSerializers.writePooledString(kryo, output, path.toString());
    }

    @Override
    public Path read(Kryo kryo, Input input, Class<? extends Path> type) {
        return Path.of(SnapshotSerializers.readPooledString(kryo, input));
    }
}

final class OutputLocationSerializer extends VersionedKryoSerializer<OutputLocation> {
    private static final byte LOCATION_WORKSPACE_ID = 0;
    private static final byte LOCATION_OUTPUT_ID = 1;
    private static final byte LOCATION_EXTERNAL_ID = 2;
    private static final byte LOCATION_HOST_ID = 3;
    private static final String RELATIVE_PATH_SEPARATOR = "/";

    @Override
    public int getBinaryFormatVersion() {
        return 1;
    }

    @Override
    public void write(
        Kryo kryo,
        Output output,
        OutputLocation obj
    ) {
        switch (obj) {
            case OutputLocation.Workspace workspace -> {
                output.writeByte(LOCATION_WORKSPACE_ID);
                writePooledPathString(kryo, output, workspace.relativePath());
            }

            case OutputLocation.Output location -> {
                output.writeByte(LOCATION_OUTPUT_ID);
                SnapshotSerializers.writeRef(kryo, output, location.root());
                writePooledPathString(kryo, output, location.relativePath());
            }

            case OutputLocation.External external -> {
                output.writeByte(LOCATION_EXTERNAL_ID);
                SnapshotSerializers.writePooledString(kryo, output, external.repoName());
                writePooledPathString(kryo, output, external.relativePath());
                output.writeBoolean(external.siblingLayout());
            }

            case OutputLocation.Host host -> {
                output.writeByte(LOCATION_HOST_ID);
                writePooledPathString(kryo, output, host.absolutePath());
            }
        }
    }

    @Override
    public OutputLocation read(
        Kryo kryo,
        Input input,
        Class<? extends OutputLocation> type
    ) {
        return switch (input.readByte()) {
            case LOCATION_WORKSPACE_ID ->
                new OutputLocation.Workspace(readPooledPathString(kryo, input));

            case LOCATION_OUTPUT_ID -> {
                OutputRoot outputRoot = (OutputRoot) SnapshotSerializers.readRef(kryo, input);
                String relativePath = readPooledPathString(kryo, input);
                yield new OutputLocation.Output(outputRoot, relativePath);
            }

            case LOCATION_EXTERNAL_ID -> {
                String repoName = SnapshotSerializers.readPooledString(kryo, input);
                String relativePath = readPooledPathString(kryo, input);
                boolean siblingLayout = input.readBoolean();
                yield new OutputLocation.External(repoName, relativePath, siblingLayout);
            }

            case LOCATION_HOST_ID ->
                new OutputLocation.Host(readPooledPathString(kryo, input));


            default -> throw new IllegalStateException("Unknown `" + OutputLocation.class + "` discriminator");
        };
    }

    private static void writePooledPathString(Kryo kryo, Output output, String str) {
        // consulo: Kotlin String.split(delimiter) keeps empty entries and does not use regular expressions
        String[] segments = str.split(Pattern.quote(RELATIVE_PATH_SEPARATOR), -1);
        output.writeVarInt(segments.length, true);
        for (String segment : segments) {
            SnapshotSerializers.writePooledString(kryo, output, segment);
        }
    }

    private static String readPooledPathString(Kryo kryo, Input input) {
        int size = input.readVarInt(true);
        List<String> segments = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            segments.add(SnapshotSerializers.readPooledString(kryo, input));
        }
        return String.join(RELATIVE_PATH_SEPARATOR, segments);
    }

}
