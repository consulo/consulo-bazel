package org.jetbrains.bazel.sync.workspace.snapshot;

import com.dynatrace.hash4j.hashing.HashValue128;
import com.google.devtools.intellij.aspect.Common;
import consulo.util.interner.Interner;
import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

// consulo: internal upstream, public because the snapshot serializers use it
public final class InterningOutputLocationCollection implements OutputLocationCollection {
    private final List<OutputLocation> myLocations;

    // consulo: lazy(LazyThreadSafetyMode.PUBLICATION)
    private final LazyValue<HashValue128> myContentHash;

    private InterningOutputLocationCollection(List<OutputLocation> locations) {
        myLocations = locations;
        myContentHash = LazyValue.notNull(() -> OutputLocation.contentHash(myLocations));
    }

    public List<OutputLocation> getLocations() {
        return myLocations;
    }

    @Override
    public boolean isEmpty() {
        return myLocations.isEmpty();
    }

    @Override
    public Stream<OutputLocation> getOutputLocations() {
        return myLocations.stream();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof InterningOutputLocationCollection that) {
            return myLocations.size() == that.myLocations.size() && myContentHash.get().equals(that.myContentHash.get());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return myContentHash.get().getAsInt();
    }

    private static final Interner<OutputLocation> INTERNER = Interner.createWeakInterner();

    public static InterningOutputLocationCollection create(Iterable<OutputLocation> locations) {
        List<OutputLocation> interned = new ArrayList<>();
        for (OutputLocation it : locations) {
            interned.add(INTERNER.intern(it));
        }
        return new InterningOutputLocationCollection(interned);
    }

    public static final class OutputLocationCollectionBuilder {
        private OutputLocationCollectionBuilder() {
        }

        public static CompletableFuture<OutputLocationCollection> build(List<Common.ArtifactLocation> locations, OutputLocationParser parser) {
            return parser.parse(locations).thenApply(OutputLocationCollectionBuilder::ofLocations);
        }

        public static CompletableFuture<OutputLocationCollection> buildExecroot(List<String> locations, OutputLocationParser parser) {
            return CompletableFuture.completedFuture(ofLocations(parser.parseExecrootPath(locations)));
        }

        public static OutputLocationCollection ofLocations(List<OutputLocation> locations) {
            return locations.isEmpty() ? OutputLocationCollection.EMPTY : InterningOutputLocationCollection.create(locations);
        }
    }
}
