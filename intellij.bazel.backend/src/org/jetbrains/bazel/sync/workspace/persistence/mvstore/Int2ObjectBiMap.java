package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jspecify.annotations.Nullable;

public record Int2ObjectBiMap<T>(
    Int2ObjectMap<T> forward,
    Object2IntMap<T> reverse
) {
    public Int2ObjectBiMap() {
        this(new Int2ObjectOpenHashMap<>(), new Object2IntOpenHashMap<>());
    }

    // consulo: upstream `operator fun set`
    public void set(int key, T value) {
        forward.put(key, value);
        reverse.put(value, key);
    }

    public @Nullable T get(int key) {
        return forward.get(key);
    }

    public int getReverseOrDefault(T key, int defaultValue) {
        return reverse.getOrDefault(key, defaultValue);
    }
}
