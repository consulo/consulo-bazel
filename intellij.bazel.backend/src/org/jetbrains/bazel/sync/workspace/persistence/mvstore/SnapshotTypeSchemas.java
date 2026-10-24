package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotManifest.SnapshotManifestEntry;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotManifest.SnapshotTypeSchema;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

// consulo: internal upstream object, public for the public top-level constant
public final class SnapshotTypeSchemas {
    // start from larger registration id to keep room for builtin types
    public static final int KRYO_REGISTER_BEGIN_ID = 256;

    private SnapshotTypeSchemas() {
    }

    static SnapshotManifest buildManifest(List<WorkspaceTypeEntry> contributed) {
        List<String> problems = new ArrayList<>();
        Map<String, List<WorkspaceTypeEntry>> byFqn = new LinkedHashMap<>();
        for (WorkspaceTypeEntry entry : contributed) {
            byFqn.computeIfAbsent(entry.fqn(), it -> new ArrayList<>()).add(entry);
        }
        byFqn.forEach((fqn, entries) -> {
            if (entries.size() > 1) {
                problems.add("duplicate contribution of `" + fqn + "`");
            }
        });

        Map<String, SnapshotTypeSchema> schemas = new LinkedHashMap<>();
        for (WorkspaceTypeEntry entry : contributed) {
            String fqn = entry.fqn();
            Class<?> type = entry.type();
            if (type.isAnonymousClass() || type.isLocalClass() || type.isSynthetic()) {
                problems.add("`" + fqn + "`: anonymous, local and synthetic (lambda) classes cannot be persisted");
                continue;
            }
            SnapshotTypeSchema schema;
            if (type.isEnum()) {
                Object[] constants = Objects.requireNonNull(type.getEnumConstants());
                schema = new SnapshotTypeSchema.Enum(Arrays.stream(constants).map(it -> ((java.lang.Enum<?>) it).name()).toList());
            }
            else if (isKotlinObject(type)) {
                schema = SnapshotTypeSchema.Singleton.INSTANCE;
            }
            else if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
                problems.add("`" + fqn + "`: collection types cannot be contributed");
                continue;
            }
            else {
                schema = new SnapshotTypeSchema.Fields(collectSerializedFields(type));
            }
            schemas.put(fqn, schema);
        }
        if (!problems.isEmpty()) {
            throw new SnapshotUniverseValidationException(problems);
        }

        SortedSet<SnapshotManifestEntry> entries = new TreeSet<>(SnapshotManifestEntry.COMPARATOR);
        for (int index = 0; index < contributed.size(); index++) {
            WorkspaceTypeEntry entry = contributed.get(index);
            entries.add(new SnapshotManifestEntry(
                KRYO_REGISTER_BEGIN_ID + index,
                entry.fqn(),
                Objects.requireNonNull(schemas.get(entry.fqn()))
            ));
        }
        return new SnapshotManifest(
            entries
        );
    }

    private static List<SnapshotTypeSchema.Fields.Field> collectSerializedFields(Class<?> type) {
        List<SnapshotTypeSchema.Fields.Field> collected = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || field.isSynthetic() || Modifier.isTransient(modifiers)) {
                    continue;
                }
                collected.add(new SnapshotTypeSchema.Fields.Field(field.getName(), field.getType().getName()));
            }
            current = current.getSuperclass();
        }
        collected.sort(Comparator.comparing(SnapshotTypeSchema.Fields.Field::name));
        return collected;
    }

    // consulo: matches Java singletons with a `public static final INSTANCE` field too
    private static boolean isKotlinObject(Class<?> type) {
        Field instance = Arrays.stream(type.getDeclaredFields()).filter(it -> it.getName().equals("INSTANCE")).findFirst().orElse(null);
        if (instance == null) {
            return false;
        }
        return Modifier.isStatic(instance.getModifiers()) && Modifier.isFinal(instance.getModifiers()) && instance.getType() == type;
    }
}

final class SnapshotUniverseValidationException extends IllegalStateException {
    SnapshotUniverseValidationException(List<String> problems) {
        super("Workspace snapshot type universe is not serializable:\n" + String.join("\n", problems));
    }
}
