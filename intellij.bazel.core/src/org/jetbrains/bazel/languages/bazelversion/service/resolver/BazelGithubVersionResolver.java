package org.jetbrains.bazel.languages.bazelversion.service.resolver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionImpl;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.http.HttpRequests;
import consulo.project.Project;
import consulo.util.lang.SemVer;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral;
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionResolver;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

@ExtensionImpl
class BazelGithubVersionResolver implements BazelVersionResolver {
    private final Gson myGithubGson =
        new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private final String myId = ID;
    private final String myName = "Github";

    public BazelGithubVersionResolver() {
    }

    public Gson getGithubGson() {
        return myGithubGson;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public String getName() {
        return myName;
    }

    public @Nullable String getForkOfSupportedBazelVersion(@Nullable BazelVersionLiteral version) {
        if (version instanceof BazelVersionLiteral.Forked forked) {
            return forked.fork();
        }
        else if (version instanceof BazelVersionLiteral.Specific || version == null) {
            return DEFAULT_FORK_NAME;
        }
        else {
            return null;
        }
    }

    @Override
    public CompletableFuture<@Nullable BazelVersionLiteral> resolveLatestBazelVersion(
        Project project,
        @Nullable BazelVersionLiteral currentVersion
    ) {
        BazelGithubGlobalCacheService githubService = project.getInstance(BazelGithubGlobalCacheService.class);
        githubService.optimizeCache();
        String fork = getForkOfSupportedBazelVersion(currentVersion);
        if (fork == null) {
            return CompletableFuture.completedFuture(null);
        }
        return githubService.getCachedVersion(fork, System.currentTimeMillis(), (name, ignored) ->
                getLatestBazelVersion(project, name)
            )
            .thenApply(version -> {
                if (version == null) {
                    return null;
                }
                return currentVersion != null ? BazelVersionLiteral.withNewVersionWhenPossible(currentVersion, version) : null;
            });
    }

    // consulo: upstream suspend function; the blocking HTTP request runs on the application executor
    public CompletableFuture<@Nullable String> getLatestBazelVersion(Project project, String forkName) {
        return CompletableFuture.supplyAsync(() -> {
            String response;
            try {
                response =
                    HttpRequests
                        .request("https://api.github.com/repos/" + forkName + "/" + DEFAULT_REPO_NAME + "/releases")
                        .productNameAsUserAgent()
                        .readString(null);
            }
            catch (IOException ignored) {
                return null;
            }
            BazelGithubGlobalCacheService cacheService = project.getInstance(BazelGithubGlobalCacheService.class);

            GithubReleaseResponse[] releases;
            try {
                releases = Objects.requireNonNull(myGithubGson.fromJson(response, GithubReleaseResponse[].class));
            }
            catch (JsonSyntaxException ignored) {
                return null;
            }
            // consulo: Kotlin compareBy with a nullable selector sorts null first
            Comparator<GithubReleaseResponse> comparator =
                Comparator.<GithubReleaseResponse, @Nullable SemVer>comparing(
                    it -> {
                        String tagName = it.getTagName();
                        return tagName != null ? SemVer.parseFromText(tagName) : null;
                    },
                    Comparator.nullsFirst(Comparator.naturalOrder())
                );
            GithubReleaseResponse latest =
                Arrays.stream(releases)
                    .filter(it -> {
                        if (!cacheService.getIncludePrelease() && Boolean.TRUE.equals(it.getPrerelease())) {
                            return false;
                        }
                        String tagName = it.getTagName();
                        if (!cacheService.getIncludeReleaseCandidate() && tagName != null && tagName.contains("-rc")) {
                            return false;
                        }
                        return true;
                    })
                    .max(comparator)
                    .orElse(null);
            return latest != null ? latest.getTagName() : null;
        }, Application.get().getInstance(ApplicationConcurrency.class).getExecutorService());
    }

    public static final String ID = "github";
    public static final String DEFAULT_FORK_NAME = "bazelbuild";
    public static final String DEFAULT_REPO_NAME = "bazel";

    // consulo: upstream data class with var properties -> mutable bean deserialized by Gson
    public static class GithubReleaseResponse {
        @SerializedName("id")
        private @Nullable Integer myId = null;
        @SerializedName("tag_name")
        private @Nullable String myTagName = null;
        @SerializedName("name")
        private @Nullable String myName = null;
        @SerializedName("prerelease")
        private @Nullable Boolean myPrerelease = null;

        public GithubReleaseResponse() {
        }

        public GithubReleaseResponse(
            @Nullable Integer id,
            @Nullable String tagName,
            @Nullable String name,
            @Nullable Boolean prerelease
        ) {
            myId = id;
            myTagName = tagName;
            myName = name;
            myPrerelease = prerelease;
        }

        public @Nullable Integer getId() {
            return myId;
        }

        public void setId(@Nullable Integer id) {
            myId = id;
        }

        public @Nullable String getTagName() {
            return myTagName;
        }

        public void setTagName(@Nullable String tagName) {
            myTagName = tagName;
        }

        public @Nullable String getName() {
            return myName;
        }

        public void setName(@Nullable String name) {
            myName = name;
        }

        public @Nullable Boolean getPrerelease() {
            return myPrerelease;
        }

        public void setPrerelease(@Nullable Boolean prerelease) {
            myPrerelease = prerelease;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GithubReleaseResponse that)) {
                return false;
            }
            return Objects.equals(myId, that.myId) &&
                Objects.equals(myTagName, that.myTagName) &&
                Objects.equals(myName, that.myName) &&
                Objects.equals(myPrerelease, that.myPrerelease);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myId, myTagName, myName, myPrerelease);
        }

        @Override
        public String toString() {
            return "GithubReleaseResponse(id=" + myId + ", tagName=" + myTagName + ", name=" + myName +
                ", prerelease=" + myPrerelease + ")";
        }
    }

    // consulo: upstream top-level declaration of BazelGithubVersionResolver.kt
    static final Duration GITHUB_CACHE_EVICTION_TIME = Duration.ofHours(24);
}

