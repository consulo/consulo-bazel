package org.jetbrains.bazel.sync.workspace.languages.jvm;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// consulo: holder for the top-level functions of upstream DerivedEntryPointClass.kt
public final class DerivedEntryPointClass {
    private DerivedEntryPointClass() {
    }

    /**
     * Returns the class that `rules_java` derives for {@code target} when `test_class` or `main_class` is not set.
     * Returns {@code null} when Bazel derives no class.
     * <p>
     * This is a copy of <a href="https://github.com/bazelbuild/rules_java/blob/9.9.0/java/common/rules/impl/java_helper.bzl#L91-L97">_primary_class</a>
     * in `rules_java`, and it must give the same result.
     */
    public static @Nullable String rulesJavaPrimaryClass(BuildTarget target) {
        Label label = BuildTarget.getId(target);
        String primaryFileName = label.getTargetName() + ".java";
        Optional<String> primarySrc = sourceExecPaths(target).filter(it -> substringAfterLast(it, '/').equals(primaryFileName)).findFirst();
        if (primarySrc.isPresent()) {
            return javaClassName(Arrays.asList(removeSuffix(primarySrc.get(), ".java").split("/", -1)));
        }
        List<String> path = new ArrayList<>(label.packagePath().pathSegments());
        path.addAll(Arrays.asList(label.getTargetName().split("/", -1)));
        return javaClassName(path);
    }

    /**
     * Returns the test class that `rules_kotlin` derives for the `kt_jvm_test` {@code target} when `test_class` is not set.
     * Returns {@code null} when Bazel derives no class.
     * <p>
     * This is a copy of <a href="https://github.com/bazelbuild/rules_kotlin/blob/v2.4.21/kotlin/internal/jvm/impl.bzl#L422-L459">kt_jvm_junit_test_impl</a>
     * in `rules_kotlin`, and it must give the same result.
     */
    public static @Nullable String rulesKotlinTestClass(BuildTarget target) {
        Label label = BuildTarget.getId(target);
        String packagePrefix = String.join("/", label.packagePath().pathSegments()) + "/";
        String testClass = null;
        for (String src : (Iterable<String>) sourceExecPaths(target)::iterator) {
            if (!substringBefore(src.replace(packagePrefix, ""), '.').equals(label.getTargetName())) {
                continue;
            }
            String splitter = KOTLIN_SPLIT_STRINGS.stream().filter(src::contains).findFirst().orElse(null);
            if (splitter == null) {
                continue;
            }
            testClass = substringBefore(substringAfter(src, splitter), '.')
                .replace('/', '.');
        }
        return testClass == null || testClass.isEmpty() ? null : testClass;
    }

    private static Stream<String> sourceExecPaths(BuildTarget receiver) {
        return receiver.getSources().getOutputLocations().map(OutputLocation::toExecrootPath);
    }

    private static @Nullable String javaClassName(List<String> path) {
        List<String> segments = path.stream().filter(it -> !it.isEmpty()).toList();
        Integer rootIndex = BazelConventionalRoots.javaRootIndex(segments);
        if (rootIndex == null) {
            return null;
        }
        String result = String.join(".", segments.subList(rootIndex + 1, segments.size()));
        return result.isEmpty() ? null : result;
    }

    private static final List<String> KOTLIN_SPLIT_STRINGS =
        List.of("src/test/java/", "src/test/kotlin/", "javatests/", "kotlin/", "java/", "test/");

    // consulo: Kotlin String helpers used above (substringAfterLast, substringBefore, substringAfter, removeSuffix)
    private static String substringAfterLast(String receiver, char delimiter) {
        int index = receiver.lastIndexOf(delimiter);
        return index < 0 ? receiver : receiver.substring(index + 1);
    }

    private static String substringBefore(String receiver, char delimiter) {
        int index = receiver.indexOf(delimiter);
        return index < 0 ? receiver : receiver.substring(0, index);
    }

    private static String substringAfter(String receiver, String delimiter) {
        int index = receiver.indexOf(delimiter);
        return index < 0 ? receiver : receiver.substring(index + delimiter.length());
    }

    private static String removeSuffix(String receiver, String suffix) {
        return receiver.endsWith(suffix) ? receiver.substring(0, receiver.length() - suffix.length()) : receiver;
    }
}
