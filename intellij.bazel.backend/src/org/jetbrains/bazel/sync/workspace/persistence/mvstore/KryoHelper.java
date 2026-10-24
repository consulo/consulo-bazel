package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.jspecify.annotations.Nullable;
import com.esotericsoftware.kryo.kryo5.util.Pool;

import java.util.function.Function;

// consulo: upstream Kotlin top-level extension function
public final class KryoHelper {
    private KryoHelper() {
    }

    public static <T, R extends @Nullable Object> R use(Pool<T> receiver, Function<T, R> fn) {
        T obj = receiver.obtain();
        try {
            return fn.apply(obj);
        }
        finally {
            receiver.free(obj);
        }
    }
}