// consulo: a package-private top-level class, because Consulo's annotation processor cannot handle nested @ServiceImpl classes
@State(
    name = "BazelGithubResolverVersionCache",
    // consulo: StoragePathMacros.CACHE_FILE does not exist in Consulo, the cache is stored in the workspace file
    storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)
)
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
class BazelGithubGlobalCacheService implements PersistentStateComponent<BazelGithubGlobalCacheService.State> {
    // consulo: upstream BaseState with map()/property() delegates -> plain bean
    public static class State {
        // consulo: upstream @XMap
        @MapAnnotation
        public Map<String, ForkState> forks = new LinkedHashMap<>();
        public boolean includePrelease = false;
        public boolean includeReleaseCandidate = false;
    }

    @Tag("fork_state")
    public static class ForkState {
        public String name = "";
        public String version = "";
        public long lastUpdated = 0;

        public ForkState() {
        }

        public ForkState(String name, String version, long lastUpdated) {
            this.name = name;
            this.version = version;
            this.lastUpdated = lastUpdated;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ForkState that)) {
                return false;
            }
            return lastUpdated == that.lastUpdated && name.equals(that.name) && version.equals(that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, version, lastUpdated);
        }

        @Override
        public String toString() {
            return "ForkState(name=" + name + ", version=" + version + ", lastUpdated=" + lastUpdated + ")";
        }
    }

    sealed interface CachedVersion permits CachedVersion.Cached, CachedVersion.Unavailable {
        record Cached(String version, long lastUpdated) implements CachedVersion {
        }

        final class Unavailable implements CachedVersion {
            public static final Unavailable INSTANCE = new Unavailable();

            private Unavailable() {
            }

            @Override
            public String toString() {
                return "Unavailable";
            }
        }
    }

    // consulo: Caffeine is not available in Consulo. The AsyncCache with a per-entry Expiry is a concurrent map of futures:
    // a completed entry expires GITHUB_CACHE_EVICTION_TIME after its lastUpdated (Unavailable and failed entries immediately),
    // expired entries are replaced on access and removed by optimizeCache(). The upstream executor (coroutine scope) is not needed.
    private final ConcurrentMap<String, CompletableFuture<CachedVersion>> myCache = new ConcurrentHashMap<>();

    private boolean myIncludePrelease = false;
    private boolean myIncludeReleaseCandidate = false;

    @Inject
    public BazelGithubGlobalCacheService() {
    }

    public ConcurrentMap<String, CompletableFuture<CachedVersion>> getCache() {
        return myCache;
    }

    // consulo: the upstream Expiry.writing { _, value -> ... }
    private static Duration expireAfterWrite(CachedVersion value) {
        if (value instanceof CachedVersion.Cached cached) {
            long currentTime = System.currentTimeMillis();
            long whenExpire = cached.lastUpdated() + BazelGithubVersionResolver.GITHUB_CACHE_EVICTION_TIME.toMillis();
            return Duration.ofMillis(whenExpire - currentTime);
        }
        else {
            return Duration.ZERO;
        }
    }

    private static boolean isExpired(CompletableFuture<CachedVersion> future) {
        if (!future.isDone()) {
            return false;
        }
        if (future.isCompletedExceptionally()) {
            return true;
        }
        return !expireAfterWrite(future.join()).isPositive();
    }

    public boolean getIncludePrelease() {
        return myIncludePrelease;
    }

    public void setIncludePrelease(boolean includePrelease) {
        myIncludePrelease = includePrelease;
    }

    public boolean getIncludeReleaseCandidate() {
        return myIncludeReleaseCandidate;
    }

    public void setIncludeReleaseCandidate(boolean includeReleaseCandidate) {
        myIncludeReleaseCandidate = includeReleaseCandidate;
    }

    @Override
    public BazelGithubGlobalCacheService.State getState() {
        State it = new State();
        it.includePrelease = myIncludePrelease;
        it.includeReleaseCandidate = myIncludeReleaseCandidate;

        // consulo: cache.synchronous().asMap() only contains completed, not expired values
        Map<String, ForkState> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, CompletableFuture<CachedVersion>> entry : myCache.entrySet()) {
            CompletableFuture<CachedVersion> future = entry.getValue();
            if (!future.isDone() || isExpired(future)) {
                continue;
            }
            String key = entry.getKey();
            if (future.join() instanceof CachedVersion.Cached value) {
                newMap.put(key, new ForkState(key, value.version(), value.lastUpdated()));
            }
        }
        it.forks.putAll(newMap);
        return it;
    }

    @Override
    public void loadState(BazelGithubGlobalCacheService.State state) {
        myIncludePrelease = state.includePrelease;
        myIncludeReleaseCandidate = state.includeReleaseCandidate;

        myCache.clear();
        Map<String, CompletableFuture<CachedVersion>> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, ForkState> entry : state.forks.entrySet()) {
            ForkState value = entry.getValue();
            newMap.put(entry.getKey(), CompletableFuture.completedFuture(new CachedVersion.Cached(value.version, value.lastUpdated)));
        }
        myCache.putAll(newMap);
    }

    public CompletableFuture<@Nullable String> getCachedVersion(
        String forkName,
        long currentTimeMs,
        BiFunction<String, Long, CompletableFuture<@Nullable String>> supplier
    ) {
        CompletableFuture<CachedVersion> cachedVersion =
            myCache.compute(forkName, (name, existing) -> {
                if (existing != null && !isExpired(existing)) {
                    return existing;
                }
                return supplier.apply(name, currentTimeMs)
                    .<CachedVersion>thenApply(version -> {
                        if (version == null) {
                            return CachedVersion.Unavailable.INSTANCE;
                        }
                        return new CachedVersion.Cached(version, currentTimeMs);
                    });
            });
        return cachedVersion.thenApply(it -> {
            if (it instanceof CachedVersion.Cached cached) {
                return cached.version();
            }
            else {
                return null;
            }
        });
    }

    public void optimizeCache() {
        myCache.entrySet().removeIf(it -> isExpired(it.getValue()));
    }
}
