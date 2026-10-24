package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import org.h2.mvstore.MVMap;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.function.ToLongFunction;

final class PeekingEntryCursor<K, V> {
    private final ToLongFunction<K> myKeyToLong;
    private final Iterator<Map.Entry<K, V>> myIterator;
    private Map.@Nullable Entry<K, V> myPeeked;

    PeekingEntryCursor(MVMap<K, V> map, ToLongFunction<K> keyToLong) {
        myKeyToLong = keyToLong;
        myIterator = map.entrySet().iterator();
    }

    private Map.@Nullable Entry<K, V> peek() {
        if (myPeeked == null && myIterator.hasNext()) {
            myPeeked = myIterator.next();
        }
        return myPeeked;
    }

    @Nullable V advanceTo(long target) {
        while (true) {
            Map.Entry<K, V> entry = peek();
            if (entry == null) {
                return null;
            }
            long entryKey = myKeyToLong.applyAsLong(entry.getKey());
            if (entryKey < target) {
                myPeeked = null;
            }
            else if (entryKey == target) {
                myPeeked = null;
                return entry.getValue();
            }
            else {
                return null;
            }
        }
    }
}
