package org.jetbrains.bazel.languages.starlark.bazel.bzlmod.resolver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.Mutex;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelModuleBcrCacheService implements Disposable {
    private final Project myProject;
    private final Gson myGson = new Gson();
    private final MVStore myStore;
    private final MVMap<String, String> myMap;

    @Inject
    public BazelModuleBcrCacheService(Project project) {
        myProject = project;
        myStore = openStore(getProjectDataPath(project, "bzlmod/bcr-cache.mv"));
        myMap = myStore.openMap("bcrCache");
    }

    @Tag("cachedState")
    public static final class CachedValue {
        public List<String> values = List.of();
        public long lastUpdated = 0;

        public CachedValue() {
        }

        public CachedValue(List<String> values, long lastUpdated) {
            this.values = values;
            this.lastUpdated = lastUpdated;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CachedValue that)) {
                return false;
            }
            return lastUpdated == that.lastUpdated && Objects.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(values, lastUpdated);
        }

        @Override
        public String toString() {
            return "CachedValue(values=" + values + ", lastUpdated=" + lastUpdated + ")";
        }
    }

    // consulo: Caffeine is replaced by Guava; Guava has no per-entry Expiry, so the expiration computed by `expiry`
    // (12 hours from CachedValue.lastUpdated) is checked when the value is read, see getCachedValuesIfPresent().
    // The Caffeine maintenance executor (pooled thread) is not needed by Guava.
    private final Cache<String, CachedValue> myCache = CacheBuilder.newBuilder().build();

    public Cache<String, CachedValue> getCache() {
        return myCache;
    }

    private final ConcurrentHashMap<String, Mutex> myKeyLocks = new ConcurrentHashMap<>();

    /**
     * Returns cached values if present.
     * Otherwise, schedules a single asynchronous load per key and awaits it (no thread blocking).
     */
    public CompletableFuture<List<String>> getOrFetch(String key, Supplier<CompletableFuture<@Nullable List<String>>> supplier) {
        List<String> cachedValues = getCachedValuesIfPresent(key);
        if (cachedValues != null) {
            return CompletableFuture.completedFuture(cachedValues);
        }

        // To ensure only one thread is loading the value for a given key at a time, we use a mutex per key.
        // Then other threads will wait for the value to be loaded and get it from the cache.
        return withLock(myKeyLocks.computeIfAbsent(key, it -> new Mutex()), () -> {
            List<String> cachedInLock = getCachedValuesIfPresent(key);
            if (cachedInLock != null) {
                return CompletableFuture.completedFuture(cachedInLock);
            }
            return supplier.get().thenApply(loaded -> {
                if (loaded == null) {
                    return List.of();
                }
                CachedValue value = new CachedValue(loaded, System.currentTimeMillis());
                myCache.put(key, value);
                myMap.put(key, myGson.toJson(value));
                myStore.commit();
                return loaded;
            });
        });
    }

    public void invalidate(String key) {
        myCache.invalidate(key);
        myKeyLocks.remove(key);

        myMap.remove(key);
        myStore.commit();
    }

    @Override
    public void dispose() {
        try {
            myStore.close();
        }
        catch (Throwable e) {
            Logger
                .getInstance(BazelModuleBcrCacheService.class)
                .warn("Failed to close MVStore for Bazel BCR cache", e);
        }
    }

    private @Nullable List<String> getCachedValuesIfPresent(String key) {
        CachedValue inMemory = myCache.getIfPresent(key);
        if (inMemory != null) {
            // consulo: Caffeine `expiry` evicted the entry after its remaining time
            if (remainingNanos(inMemory.lastUpdated) > 0) {
                return inMemory.values;
            }
            myCache.invalidate(key);
        }

        String raw = myMap.get(key);
        if (raw == null) {
            return null;
        }
        CachedValue parsed;
        try {
            parsed = myGson.fromJson(raw, CachedValue.class);
        }
        catch (Throwable e) {
            parsed = null;
        }
        if (parsed == null) {
            return null;
        }
        if (remainingMillis(parsed.lastUpdated) <= 0) {
            myMap.remove(key);
            myStore.commit();
            return null;
        }

        myCache.put(key, parsed);
        return parsed.values;
    }

    // consulo: kotlinx Mutex.withLock { } over a Consulo coroutine Mutex
    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    private <T extends @Nullable Object> CompletableFuture<T> withLock(Mutex mutex, Supplier<CompletableFuture<T>> action) {
        Object owner = new Object();
        CoroutineScope scope = CoroutineScope.of(myProject.coroutineContext());
        return Coroutine.first(mutex.<@Nullable Object>lock(owner))
            .then(CompletableFutureStep.<@Nullable Object, T>await(ignored -> action.get()))
            .runAsync(scope, null)
            .toFuture()
            .whenComplete((result, error) -> {
                if (mutex.holdsLock(owner)) {
                    mutex.unlock(owner);
                }
            });
    }

    private static final long CACHE_EVICTION_TIME = TimeUnit.HOURS.toMillis(12L);

    // consulo: the upstream Caffeine `expiry` object (expire 12 hours after lastUpdated, since the cache is persistent,
    // reads keep the current duration) has no Guava counterpart; its rule is applied in getCachedValuesIfPresent()
    /*
     * Object to expire cached values after 12 hours.
     *
     * Since the cache is persistent, we need a custom Expiry parameter
     * (otherwise, in case of IDE reset, the expiration time would be renewed
     * and the data could remain valid for a very long time).
     * The implementation stores lastUpdated and calculates expiration from that
     * timestamp, so data is kept for 12 hours from the moment it is fetched
     * from BCR, regardless of how often the IDE is restarted.
     */

    private static long remainingMillis(long lastUpdatedMillis) {
        return lastUpdatedMillis + CACHE_EVICTION_TIME - System.currentTimeMillis();
    }

    private static long remainingNanos(long lastUpdatedMillis) {
        long remaining = remainingMillis(lastUpdatedMillis);
        return remaining <= 0 ? 0L : TimeUnit.MILLISECONDS.toNanos(remaining);
    }

    /**
     * Opens an MVStore instance for the given file path.
     * Creates the parent directories if needed.
     */
    private static MVStore openStore(Path path) {
        // consulo: Kotlin Path.createParentDirectories()
        Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        MVStore store = new MVStore.Builder()
            .fileName(path.toAbsolutePath().toString())
            .autoCommitDisabled()
            .cacheSize(128)
            .open();
        store.setVersionsToKeep(0);
        return store;
    }

    // consulo: IntelliJ Project.getProjectDataPath(name)
    private static Path getProjectDataPath(Project project, String name) {
        return ProjectUtil.getProjectCachePath(project, "projects").resolve(name);
    }
}
