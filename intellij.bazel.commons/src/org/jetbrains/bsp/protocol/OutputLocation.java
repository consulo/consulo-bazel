package org.jetbrains.bsp.protocol;

import com.dynatrace.hash4j.hashing.HashStream128;
import com.dynatrace.hash4j.hashing.HashValue128;
import com.dynatrace.hash4j.hashing.Hashing;
import consulo.util.interner.Interner;
import consulo.util.io.FileUtil;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface OutputLocation {
    // consulo: not a record, upstream has a private constructor (instances are interned)
    /**
     * Represent location root under {@code bazel-out}, e.g. "k8-fastbuild/bin"
     */
    final class OutputRoot {
        private final List<String> mySegments;
        private final String myPath;

        private OutputRoot(List<String> segments) {
            mySegments = segments;
            myPath = String.join("/", segments);
        }

        public List<String> getSegments() {
            return mySegments;
        }

        public String getPath() {
            return myPath;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof OutputRoot that && mySegments.equals(that.mySegments);
        }

        @Override
        public int hashCode() {
            return mySegments.hashCode();
        }

        @Override
        public String toString() {
            return "OutputRoot(segments=" + mySegments + ")";
        }

        private static final Interner<OutputRoot> INTERNER = Interner.createWeakInterner();

        public static OutputRoot of(List<String> segments) {
            return INTERNER.intern(new OutputRoot(segments));
        }
    }

    // main workspace relative path
    record Workspace(String relativePath) implements OutputLocation {
    }

    // example: bazel-out/darwin_arm64-fastbuild/a/b/h.java
    // root -> bazel-out/darwin_arm64-fastbuild/
    // relativePath -> a/b/h.java
    record Output(OutputRoot root, String relativePath) implements OutputLocation {
    }

    // external repository relative path
    // repoName is canonical
    // siblingLayout marks the `../<repo>` form of `--experimental_sibling_repository_layout`
    // the default is the `external/<repo>` form
    record External(String repoName, String relativePath, boolean siblingLayout) implements OutputLocation {
        public External(String repoName, String relativePath) {
            this(repoName, relativePath, false);
        }
    }

    // host path, e.g. /usr/bin/clang
    record Host(String absolutePath) implements OutputLocation {
    }

    static boolean isGenerated(OutputLocation receiver) {
        return switch (receiver) {
            case Output output -> true;
            case External external -> external.relativePath().startsWith("bazel-out/");
            default -> false;
        };
    }

    static boolean isSource(OutputLocation receiver) {
        return !isGenerated(receiver);
    }

    static boolean isExternal(OutputLocation receiver) {
        return receiver instanceof External;
    }

    static boolean isInMainWorkspace(OutputLocation receiver) {
        return receiver instanceof Workspace;
    }

    static boolean isUserCode(OutputLocation receiver, RepoMapping repoMapping) {
        Map<String, Path> localRepositories = RepoMapping.getLocalRepositories(repoMapping).localRepositories();
        return switch (receiver) {
            case Workspace workspace -> true;
            case External external -> localRepositories.containsKey(external.repoName());
            case Output output -> {
                String repo = externalRepoOfOutput(output);
                yield repo == null || localRepositories.containsKey(repo);
            }
            case Host host -> false;
        };
    }

    // canonical repository name when the output belongs to an external target, else null
    private static @Nullable String externalRepoOfOutput(Output receiver) {
        if (!receiver.relativePath().startsWith("external/")) {
            return null;
        }
        String withoutPrefix = receiver.relativePath().substring("external/".length());
        int slash = withoutPrefix.indexOf('/');
        String repo = slash == -1 ? withoutPrefix : withoutPrefix.substring(0, slash);
        return !repo.isEmpty() ? repo : null;
    }

    static String toExecrootPath(OutputLocation receiver) {
        return switch (receiver) {
            case External external -> {
                if (external.siblingLayout()) {
                    yield joinNonEmpty("..", external.repoName(), external.relativePath());
                }
                else {
                    yield joinNonEmpty("external", external.repoName(), external.relativePath());
                }
            }

            case Output output -> joinNonEmpty("bazel-out", output.root().getPath(), output.relativePath());
            case Workspace workspace -> workspace.relativePath();
            case Host host -> host.absolutePath();
        };
    }

    // consulo: internal upstream, interface methods cannot be package-private
    static String joinNonEmpty(String... parts) {
        return Arrays.stream(parts).filter(it -> !it.isEmpty()).collect(Collectors.joining("/"));
    }


    /**
     * Obtain relative path, for real path resolution use {@link OutputLocationResolver}
     */
    static Path getRelativeNioPath(OutputLocation receiver) {
        return switch (receiver) {
            case Workspace workspace -> Path.of(workspace.relativePath());
            case Output output -> Path.of(output.relativePath());
            case External external -> Path.of(external.relativePath());
            case Host host -> Path.of(host.absolutePath());
        };
    }

    static OutputLocation mapPath(OutputLocation receiver, Function<Path, Path> transform) {
        Function<Path, String> toOutputRelativePath = path -> {
            // consulo: Kotlin invariantSeparatorsPathString
            String it = path.toString().replace(path.getFileSystem().getSeparator(), "/");
            // TODO consulo: switch to consulo.util.io.OSAgnosticPathUtil.isAbsolute (ported to the platform, not yet in the published snapshot)
            if (FileUtil.isAbsolutePlatformIndependent(it)) {
                throw new IllegalArgumentException("Failed requirement.");
            }
            return it;
        };

        BiFunction<OutputLocation, Path, OutputLocation> withNioPath = (location, path) -> switch (location) {
            case Workspace workspace -> new Workspace(toOutputRelativePath.apply(path));
            case Output output -> new Output(output.root(), toOutputRelativePath.apply(path));
            case External external -> new External(external.repoName(), toOutputRelativePath.apply(path), external.siblingLayout());
            case Host host -> {
                String it = path.toString();
                if (!FileUtil.isAbsolutePlatformIndependent(it)) {
                    throw new IllegalArgumentException("Failed requirement.");
                }
                yield new Host(it);
            }
        };

        return withNioPath.apply(receiver, transform.apply(getRelativeNioPath(receiver)));
    }


    interface OutputLocationCollection {
        boolean isEmpty();

        Stream<OutputLocation> getOutputLocations();

        @SuppressWarnings("unused")
        default OutputLocation[] arrayOfLocations() {
            return getOutputLocations().toArray(OutputLocation[]::new);
        }

        OutputLocationCollection EMPTY = new OutputLocationCollection() {
            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public Stream<OutputLocation> getOutputLocations() {
                return Stream.of();
            }

            @Override
            public boolean equals(@Nullable Object other) {
                return other instanceof OutputLocationCollection collection && collection.isEmpty();
            }

            @Override
            public int hashCode() {
                return EMPTY_CONTENT_HASH.getAsInt();
            }
        };
    }

    interface OutputLocationResolver {

        /**
         * Resolves the {@code location} to the file it points to.
         * Respects local repositories override according to {@code localOverride}.
         *
         * @param location      output location
         * @param localOverride local repository override use to resolve files
         * @return {@code null} when the stored path is not a valid path on this platform.
         */
        @Nullable Path resolve(OutputLocation location, @Nullable LocalRepositoryMapping localOverride);

        default @Nullable Path resolve(OutputLocation location) {
            return resolve(location, null);
        }
    }

    // consulo: private upstream, interface fields are always public
    HashValue128 EMPTY_CONTENT_HASH = contentHash(List.of());

    static boolean contentEquals(OutputLocationCollection receiver, OutputLocationCollection other) {
        return receiver.getOutputLocations().collect(Collectors.toCollection(HashSet::new))
            .equals(other.getOutputLocations().collect(Collectors.toCollection(HashSet::new)));
    }

    static int contentHashCode(OutputLocationCollection receiver) {
        return contentHash(receiver).getAsInt();
    }

    static HashValue128 contentHash(OutputLocationCollection receiver) {
        Set<OutputLocation> locations = receiver.getOutputLocations().collect(Collectors.toCollection(HashSet::new));
        return contentHash(locations);
    }

    // addition is commutative, using that property the same hash will be produced regardless of input order
    // that mean we can avoid expensive sorting of location collection
    static HashValue128 contentHash(Iterable<OutputLocation> locations) {
        HashStream128 stream = Hashing.xxh3_128().hashStream();
        long hi = 0L;
        long lo = 0L;
        for (OutputLocation location : locations) {
            HashValue128 hash = hash(location, stream.reset());
            hi += hash.getMostSignificantBits();
            lo += hash.getLeastSignificantBits();
        }
        return new HashValue128(hi, lo);
    }

    private static HashValue128 hash(OutputLocation receiver, HashStream128 stream) {
        switch (receiver) {
            case Workspace workspace -> {
                stream.putByte((byte) 0);
                stream.putString(workspace.relativePath());
            }

            case Output output -> {
                stream.putByte((byte) 1);
                stream.putString(output.root().getPath());
                stream.putString(output.relativePath());
            }

            case External external -> {
                stream.putByte((byte) 2);
                stream.putString(external.repoName());
                stream.putString(external.relativePath());
                stream.putBoolean(external.siblingLayout());
            }

            case Host host -> {
                stream.putByte((byte) 3);
                stream.putString(host.absolutePath());
            }
        }
        return stream.get();
    }
}
