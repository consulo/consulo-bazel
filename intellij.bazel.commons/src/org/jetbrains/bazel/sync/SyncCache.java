package org.jetbrains.bazel.sync;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Computes a cache on the project data.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class SyncCache {
    private final Project myProject;

    @Inject
    public SyncCache(Project project) {
        myProject = project;
    }

    /**
     * Computes a value based on the sync project data.
     */
    @FunctionalInterface
    public interface SyncCacheComputable<T> {
        T compute(Project project);
    }

    private final ConcurrentMap<Object, Object> myCache = new ConcurrentHashMap<>();

    /**
     * Computes a value derived from the sync project data and caches it until the next sync.
     * {@code computable} must be the same object for the cache to work, i.e., this is <b>wrong</b>:
     * <pre>{@code
     * fun getCachedSum(project): Int {
     *   // Lambda object is created on every function call, bad
     *   return SyncCache.getInstance(project).get {
     *     2 + 2
     *   }
     * }
     * }</pre>
     * And this is correct:
     * <pre>{@code
     * val computable = SyncCacheComputable {
     *   2 + 2
     * }
     * fun getCachedSum(project): Int {
     *   return SyncCache.getInstance(project).get(computable)
     * }
     * }</pre>
     *
     * @see "com.intellij.platform.workspace.storage.VersionedEntityStorage.cachedValue"
     */
    @SuppressWarnings("unchecked")
    public <T> T get(SyncCacheComputable<T> computable) {
        // From computeIfAbsent docs: "The mapping function must not modify this map during computation."
        // So we're using getOrPut instead
        // consulo: Kotlin ConcurrentMap.getOrPut
        Object value = myCache.get(computable);
        if (value == null) {
            Object computed = computable.compute(myProject);
            Object previous = myCache.putIfAbsent(computable, computed);
            value = previous != null ? previous : computed;
        }
        return (T) value;
    }

    public void clear() {
        myCache.clear();
    }

    @TestOnly
    public <T> boolean isAlreadyComputed(SyncCacheComputable<T> computable) {
        return myCache.get(computable) != null;
    }

    public static SyncCache getInstance(Project project) {
        return project.getInstance(SyncCache.class);
    }
}
