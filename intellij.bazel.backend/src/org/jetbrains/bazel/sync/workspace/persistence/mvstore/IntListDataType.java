package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

import java.nio.ByteBuffer;

final class IntListDataType extends BasicDataType<IntArrayList> {
    static final IntListDataType INSTANCE = new IntListDataType();

    private IntListDataType() {
    }

    @Override
    public int getMemory(IntArrayList obj) {
        return obj.size() * Integer.BYTES;
    }

    @Override
    public void write(WriteBuffer buff, IntArrayList obj) {
        buff.putVarInt(obj.size());

        // `grow` inside `WriteBuffer` is private, so just manually insert elements
        // not ideal but lists using this codec usually doesn't have more than ~20 elements
        for (int n = 0; n < obj.size(); n++) {
            buff.putInt(obj.getInt(n));
        }
    }

    @Override
    public IntArrayList read(ByteBuffer buff) {
        int size = DataUtils.readVarInt(buff);
        int[] array = new int[size];
        buff.asIntBuffer().get(array);
        buff.position(buff.position() + size * Integer.BYTES);
        return new IntArrayList(array);
    }

    @Override
    public IntArrayList[] createStorage(int size) {
        return new IntArrayList[size];
    }
}
