package org.jetbrains.bazel.sync.workspace.languages.jvm;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Finds the conventional roots that `rules_java` uses when a target sets no explicit root.
 * The segments below a root give the Java class name or the resource path.
 * <p>
 * Each function takes the segments of a path relative to the root of its repository.
 */
public final class BazelConventionalRoots {
    private BazelConventionalRoots() {
    }

    /**
     * Returns the index of the Java root in {@code segments}, or {@code null} when {@code segments} have no Java root.
     * <p>
     * Some examples:
     * <ul>
     * <li>{@code java/com/foo/Foo.java} gives the index of {@code java}.</li>
     * <li>{@code svc/src/main/java/com/foo/Foo.java} gives the index of {@code java}.</li>
     * <li>{@code app/src/com/foo/Foo.java} gives the index of {@code src}.</li>
     * </ul>
     * <p>
     * This is a copy of <a href="https://github.com/bazelbuild/rules_java/blob/9.9.0/java/common/rules/java_helper.bzl#L22-L52">_java_segments</a>
     * in `rules_java`.
     */
    public static @Nullable Integer javaRootIndex(List<String> segments) {
        int firstRootIndex = -1;
        for (int i = 0; i < segments.size(); i++) {
            if (JAVA_ROOTS.contains(segments.get(i))) {
                firstRootIndex = i;
                break;
            }
        }
        if (firstRootIndex < 0) {
            return null;
        }
        boolean isFirstRootSrc = segments.get(firstRootIndex).equals("src");
        // a `java`, `javatests` or `testsrc` root below other directories is final
        if (firstRootIndex != 0 && !isFirstRootSrc) {
            return firstRootIndex;
        }

        // `src` and a root at the start can hold the real root deeper, so Bazel checks the first nested root below them
        Integer nestedRootIndex = null;
        for (int index = firstRootIndex + 1; index < segments.size() - 1; index++) {
            String segment = segments.get(index);
            boolean matches;
            if (segment.equals("src")) {
                matches = true;
            }
            else if (NESTED_JAVA_ROOTS.contains(segment)) {
                matches = isFirstRootSrc;
            }
            else {
                matches = false;
            }
            if (matches) {
                nestedRootIndex = index;
                break;
            }
        }
        // a Maven layout at a `src` root wins over a nested root
        Integer firstMavenRoot;
        if (isFirstRootSrc) {
            firstMavenRoot = mavenRootIndex(segments, firstRootIndex);
        }
        else {
            firstMavenRoot = null;
        }
        if (nestedRootIndex == null) {
            return firstMavenRoot != null ? firstMavenRoot : firstRootIndex;
        }

        if (TOP_LEVEL_PACKAGES.contains(segments.get(nestedRootIndex + 1))) {
            return firstMavenRoot != null ? firstMavenRoot : nestedRootIndex;
        }
        else if (segments.get(nestedRootIndex).equals("src")) {
            Integer nestedMavenRoot = mavenRootIndex(segments, nestedRootIndex);
            return nestedMavenRoot != null ? nestedMavenRoot : firstRootIndex;
        }
        else {
            return firstMavenRoot != null ? firstMavenRoot : firstRootIndex;
        }
    }

    /**
     * Returns the index of the resource root in {@code segments}, or {@code null} when {@code segments} have no resource root.
     * The first {@code src/<dir>/resources} is the root. When no such directory exists, the Java root from {@link #javaRootIndex} is the root.
     * <p>
     * Some examples:
     * <ul>
     * <li>{@code svc/src/main/resources/foo/a.txt} gives the index of {@code resources}.</li>
     * <li>{@code svc/java/com/foo/a.txt} gives the index of {@code java}.</li>
     * </ul>
     * <p>
     * This is a copy of `get_default_resource_path` in `rules_java`.
     * <a href="https://github.com/bazelbuild/rules_java/blob/9.9.0/java/common/rules/java_helper.bzl">_resource_mapper</a> calls it.
     */
    public static @Nullable Integer javaResourceRootIndex(List<String> segments) {
        Integer srcResourcesIndex = null;
        for (int it = 0; it < segments.size() - 2; it++) {
            if (segments.get(it).equals("src") && segments.get(it + 2).equals("resources")) {
                srcResourcesIndex = it;
                break;
            }
        }
        return srcResourcesIndex != null ? Integer.valueOf(srcResourcesIndex + 2) : javaRootIndex(segments);
    }

    /**
     * Returns the index of {@code java} or {@code resources} when {@code src} starts a Maven layout such as {@code src/main/java}.
     */
    private static @Nullable Integer mavenRootIndex(List<String> receiver, int src) {
        String sourceSet = src + 1 < receiver.size() ? receiver.get(src + 1) : null;
        String sourceRoot = src + 2 < receiver.size() ? receiver.get(src + 2) : null;
        return sourceSet != null && MAVEN_SOURCE_SETS.contains(sourceSet) && sourceRoot != null && MAVEN_SOURCE_ROOTS.contains(sourceRoot)
            ? Integer.valueOf(src + 2)
            : null;
    }

    private static final Set<String> JAVA_ROOTS = Set.of("java", "javatests", "src", "testsrc");
    private static final Set<String> NESTED_JAVA_ROOTS = Set.of("java", "javatests");
    private static final Set<String> TOP_LEVEL_PACKAGES = Set.of("com", "org", "net");
    private static final Set<String> MAVEN_SOURCE_SETS = Set.of("main", "test");
    private static final Set<String> MAVEN_SOURCE_ROOTS = Set.of("java", "resources");
}
