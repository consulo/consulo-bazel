package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Registration;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer;
import com.esotericsoftware.kryo.kryo5.util.DefaultClassResolver;
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.util.MapReferenceResolver;
import com.esotericsoftware.kryo.kryo5.util.Pool;
import consulo.project.Project;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotManifest.SnapshotManifestEntry;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotManifest.SnapshotTypeSchema;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SnapshotKryo {
    // consulo: upstream internal top-level constants
    static final int KRYO_POOL_INSTANCES = 8;

    // covers container layout, manifest codec, Kryo configuration, builtin registrations and the
    // binary formats of all handwritten serializers - bump on any change to those
    static final int SNAPSHOT_FORMAT_VERSION = 10;

    private final SnapshotTypeUniverse myUniverse;

    private final Pool<Kryo> myPool;
    private final Pool<Kryo> myValuePool;

    public SnapshotKryo(SnapshotTypeUniverse universe) {
        myUniverse = universe;
        myPool = createPool(true);
        myValuePool = createPool(false);
    }

    public SnapshotTypeUniverse getUniverse() {
        return myUniverse;
    }

    public SnapshotManifest getManifest() {
        return myUniverse.getManifest();
    }

    public Pool<Kryo> getPool() {
        return myPool;
    }

    public Pool<Kryo> getValuePool() {
        return myValuePool;
    }

    private Pool<Kryo> createPool(boolean references) {
        return new Pool<>(
            /* threadSafe = */ true,
            /* softReferences = */ false,
            /* maximumCapacity = */ KRYO_POOL_INSTANCES
        ) {
            @Override
            protected Kryo create() {
                return createKryo(references);
            }
        };
    }

    private Kryo createKryo(boolean references) {
        Kryo kryo = new Kryo(
            /* classResolver = */ new SnapshotClassResolver(),
            /* referenceResolver = */ references ? new MapReferenceResolver() : null
        );
        kryo.setReferences(references);
        kryo.setRegistrationRequired(true);
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        // register builtins
        SnapshotSerializers.registerBuiltinTypes(kryo);

        // register open type set
        for (SnapshotTypeRegistration registration : myUniverse.getRegistrations()) {
            int registrationId = registration.registrationId();
            Class<?> type = registration.type();
            VersionedKryoSerializer<?> serializer = registration.serializer();
            Registration kryoRegistration;
            if (serializer != null) {
                kryoRegistration = kryo.register(type, serializer, registrationId);
            }
            else {
                kryoRegistration = kryo.register(type, registrationId);
            }
            if (kryoRegistration.getSerializer() instanceof FieldSerializer<?> fieldSerializer) {
                SnapshotSerializers.rebindCollectionFieldSerializers(fieldSerializer);
            }
        }
        return kryo;
    }

    public record SnapshotTypeRegistration(
        int registrationId,
        Class<?> type,
        @Nullable VersionedKryoSerializer<?> serializer
    ) {
    }

    public static class SnapshotTypeUniverse {
        private final List<SnapshotTypeRegistration> myRegistrations;
        private final SnapshotManifest myManifest;
        private final Reference2IntMap<Class<?>> myType2Id;

        public SnapshotTypeUniverse(
            List<SnapshotTypeRegistration> registrations,
            SnapshotManifest manifest
        ) {
            myRegistrations = registrations;
            myManifest = manifest;
            Map<Class<?>, Integer> type2Id = new LinkedHashMap<>();
            for (SnapshotTypeRegistration it : registrations) {
                type2Id.put(it.type(), it.registrationId());
            }
            myType2Id = new Reference2IntOpenHashMap<>(type2Id);
        }

        public List<SnapshotTypeRegistration> getRegistrations() {
            return myRegistrations;
        }

        public SnapshotManifest getManifest() {
            return myManifest;
        }

        public Reference2IntMap<Class<?>> getType2Id() {
            return myType2Id;
        }

        public static SnapshotTypeUniverse buildFromProject(Project project) {
            List<WorkspaceTypeEntry> contributed = new ArrayList<>();
            for (WorkspaceTypeContributor it : WorkspaceTypeContributor.getEpName().getExtensionList()) {
                contributed.addAll(it.contribute(project));
            }
            return build(contributed);
        }

        public static SnapshotTypeUniverse build(List<WorkspaceTypeEntry> contributed) {
            SnapshotManifest manifest = SnapshotTypeSchemas.buildManifest(contributed);
            Map<String, Class<?>> typeByFqn = new LinkedHashMap<>();
            for (WorkspaceTypeEntry it : contributed) {
                typeByFqn.put(it.fqn(), it.type());
            }
            List<SnapshotTypeRegistration> registrations = new ArrayList<>(manifest.entries().size());
            for (SnapshotManifestEntry entry : manifest.entries()) {
                Class<?> type = Objects.requireNonNull(typeByFqn.get(entry.fqn()));
                SnapshotTypeSchema schema = entry.schema();
                registrations.add(new SnapshotTypeRegistration(
                    entry.registrationId(),
                    type,
                    schema instanceof SnapshotTypeSchema.Singleton ? SnapshotSerializers.singletonSerializerFor(type) : null
                ));
            }
            return new SnapshotTypeUniverse(registrations, manifest);
        }
    }
}

final class SnapshotClassResolver extends DefaultClassResolver {
    /**
     * Specify what types should be identified by their interface type ID,
     * it's necessary to avoid explicitly registering specific implementations of type like `Path`.
     * <p>
     * <b>NOTE:</b> Each type here must register kryo default serializer.
     */
    private static final Class<?>[] OPEN_TYPES = new Class<?>[]{Path.class, Label.class};

    @Override
    public @Nullable Registration getRegistration(Class type) {
        for (Class<?> openType : OPEN_TYPES) {
            if (openType.isAssignableFrom(type)) {
                return super.getRegistration(openType);
            }
        }
        return super.getRegistration(type);
    }
}
