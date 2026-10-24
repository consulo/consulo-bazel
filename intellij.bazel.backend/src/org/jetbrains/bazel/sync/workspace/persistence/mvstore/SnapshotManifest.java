package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public record SnapshotManifest(SortedSet<SnapshotManifest.SnapshotManifestEntry> entries) {
    public sealed interface SnapshotTypeSchema {
        record Fields(List<Field> fields) implements SnapshotTypeSchema {
            public record Field(String name, String typeFqn) {
            }
        }

        record Enum(List<String> constants) implements SnapshotTypeSchema {
        }

        final class Singleton implements SnapshotTypeSchema {
            public static final Singleton INSTANCE = new Singleton();

            private Singleton() {
            }

            // consulo: data object equality
            @Override
            public boolean equals(@Nullable Object o) {
                return o instanceof Singleton;
            }

            @Override
            public int hashCode() {
                return Singleton.class.getName().hashCode();
            }

            @Override
            public String toString() {
                return "Singleton";
            }
        }
    }

    public record SnapshotManifestEntry(
        int registrationId,
        String fqn,
        SnapshotTypeSchema schema
    ) {
        static final Comparator<SnapshotManifestEntry> COMPARATOR = Comparator.comparingInt(SnapshotManifestEntry::registrationId)
            .thenComparing(SnapshotManifestEntry::fqn);
    }

    private static final int KIND_FIELDS = 0;
    private static final int KIND_ENUM = 1;
    private static final int KIND_SINGLETON = 2;
    private static final int DIGEST_BUFFER_SIZE = 4096;

    // compute sha-256 fingerprint of serialization schema
    public byte[] fingerprint() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        DigestOutputStream stream = new DigestOutputStream(OutputStream.nullOutputStream(), digest);
        try (Output output = new Output(stream, DIGEST_BUFFER_SIZE)) {
            writeTo(output);
        }
        return digest.digest();
    }

    public void writeTo(Output output) {
        output.writeVarInt(entries.size(), true);
        for (SnapshotManifestEntry entry : entries) {
            output.writeVarInt(entry.registrationId(), true);
            output.writeString(entry.fqn());
            SnapshotTypeSchema schema = entry.schema();
            if (schema instanceof SnapshotTypeSchema.Fields fields) {
                output.writeByte(KIND_FIELDS);
                output.writeVarInt(fields.fields().size(), true);
                for (SnapshotTypeSchema.Fields.Field field : fields.fields()) {
                    output.writeString(field.name());
                    output.writeString(field.typeFqn());
                }
            }
            else if (schema instanceof SnapshotTypeSchema.Enum enumSchema) {
                output.writeByte(KIND_ENUM);
                output.writeVarInt(enumSchema.constants().size(), true);
                enumSchema.constants().forEach(output::writeString);
            }
            else if (schema instanceof SnapshotTypeSchema.Singleton) {
                output.writeByte(KIND_SINGLETON);
            }
        }
    }

    public static SnapshotManifest readFrom(Input input) {
        int count = input.readVarInt(true);
        List<SnapshotManifestEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int registrationId = input.readVarInt(true);
            String fqn = Objects.requireNonNull(input.readString());
            int kind = input.readByte();
            SnapshotTypeSchema schema = switch (kind) {
                case KIND_FIELDS -> {
                    int fieldCount = input.readVarInt(true);
                    List<SnapshotTypeSchema.Fields.Field> fields = new ArrayList<>(fieldCount);
                    for (int j = 0; j < fieldCount; j++) {
                        fields.add(new SnapshotTypeSchema.Fields.Field(
                            Objects.requireNonNull(input.readString()),
                            Objects.requireNonNull(input.readString())
                        ));
                    }
                    yield new SnapshotTypeSchema.Fields(fields);
                }

                case KIND_ENUM -> {
                    int constantCount = input.readVarInt(true);
                    List<String> constants = new ArrayList<>(constantCount);
                    for (int j = 0; j < constantCount; j++) {
                        constants.add(Objects.requireNonNull(input.readString()));
                    }
                    yield new SnapshotTypeSchema.Enum(constants);
                }

                case KIND_SINGLETON -> SnapshotTypeSchema.Singleton.INSTANCE;

                default -> throw new IllegalStateException("Unknown manifest schema kind: " + kind);
            };
            entries.add(new SnapshotManifestEntry(registrationId, fqn, schema));
        }
        SortedSet<SnapshotManifestEntry> sorted = new TreeSet<>(SnapshotManifestEntry.COMPARATOR);
        sorted.addAll(entries);
        return new SnapshotManifest(sorted);
    }

    // consulo: upstream internal extension function
    static List<String> describeChangesAgainst(SnapshotManifest receiver, SnapshotManifest current) {
        List<String> reasons = new ArrayList<>();
        Map<String, SnapshotManifestEntry> storedByFqn = associateByFqn(receiver.entries());
        Map<String, SnapshotManifestEntry> currentByFqn = associateByFqn(current.entries());

        Set<String> removed = new LinkedHashSet<>(storedByFqn.keySet());
        removed.removeAll(currentByFqn.keySet());
        removed.forEach(it -> reasons.add("type removed: " + it));
        Set<String> added = new LinkedHashSet<>(currentByFqn.keySet());
        added.removeAll(storedByFqn.keySet());
        added.forEach(it -> reasons.add("type added: " + it));

        for (SnapshotManifestEntry entry : receiver.entries()) {
            int storedId = entry.registrationId();
            String fqn = entry.fqn();
            SnapshotTypeSchema storedSchema = entry.schema();
            SnapshotManifestEntry currentEntry = currentByFqn.get(fqn);
            if (currentEntry == null) {
                continue;
            }
            if (storedId != currentEntry.registrationId()) {
                reasons.add("registration id of " + fqn + " changed: " + storedId + " -> " + currentEntry.registrationId());
                continue;
            }
            if (!storedSchema.equals(currentEntry.schema())) {
                reasons.add(describeSchemaChange(fqn, storedSchema, currentEntry.schema()));
            }
        }
        return reasons;
    }

    private static String describeSchemaChange(String fqn, SnapshotTypeSchema stored, SnapshotTypeSchema current) {
        if (stored.getClass() != current.getClass()) {
            return fqn + ": kind changed " + kindName(stored) + " -> " + kindName(current);
        }
        if (stored instanceof SnapshotTypeSchema.Fields storedFields) {
            Map<String, SnapshotTypeSchema.Fields.Field> storedByName = new LinkedHashMap<>();
            storedFields.fields().forEach(it -> storedByName.put(it.name(), it));
            Map<String, SnapshotTypeSchema.Fields.Field> currentByName = new LinkedHashMap<>();
            ((SnapshotTypeSchema.Fields) current).fields().forEach(it -> currentByName.put(it.name(), it));
            List<String> details = new ArrayList<>();
            Set<String> removed = new LinkedHashSet<>(storedByName.keySet());
            removed.removeAll(currentByName.keySet());
            removed.forEach(it -> details.add("field removed: " + it));
            Set<String> added = new LinkedHashSet<>(currentByName.keySet());
            added.removeAll(storedByName.keySet());
            added.forEach(it -> details.add("field added: " + it));
            for (Map.Entry<String, SnapshotTypeSchema.Fields.Field> entry : storedByName.entrySet()) {
                String name = entry.getKey();
                SnapshotTypeSchema.Fields.Field storedField = entry.getValue();
                SnapshotTypeSchema.Fields.Field currentField = currentByName.get(name);
                if (currentField == null) {
                    continue;
                }
                if (!storedField.typeFqn().equals(currentField.typeFqn())) {
                    details.add("field " + name + " type changed: " + storedField.typeFqn() + " -> " + currentField.typeFqn());
                }
            }
            return fqn + ": " + String.join(", ", details);
        }
        else if (stored instanceof SnapshotTypeSchema.Enum storedEnum) {
            return fqn + ": enum constants changed " + storedEnum.constants() + " -> " + ((SnapshotTypeSchema.Enum) current).constants();
        }
        else {
            return fqn + ": schema changed";
        }
    }

    private static String kindName(SnapshotTypeSchema receiver) {
        if (receiver instanceof SnapshotTypeSchema.Fields) {
            return "FIELDS";
        }
        else if (receiver instanceof SnapshotTypeSchema.Enum) {
            return "ENUM";
        }
        else {
            return "SINGLETON";
        }
    }

    // consulo: Kotlin `entries.associateBy { it.fqn }`
    private static Map<String, SnapshotManifestEntry> associateByFqn(Set<SnapshotManifestEntry> entries) {
        Map<String, SnapshotManifestEntry> result = new LinkedHashMap<>();
        for (SnapshotManifestEntry entry : entries) {
            result.put(entry.fqn(), entry);
        }
        return result;
    }
}
