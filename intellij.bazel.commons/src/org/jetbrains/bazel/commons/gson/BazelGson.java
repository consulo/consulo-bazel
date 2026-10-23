package org.jetbrains.bazel.commons.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BazelGson {
    private BazelGson() {
    }

    public static final Gson bazelGson =
        new GsonBuilder()
            .registerTypeHierarchyAdapter(Path.class, PathSerializer.INSTANCE)
            .registerTypeHierarchyAdapter(Label.class, LabelSerializer.INSTANCE)
            .registerTypeAdapterFactory(new SealedClassTypeAdapterFactory())
            .disableHtmlEscaping()
            .create();
}

class SealedClassTypeAdapterFactory implements TypeAdapterFactory {
    private static final String TYPE_FIELD = "type";

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();

        // Check if this is a Kotlin sealed class
        // consulo: Java sealed classes/interfaces instead of Kotlin reflection (KClass.isSealed is false for enums,
        // Class.isSealed is true for enums with constant bodies)
        if (rawType.isEnum() || !rawType.isSealed()) {
            return null;
        }

        // Get all subclasses of the sealed class
        Class<?>[] subclasses = rawType.getPermittedSubclasses();
        if (subclasses == null || subclasses.length == 0) {
            return null;
        }

        // Create a map of class name to class for deserialization lookup
        Map<String, Class<?>> nameToSubclass = new LinkedHashMap<>();
        for (Class<?> subclass : subclasses) {
            nameToSubclass.put(className(subclass), subclass);
        }

        // Create delegate adapters for each subclass to avoid stack overflow
        Map<String, TypeAdapter<?>> subclassAdapters = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : nameToSubclass.entrySet()) {
            subclassAdapters.put(entry.getKey(), gson.getDelegateAdapter(this, TypeToken.get(entry.getValue())));
        }

        // Create a delegate adapter for the base type
        TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, @Nullable T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                Class<?> valueClass = value.getClass();
                String className = className(valueClass);

                // Start writing object
                out.beginObject();
                out.name(TYPE_FIELD);
                out.value(className);

                // Get adapter for value's actual type and write its fields
                JsonObject jsonObject = gson.toJsonTree(value).getAsJsonObject();
                // Remove type field if it exists (to avoid duplication)
                jsonObject.remove(TYPE_FIELD);

                // Write all remaining properties
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    out.name(entry.getKey());
                    gson.toJson(entry.getValue(), out);
                }

                out.endObject();
            }

            @Override
            public @Nullable T read(JsonReader reader) throws IOException {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return null;
                }

                // Only parse enough to get the type
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    return delegateAdapter.read(reader);
                }

                reader.beginObject();
                String className = null;
                JsonObject jsonObject = new JsonObject();

                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals(TYPE_FIELD)) {
                        className = reader.nextString();
                    }
                    else {
                        // Read property value as JsonElement
                        JsonElement value =
                            JsonParser
                                .parseReader(reader);
                        jsonObject.add(name, value);
                    }
                }
                reader.endObject();

                if (className != null) {
                    TypeAdapter<?> adapter = subclassAdapters.get(className);
                    if (adapter != null) {
                        // Add type info back
                        jsonObject.addProperty(TYPE_FIELD, className);
                        // Use the specifically fetched adapter, not going through gson.fromJson
                        return (T) adapter.fromJsonTree(jsonObject);
                    }
                }

                // Fallback: deserialize as the base type
                return delegateAdapter.fromJsonTree(jsonObject);
            }
        };
    }

    // consulo: Kotlin KClass.qualifiedName ?: simpleName ?: toString()
    private static String className(Class<?> clazz) {
        String canonicalName = clazz.getCanonicalName();
        if (canonicalName != null) {
            return canonicalName;
        }
        String simpleName = clazz.getSimpleName();
        return simpleName.isEmpty() ? clazz.toString() : simpleName;
    }

    // consulo: upstream getKClass() (Kotlin reflection) is not needed, Java reflection is used directly
}

class LabelSerializer extends TypeAdapter<@Nullable Label> {
    static final LabelSerializer INSTANCE = new LabelSerializer();

    private LabelSerializer() {
    }

    @Override
    public @Nullable Label read(JsonReader jsonReader) throws IOException {
        return Label.parseOrNull(jsonReader.nextString());
    }

    @Override
    public void write(JsonWriter out, @Nullable Label value) throws IOException {
        if (value == null) {
            out.nullValue();
        }
        else {
            out.value(value.toString());
        }
    }
}

class PathSerializer extends TypeAdapter<@Nullable Path> {
    static final PathSerializer INSTANCE = new PathSerializer();

    private PathSerializer() {
    }

    @Override
    public @Nullable Path read(JsonReader jsonReader) throws IOException {
        return Path.of(jsonReader.nextString());
    }

    @Override
    public void write(JsonWriter out, @Nullable Path value) throws IOException {
        if (value == null) {
            out.nullValue();
        }
        else {
            out.value(value.toString());
        }
    }
}
