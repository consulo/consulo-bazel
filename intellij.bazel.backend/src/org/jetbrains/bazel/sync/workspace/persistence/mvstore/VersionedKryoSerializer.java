package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import com.esotericsoftware.kryo.kryo5.Serializer;

/**
 * Base class for handwritten Kryo serializers for workspace snapshot.
 * Serializer versioning allow us to correctly reason about schema changes.
 * <p>
 * {@link #getBinaryFormatVersion()} Bump after each serializer modification
 */
public abstract class VersionedKryoSerializer<T> extends Serializer<T> {

    // TODO: currently only used inside test
    //  think of how to enforce it inside
    public abstract int getBinaryFormatVersion();
}
