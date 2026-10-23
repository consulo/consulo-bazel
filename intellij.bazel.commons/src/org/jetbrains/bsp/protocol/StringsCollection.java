package org.jetbrains.bsp.protocol;

import consulo.util.interner.Interner;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Immutable list of strings that is interned as a whole, e.g. compiler options or program arguments.
 * <p>
 * Most targets share the same lists, so equal collections share one instance in memory,
 * and the workspace snapshot stores each distinct collection once per generation.
 */
public final class StringsCollection {
    private final String[] myStrings;

    private final int myContentHash;

    private StringsCollection(String[] strings) {
        myStrings = strings;
        myContentHash = Arrays.hashCode(strings);
    }

    public int getSize() {
        return myStrings.length;
    }

    public boolean isEmpty() {
        return myStrings.length == 0;
    }

    public String get(int index) {
        return myStrings[index];
    }

    public List<String> asList() {
        return Arrays.asList(myStrings);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof StringsCollection that && myContentHash == that.myContentHash && Arrays.equals(myStrings, that.myStrings);
    }

    @Override
    public int hashCode() {
        return myContentHash;
    }

    @Override
    public String toString() {
        return Arrays.toString(myStrings);
    }

    private static final Interner<StringsCollection> ARR_INTERNER = Interner.createWeakInterner();
    private static final Interner<String> STR_INTERNER = Interner.createWeakInterner();

    public static final StringsCollection EMPTY = new StringsCollection(new String[0]);

    public static StringsCollection of(Collection<String> strings) {
        if (strings.isEmpty()) {
            return EMPTY;
        }

        return ARR_INTERNER.intern(new StringsCollection(strings.stream().map(STR_INTERNER::intern).toArray(String[]::new)));
    }
}
