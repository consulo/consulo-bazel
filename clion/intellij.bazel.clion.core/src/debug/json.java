package org.jetbrains.bazel.clion.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.clion.workspace.buildEquivalenceClasses;
import org.jetbrains.bazel.clion.workspace.CcResolveConfiguration;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.commons.gson.BazelGson;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
final class json {
    private json() {
    }

    static final Gson bazelDebugGson = createBazelDebugGson();

    // consulo: the upstream builder chain, the serializer type is passed explicitly (Kotlin reified type parameter)
    private static Gson createBazelDebugGson() {
        GsonBuilder builder = BazelGson.bazelGson.newBuilder()
            .setPrettyPrinting();
        registerSerializer(builder, OutputLocation.class, json::serializeOutputLocation);
        registerSerializer(builder, OutputLocationCollection.class, json::serializeOutputLocations);
        registerSerializer(builder, WorkspaceConfigurationId.class, json::serializeWorkspaceConfigurationId);
        registerSerializer(builder, WorkspaceTargetKey.class, json::serializeTargetKey);
        registerSerializer(builder, TargetKind.class, json::serializeTargetKind);
        registerSerializer(builder, DependencyLabel.class, json::serializeDependencyLabel);
        registerSerializer(builder, CcResolveConfiguration.Identifier.class, json::serializeCcIdentifier);
        registerSerializer(builder, File.class, json::serializeFile);
        registerSerializer(builder, VirtualFile.class, json::serializeVirtualFile);
        registerSerializer(builder, StringsCollection.class, json::serializeStringsCollection);
        return builder.create();
    }

    @FunctionalInterface
    private interface Serializer<T> {
        void write(JsonWriter writer, T value) throws IOException;
    }

    private static <T> GsonBuilder registerSerializer(GsonBuilder receiver, Class<T> type, Serializer<T> body) {
        receiver.registerTypeHierarchyAdapter(type, new TypeAdapter<@Nullable T>() {
            @Override
            public T read(JsonReader reader) {
                throw new UnsupportedOperationException("An operation is not implemented.");
            }

            @Override
            public void write(JsonWriter writer, @Nullable T value) throws IOException {
                if (value == null) {
                    writer.nullValue();
                }
                else {
                    body.write(writer, value);
                }
            }
        });

        return receiver;
    }

    private static void serializeOutputLocation(JsonWriter writer, OutputLocation value) throws IOException {
        writer.value(OutputLocation.toExecrootPath(value));
    }

    private static void serializeOutputLocations(JsonWriter writer, OutputLocationCollection value) throws IOException {
        writer.beginArray();
        for (Iterator<OutputLocation> it = value.getOutputLocations().iterator(); it.hasNext(); ) {
            writer.value(OutputLocation.toExecrootPath(it.next()));
        }
        writer.endArray();
    }

    private static void serializeWorkspaceConfigurationId(JsonWriter writer, WorkspaceConfigurationId value) throws IOException {
        String shortChecksum = value.getShortChecksum();
        if (shortChecksum == null) {
            writer.nullValue();
        }
        else {
            writer.value(shortChecksum);
        }
    }

    private static void serializeTargetKey(JsonWriter writer, WorkspaceTargetKey value) throws IOException {
        writer.beginObject();
        writer.name("label");
        writer.value(value.label().toString());
        writer.name("configuration");
        serializeWorkspaceConfigurationId(writer, value.configuration());
        writer.name("aspect_ids");
        writer.beginArray();
        for (String it : value.aspectIds().getIds()) {
            writer.value(it);
        }
        writer.endArray();
        writer.endObject();
    }

    private static void serializeTargetKind(JsonWriter writer, TargetKind value) throws IOException {
        writer.beginObject();
        writer.name("rule");
        writer.value(value.kind());
        writer.name("rule_type");
        writer.value(value.ruleType().name());
        writer.name("language_classes");
        writer.beginArray();
        for (Object it : value.languageClasses()) {
            writer.value(it.toString());
        }
        writer.endArray();
        writer.endObject();
    }

    private static void serializeDependencyLabel(JsonWriter writer, DependencyLabel value) throws IOException {
        writer.beginObject();
        writer.name("kind");
        writer.value(value.kind().name());
        writer.name("target");
        serializeTargetKey(writer, value.targetKey());
        writer.endObject();
    }

    private static void serializeCcIdentifier(JsonWriter writer, CcResolveConfiguration.Identifier value) throws IOException {
        writer.value(buildEquivalenceClasses.encode(value));
    }

    private static void serializeFile(JsonWriter writer, File value) throws IOException {
        writer.value(value.getPath());
    }

    private static void serializeVirtualFile(JsonWriter writer, VirtualFile value) throws IOException {
        writer.value(value.getPath());
    }

    private static void serializeStringsCollection(JsonWriter writer, StringsCollection value) throws IOException {
        writer.beginArray();
        for (String it : value.asList()) {
            writer.value(it);
        }
        writer.endArray();
    }
}
