package org.jetbrains.bazel.commons;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public sealed interface RepoMapping permits RepoMapping.BzlmodRepoMapping, RepoMapping.RepoMappingDisabled {
    // consulo: data class with extra state (canonicalRepoNameToApparentName), so not a record
    final class BzlmodRepoMapping implements RepoMapping {
        private final Map<String, Path> myCanonicalRepoNameToLocalPath;
        private final Map<String, String> myApparentRepoNameToCanonicalName;
        private final Map<String, Path> myCanonicalRepoNameToPath;
        private final Set<String> myNonLocalCanonicalRepoNames;

        private final Map<String, String> myCanonicalRepoNameToApparentName;

        public BzlmodRepoMapping(
            Map<String, Path> canonicalRepoNameToLocalPath,
            Map<String, String> apparentRepoNameToCanonicalName,
            Map<String, Path> canonicalRepoNameToPath,
            Set<String> nonLocalCanonicalRepoNames
        ) {
            myCanonicalRepoNameToLocalPath = canonicalRepoNameToLocalPath;
            myApparentRepoNameToCanonicalName = apparentRepoNameToCanonicalName;
            myCanonicalRepoNameToPath = canonicalRepoNameToPath;
            myNonLocalCanonicalRepoNames = nonLocalCanonicalRepoNames;

            SortedMap<String, String> canonicalRepoNameToApparentName = new TreeMap<>();
            for (Map.Entry<String, String> entry : apparentRepoNameToCanonicalName.entrySet()) {
                canonicalRepoNameToApparentName.put(entry.getValue(), entry.getKey());
            }
            myCanonicalRepoNameToApparentName = canonicalRepoNameToApparentName;
        }

        public Map<String, Path> getCanonicalRepoNameToLocalPath() {
            return myCanonicalRepoNameToLocalPath;
        }

        public Map<String, String> getApparentRepoNameToCanonicalName() {
            return myApparentRepoNameToCanonicalName;
        }

        public Map<String, Path> getCanonicalRepoNameToPath() {
            return myCanonicalRepoNameToPath;
        }

        public Set<String> getNonLocalCanonicalRepoNames() {
            return myNonLocalCanonicalRepoNames;
        }

        public Map<String, String> getCanonicalRepoNameToApparentName() {
            return myCanonicalRepoNameToApparentName;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof BzlmodRepoMapping that
                && myCanonicalRepoNameToLocalPath.equals(that.myCanonicalRepoNameToLocalPath)
                && myApparentRepoNameToCanonicalName.equals(that.myApparentRepoNameToCanonicalName)
                && myCanonicalRepoNameToPath.equals(that.myCanonicalRepoNameToPath)
                && myNonLocalCanonicalRepoNames.equals(that.myNonLocalCanonicalRepoNames);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                myCanonicalRepoNameToLocalPath,
                myApparentRepoNameToCanonicalName,
                myCanonicalRepoNameToPath,
                myNonLocalCanonicalRepoNames
            );
        }

        @Override
        public String toString() {
            return "BzlmodRepoMapping(canonicalRepoNameToLocalPath=" + myCanonicalRepoNameToLocalPath
                + ", apparentRepoNameToCanonicalName=" + myApparentRepoNameToCanonicalName
                + ", canonicalRepoNameToPath=" + myCanonicalRepoNameToPath
                + ", nonLocalCanonicalRepoNames=" + myNonLocalCanonicalRepoNames + ")";
        }
    }

    final class RepoMappingDisabled implements RepoMapping {
        public static final RepoMappingDisabled INSTANCE = new RepoMappingDisabled();

        private RepoMappingDisabled() {
        }

        // consulo: data object equality
        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof RepoMappingDisabled;
        }

        @Override
        public int hashCode() {
            return RepoMappingDisabled.class.getName().hashCode();
        }

        @Override
        public String toString() {
            return "RepoMappingDisabled";
        }
    }

    record LocalRepositoryMapping(Map<String, Path> localRepositories) {
    }

    /**
     * A mapping without local repositories.
     * Use it for a generated artifact or an existence check, which the local override does not change.
     */
    LocalRepositoryMapping NoLocalRepositories = new LocalRepositoryMapping(Map.of());

    static LocalRepositoryMapping getLocalRepositories(RepoMapping receiver) {
        return new LocalRepositoryMapping(
            receiver instanceof BzlmodRepoMapping bzlmodRepoMapping ? bzlmodRepoMapping.getCanonicalRepoNameToLocalPath() : Map.of()
        );
    }

    static Map<String, String> getApparentRepoNameToCanonicalName(RepoMapping receiver) {
        if (receiver instanceof BzlmodRepoMapping bzlmodRepoMapping && !bzlmodRepoMapping.getApparentRepoNameToCanonicalName().isEmpty()) {
            return bzlmodRepoMapping.getApparentRepoNameToCanonicalName();
        }
        return Map.of("", "");
    }

    static Map<String, String> getCanonicalRepoNameToApparentName(RepoMapping receiver) {
        if (receiver instanceof BzlmodRepoMapping bzlmodRepoMapping && !bzlmodRepoMapping.getCanonicalRepoNameToApparentName().isEmpty()) {
            return bzlmodRepoMapping.getCanonicalRepoNameToApparentName();
        }
        return Map.of("", "");
    }

    static Map<String, Path> canonicalRepoNameToPath(RepoMapping receiver, Path workspaceRoot) {
        if (receiver instanceof BzlmodRepoMapping bzlmodRepoMapping && !bzlmodRepoMapping.getCanonicalRepoNameToPath().isEmpty()) {
            return bzlmodRepoMapping.getCanonicalRepoNameToPath();
        }
        return Map.of("", workspaceRoot);
    }
}
