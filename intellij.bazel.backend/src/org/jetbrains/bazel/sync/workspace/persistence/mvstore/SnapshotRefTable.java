package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import org.h2.mvstore.MVMap;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// global generation object table, value frames keep type information (`writeClassAndObject`),
// so a single table can hold objects of any type

// consulo: upstream Kotlin top-level constants
final class SnapshotRefTable {
    static final String REF_TABLE_WRITE_KEY = "snapshot.refTable.write";
    static final String REF_TABLE_READ_KEY = "snapshot.refTable.read";

    private SnapshotRefTable() {
    }
}

final class RefTableWriter {
    // keyed by value equality, equal objects share one id
    private final ConcurrentHashMap<Object, Integer> myIds = new ConcurrentHashMap<>();
    private final AtomicInteger myNextId = new AtomicInteger(1);

    int idFor(Object value) {
        return myIds.computeIfAbsent(value, it -> myNextId.getAndIncrement());
    }

    // ascending by id, as required by MVMap.append
    List<Pair<Integer, Object>> sortedEntries() {
        return myIds.entrySet().stream()
            .map(it -> Pair.create(it.getValue(), it.getKey()))
            .sorted(Comparator.comparingInt(it -> it.getFirst()))
            .toList();
    }
}

final class RefTableReader {
    private final MVMap<Integer, ValueFrame> myRefs;
    private final Function<ValueFrame, Object> myDecode;
    private final ConcurrentMap<Integer, Object> myCache = ContainerUtil.createConcurrentWeakValueMap();

    RefTableReader(
        MVMap<Integer, ValueFrame> refs,
        Function<ValueFrame, Object> decode
    ) {
        myRefs = refs;
        myDecode = decode;
    }

    Object get(int id) {
        // consulo: Kotlin ConcurrentMap.getOrPut
        Object cached = myCache.get(id);
        if (cached != null) {
            return cached;
        }
        ValueFrame frame = myRefs.get(id);
        if (frame == null) {
            throw new IllegalStateException("ref table has no entry for id " + id);
        }
        Object decoded = myDecode.apply(frame);
        Object previous = myCache.putIfAbsent(id, decoded);
        return previous != null ? previous : decoded;
    }
}
