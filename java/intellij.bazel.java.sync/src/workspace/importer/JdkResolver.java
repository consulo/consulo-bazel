package org.jetbrains.bazel.workspace.importer;

import com.intellij.java.language.projectRoots.JavaSdkType;
import com.intellij.java.language.projectRoots.JavaSdkVersion;
import consulo.platform.Platform;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.sync.workspace.languages.java.Jdk;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JavaToolchainData;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.function.Function;

public class JdkResolver {
    private final Map<WorkspaceTargetKey, BuildTarget> myAllTargets;
    private final Function<OutputLocation, @Nullable Path> myResolveExecrootLocation;

    public JdkResolver(
        Map<WorkspaceTargetKey, BuildTarget> allTargets,
        Function<OutputLocation, @Nullable Path> resolveExecrootLocation
    ) {
        myAllTargets = allTargets;
        myResolveExecrootLocation = resolveExecrootLocation;
    }

    public @Nullable Jdk resolve() {
        List<JdkCandidate> allCandidates = new ArrayList<>();
        for (BuildTarget it : myAllTargets.values()) {
            JdkCandidate candidate = resolveJdkData(it);
            if (candidate != null) {
                allCandidates.add(candidate);
            }
        }
        if (allCandidates.isEmpty()) {
            return localJdkFallback();
        }
        // consulo: Kotlin maxBy returns the first maximal element
        JdkCandidate bestJdk = null;
        for (JdkCandidate it : sortByFrequency(allCandidates)) {
            if (bestJdk == null || it.jdkType().getPriority() > bestJdk.jdkType().getPriority()) {
                bestJdk = it;
            }
        }
        return new Jdk(Objects.requireNonNull(bestJdk).javaHome());
    }

    private @Nullable JdkCandidate resolveJdkData(BuildTarget target) {
        JavaToolchainData toolchain = WorkspaceHelpers.findBuildData(target, JavaToolchainData.class);
        OutputLocation bootClasspath = toolchain != null ? toolchain.bootClasspathJavaHome() : null;
        OutputLocation toolchainHome = toolchain != null ? toolchain.javaHome() : null;
        OutputLocation javaHome;
        JdkType jdkType;
        if (bootClasspath != null) {
            javaHome = bootClasspath;
            jdkType = JdkType.BOOT_CLASSPATH;
        }
        else if (toolchainHome != null) {
            javaHome = toolchainHome;
            jdkType = JdkType.TOOLCHAIN;
        }
        else {
            OutputLocation runtimeHome = null;
            for (DependencyLabel it : target.getDependencies()) {
                BuildTarget dependency = myAllTargets.get(it.targetKey());
                if (dependency == null) {
                    continue;
                }
                JavaToolchainData data = WorkspaceHelpers.findBuildData(dependency, JavaToolchainData.class);
                if (data != null && data.javaHome() != null) {
                    runtimeHome = data.javaHome();
                    break;
                }
            }
            if (runtimeHome == null) {
                return null;
            }
            javaHome = runtimeHome;
            jdkType = JdkType.RUNTIME;
        }
        Path resolved = myResolveExecrootLocation.apply(javaHome);
        if (resolved == null || !Files.exists(resolved)) {
            return null;
        }
        return new JdkCandidate(jdkType, resolved);
    }

    private record JdkCandidate(JdkType jdkType, Path javaHome) {
    }

    /**
     * We can get the JDK from aspect in different ways, some of which are preferable to others.
     */
    public enum JdkType {
        /**
         * The actual JDK used during compilation (rules_java bootclasspath).
         */
        BOOT_CLASSPATH(3),

        /**
         * Runtime JDK (e.g. `--java_runtime_version`); only surfaced by `java_binary` targets.
         */
        RUNTIME(2),

        /**
         * Bazel 7 or older with no runtime JDK set explicitly.
         */
        TOOLCHAIN(1);

        private final int myPriority;

        JdkType(int priority) {
            myPriority = priority;
        }

        public int getPriority() {
            return myPriority;
        }
    }

    private static <A> List<A> sortByFrequency(List<A> receiver) {
        Map<A, List<A>> groups = new LinkedHashMap<>();
        for (A it : receiver) {
            groups.computeIfAbsent(it, ignored -> new ArrayList<>()).add(it);
        }
        List<List<A>> values = new ArrayList<>(groups.values());
        values.sort(Comparator.<List<A>>comparingInt(List::size).reversed());
        return values.stream().map(it -> it.get(0)).toList();
    }

    private @Nullable Jdk localJdkFallback() {
        // From: [JavaHomeFinder.JdkEntry.compareTo] docs:
        // "An entry should appear before another one if it has a **more recent** version or a shorter path."
        // consulo: JavaHomeFinder does not exist, the JDK homes found by the default Java SDK type are compared the same way
        Platform platform = Platform.current();
        JavaSdkType sdkType = JavaSdkType.getDefaultJavaSdkType();
        List<Path> homes = new ArrayList<>();
        sdkType.collectHomePaths(platform, homes::add);
        Path jdkWithNewestVersion = null;
        JavaSdkVersion newestVersion = null;
        for (Path home : homes) {
            if (!sdkType.isValidSdkHome(platform, home)) {
                continue;
            }
            String versionString = sdkType.getVersionString(platform, home);
            JavaSdkVersion version = versionString != null ? JavaSdkVersion.fromVersionString(versionString) : null;
            boolean better;
            if (jdkWithNewestVersion == null) {
                better = true;
            }
            else if (version != null && (newestVersion == null || version.compareTo(newestVersion) > 0)) {
                better = true;
            }
            else if (version == newestVersion) {
                better = home.toString().length() < jdkWithNewestVersion.toString().length();
            }
            else {
                better = false;
            }
            if (better) {
                jdkWithNewestVersion = home;
                newestVersion = version;
            }
        }
        if (jdkWithNewestVersion == null) {
            return null;
        }
        return new Jdk(jdkWithNewestVersion);
    }
}
