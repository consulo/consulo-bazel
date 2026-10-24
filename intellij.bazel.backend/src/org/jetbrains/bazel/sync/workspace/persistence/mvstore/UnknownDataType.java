package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

import java.nio.ByteBuffer;

// used only for unopened MVMap pruning
public final class UnknownDataType extends BasicDataType<byte[]> {
    public static final UnknownDataType INSTANCE = new UnknownDataType();

    private UnknownDataType() {
    }

    @Override
    public boolean isMemoryEstimationAllowed() {
        return false;
    }

    @Override
    public int getMemory(byte[] obj) {
        return obj.length;
    }

    @Override
    public byte[][] createStorage(int size) {
        return new byte[size][];
    }

    @Override
    public void write(WriteBuffer buff, byte[] obj) {
        buff.put(obj);
    }

    @Override
    public byte[] read(ByteBuffer buff) {
        byte[] data = new byte[buff.remaining()];
        buff.get(data);
        return data;
    }
}
