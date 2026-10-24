package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import org.h2.mvstore.MVMap;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: right now only specialized serializers use string table,
//  we could add some type marker (e.g. PoolableString) so open types can use it, e.g., for compiler args

// consulo: upstream Kotlin top-level constants
final class SnapshotStringTable {
    static final String STRING_TABLE_WRITE_KEY = "snapshot.stringTable.write";
    static final String STRING_TABLE_READ_KEY = "snapshot.stringTable.read";

    private SnapshotStringTable() {
    }
}

final class StringTableWriter {
    private final ConcurrentHashMap<String, Integer> myIds = new ConcurrentHashMap<>();
    private final AtomicInteger myNextId = new AtomicInteger(1);

    int idFor(String value) {
        return myIds.computeIfAbsent(value, it -> myNextId.getAndIncrement());
    }

    // ascending by id, as required by MVMap.append
    List<Pair<Integer, String>> sortedEntries() {
        return myIds.entrySet().stream()
            .map(it -> Pair.create(it.getValue(), it.getKey()))
            .sorted(Comparator.comparingInt(it -> it.getFirst()))
            .toList();
    }
}

final class StringTableReader {
    private final MVMap<Integer, String> myStrings;
    private final ConcurrentMap<Integer, String> myCache = ContainerUtil.createConcurrentWeakValueMap();

    StringTableReader(MVMap<Integer, String> strings) {
        myStrings = strings;
    }

    String get(int id) {
        return myCache.computeIfAbsent(id, it -> {
            String value = myStrings.get(it);
            if (value == null) {
                throw new IllegalStateException("string table has no entry for id " + it);
            }
            return value;
        });
    }
}
