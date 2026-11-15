package org.jetbrains.bazel.workspace.importer;

import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.DependencyLabel.DependencyLabelKind;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.languages.java.Jdk;
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JavaProviderData;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JavaToolchainData;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JdepsJar;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JvmDependency;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JvmOutputs;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.ScalaBuildTarget;
import org.jetbrains.bazel.sync.workspace.mapper.normal.java.MavenCoordinatesResolver;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.target.BuildTargetUtils;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.WorkspaceLibraries;
import org.jetbrains.bsp.protocol.WorkspaceLibraries.LibraryItem;
import org.jetbrains.bsp.protocol.WorkspaceLibraries.MavenCoordinates;
import org.jetbrains.bsp.protocol.utils.StringUtils;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class JvmBuildTargetResolver {
    // consulo: upstream `private typealias DependencyLabelPatcher = (DependencyLabel) -> DependencyLabel`
    private interface DependencyLabelPatcher extends UnaryOperator<DependencyLabel> {
    }

    public static boolean containsInternalJars(JvmBuildTarget receiver, RepoMapping repoMapping) {
        return receiver.outputSourceJars().getOutputLocations().anyMatch(it -> OutputLocation.isUserCode(it, repoMapping)) &&
            receiver.binaryOutputs().getOutputLocations().anyMatch(it -> OutputLocation.isUserCode(it, repoMapping));
    }

    private static final String KOTLIN_STDLIB_LIBRARY_NAME = "rules_kotlin_kotlin-stdlibs";

    private final Map<WorkspaceTargetKey, BuildTarget> myAllTargets;
    private final Map<WorkspaceTargetKey, BuildTarget> myTargetsToImport;
    private final JavaWorkspaceSyncConfig myJavaSyncConfig;
    private final RepoMapping myRepoMapping;
    private final Function<OutputLocation, @Nullable Path> myResolveLocation;
    private final Function<OutputLocation, @Nullable Path> myResolveExecrootLocation;

    private Map<WorkspaceTargetKey, List<DependencyLabel>> myExtraLibDependencies = Map.of();
    private Map<WorkspaceTargetKey, List<DependencyLabel>> myToolchainDependencies = Map.of();
    private Map<WorkspaceTargetKey, List<LibraryItem>> myAllLibraries = Map.of();
    private Map<MavenCoordinatesKey, WorkspaceTargetKey> myWellKnownTargetKeyByMavenCoordinates = Map.of();

    // keep output jar label identity
    private final Map<Label, Set<Path>> myOutputJarsByLabel;

    private final Supplier<@Nullable Path> myProjectJavaHome;

    private final Map<WorkspaceTargetKey, List<DependencyLabel>> myDependenciesCache = new HashMap<>();
    private final Map<WorkspaceTargetKey, List<DependencyLabel>> myMavenExportDependenciesCache = new HashMap<>();

    private final Set<String> myWellKnownTargetKinds =
        Set.of(
            "java_library",
            "java_binary",
            "java_test",
            "kt_jvm_library",
            "kt_jvm_binary",
            "kt_jvm_test",
            "jvm_library",
            "_jvm_library_jps",
            "jvm_resources",
            "_resourcegroup_jps",
            "scala_library",
            "scala_binary",
            "scala_test",
            "intellij_plugin_debug_target"
        );

    private final Map<Path, Label> myJdepsLabelByJar;

    public JvmBuildTargetResolver(
        Map<WorkspaceTargetKey, BuildTarget> allTargets,
        Map<WorkspaceTargetKey, BuildTarget> targetsToImport,
        JavaWorkspaceSyncConfig javaSyncConfig,
        RepoMapping repoMapping,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        Function<OutputLocation, @Nullable Path> resolveExecrootLocation
    ) {
        myAllTargets = allTargets;
        myTargetsToImport = targetsToImport;
        myJavaSyncConfig = javaSyncConfig;
        myRepoMapping = repoMapping;
        myResolveLocation = resolveLocation;
        myResolveExecrootLocation = resolveExecrootLocation;

        Map<Label, Set<Path>> outputJarsByLabel = new HashMap<>();
        for (Map.Entry<WorkspaceTargetKey, BuildTarget> entry : myAllTargets.entrySet()) {
            Set<Path> jars = outputBinaryJars(entry.getValue());
            if (!jars.isEmpty()) {
                outputJarsByLabel.computeIfAbsent(entry.getKey().label(), it -> new LinkedHashSet<>()).addAll(jars);
            }
        }
        myOutputJarsByLabel = outputJarsByLabel;

        myProjectJavaHome = LazyValue.nullable(() -> {
            Jdk jdk = new JdkResolver(myAllTargets, myResolveExecrootLocation).resolve();
            return jdk != null ? jdk.javaHome() : null;
        });

        Map<Path, Label> jdepsLabelByJar = new LinkedHashMap<>();
        for (BuildTarget target : myAllTargets.values()) {
            JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
            if (jvmBuildTarget == null) {
                continue;
            }
            for (JdepsJar jdepsJar : jvmBuildTarget.jdepsJars()) {
                Path jar = myResolveLocation.apply(jdepsJar.jar());
                if (jar != null) {
                    jdepsLabelByJar.put(jar, jdepsJar.syntheticLabel());
                }
            }
        }
        myJdepsLabelByJar = jdepsLabelByJar;
    }

    public Map<WorkspaceTargetKey, JvmResolvedTarget> resolveAll() {
        myWellKnownTargetKeyByMavenCoordinates = computeWellKnownTargetKeyByMavenCoordinates();
        Map<WorkspaceTargetKey, BuildTarget> jvmTargetsToImport = new LinkedHashMap<>();
        myTargetsToImport.forEach((key, target) -> {
            if (WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class) != null) {
                jvmTargetsToImport.put(key, target);
            }
        });
        calculateAllLibraries(jvmTargetsToImport);

        // We merge targets based on (label, configuration) pair because:
        //  * dependency edges contain only partial key (label + configuration)
        //  * to avoid unnecessary expansion of module graph (two axis is enough),
        //    Merging is being done inside `JvmWorkspaceTargetMerger`, this part of the code aims to replicate that,
        //    so we get exactly single target for (label, configuration) pair
        //  The most common example where merging is used is for code generators like protobuf,
        //  which use aspect to attach language-specific providers to e.g.,
        //  proto provider, then we get `(label = //a, config = X, aspects = [])` - no JavaInfo
        //  and `(label = //a, config = X, aspects = [//proto_smth])` - with JavaInfo (generated proto sources),
        //  and based on that generated target when used as dependency bring proto generated sources
        Map<WorkspaceTargetKey, List<JvmResolvedTarget>> groups = new LinkedHashMap<>();
        for (BuildTarget target : myTargetsToImport.values()) {
            if (WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class) == null) {
                continue;
            }
            JvmResolvedTarget resolved = createJvmResolvedTarget(target);
            groups.computeIfAbsent(DependencyBuilder.stripAspects(resolved.key()), it -> new ArrayList<>()).add(resolved);
        }
        Map<WorkspaceTargetKey, JvmResolvedTarget> result = new LinkedHashMap<>();
        for (Map.Entry<WorkspaceTargetKey, List<JvmResolvedTarget>> entry : groups.entrySet()) {
            WorkspaceTargetKey strippedKey = entry.getKey();
            List<JvmResolvedTarget> group = entry.getValue();
            Map<WorkspaceTargetKey, LibraryItem> libraries = new LinkedHashMap<>();
            for (JvmResolvedTarget it : group) {
                for (LibraryItem library : it.libraries()) {
                    libraries.putIfAbsent(library.key(), library);
                }
            }
            // consulo: distinctBy { it::class to it.dependency.copy(targetKey = it.dependency.targetKey.stripAspects()) }
            Map<List<Object>, JvmDependency> jvmDependencies = new LinkedHashMap<>();
            for (JvmResolvedTarget it : group) {
                for (JvmDependency dependency : it.jvmDependencies()) {
                    List<Object> distinctKey = List.of(
                        dependency.getClass(),
                        new DependencyLabel(DependencyBuilder.stripAspects(dependency.dependency().targetKey()), dependency.dependency().kind())
                    );
                    jvmDependencies.putIfAbsent(distinctKey, dependency);
                }
            }
            result.put(strippedKey, new JvmResolvedTarget(
                strippedKey,
                new ArrayList<>(libraries.values()),
                new ArrayList<>(jvmDependencies.values()),
                myProjectJavaHome.get(),
                group.get(0).javaVersion()
            ));
        }
        return result;
    }

    private JvmResolvedTarget createJvmResolvedTarget(BuildTarget target) {
        WorkspaceTargetKey targetKey = target.getKey();
        JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
        String javaVersion = javaVersionFromJavacOpts(jvmBuildTarget != null ? jvmBuildTarget.javacOpts().asList() : List.of());
        if (javaVersion == null) {
            javaVersion = javaVersionFromToolchain(target);
        }

        Map<WorkspaceTargetKey, LibraryItem> targetLibraries = new LinkedHashMap<>();
        for (LibraryItem it : myAllLibraries.getOrDefault(targetKey, List.of())) {
            targetLibraries.put(it.key(), it);
        }

        // https://youtrack.jetbrains.com/issue/BAZEL-983
        // extra libraries can override some library versions, so they should be put before
        List<JvmDependency> jvmDependencies = new ArrayList<>();
        for (DependencyLabel it : myExtraLibDependencies.getOrDefault(targetKey, List.of())) {
            jvmDependencies.add(new JvmDependency.LibraryDependency(it));
        }
        for (DependencyLabel it : dependencies(target)) {
            if (targetLibraries.containsKey(it.targetKey())) {
                jvmDependencies.add(new JvmDependency.LibraryDependency(it));
            }
            else {
                jvmDependencies.add(new JvmDependency.ModuleDependency(it));
            }
        }
        for (DependencyLabel it : myToolchainDependencies.getOrDefault(targetKey, List.of())) {
            jvmDependencies.add(new JvmDependency.LibraryDependency(it));
        }

        return new JvmResolvedTarget(
            targetKey,
            new ArrayList<>(targetLibraries.values()),
            jvmDependencies,
            myProjectJavaHome.get(),
            javaVersion != null ? javaVersion : ""
        );
    }

    private List<DependencyLabel> dependencies(BuildTarget receiver) {
        MavenCoordinatesKey coordinatesKey = mavenCoordinatesKeyOrNull(receiver);
        if (coordinatesKey != null && myWellKnownTargetKeyByMavenCoordinates.containsKey(coordinatesKey)) {
            return mavenExportDependencies(receiver);
        }
        return directDependencies(receiver);
    }

    private List<DependencyLabel> directDependencies(BuildTarget receiver) {
        BuildTarget target = receiver;
        List<DependencyLabel> cached = myDependenciesCache.get(target.getKey());
        if (cached != null) {
            return cached;
        }
        List<DependencyLabel> value = computeDirectDependencies(target);
        myDependenciesCache.put(target.getKey(), value);
        return value;
    }

    // consulo: body of the upstream `dependenciesCache.getOrPut(target.key) F@{ ... }`
    private List<DependencyLabel> computeDirectDependencies(BuildTarget target) {
        String kind = target.getKind().kind();

        // Well-known targets which include generated libraries as dependencies.
        // They must be exported, but this is not returned from aspects:
        // https://bazel.build/reference/be/protocol-buffer#proto_library_args
        if (kind.equals("java_proto_library")) {
            return target.getDependencies().stream()
                .map(it -> new DependencyLabel(it.targetKey(), DependencyLabelKind.EXPORTED_COMPILE_TIME))
                .toList();
        }

        // Scala proto libraries do not have meaningful dependencies despite somethign might return from aspects
        // The entire transitive closure of libraries, which is used by compiler,
        // is returned in targetInfo.javaProvider.fullCompileJarsList
        if (kind.equals("scala_proto_library")) {
            return List.of();
        }

        // https://youtrack.jetbrains.com/issue/BAZEL-3218
        // Some custom rules declare the dependent output jar as its own,
        // thus making the dependency effectively exported
        DependencyLabelPatcher exportByOutputJarsPatcher;
        if (myWellKnownTargetKinds.contains(kind)) {
            // Well known rules have well known behavior
            exportByOutputJarsPatcher = it -> it;
        }
        else {
            Set<Path> outputJars = outputBinaryJars(target);
            exportByOutputJarsPatcher = dependency -> {
                // Over-approximate to label to keep correct behavior when target depend on targets with different configuration/aspects
                Set<Path> depJars = myOutputJarsByLabel.getOrDefault(dependency.targetKey().label(), Set.of());
                for (Path it : outputJars) {
                    if (depJars.contains(it)) {
                        return new DependencyLabel(dependency.targetKey(), DependencyLabelKind.EXPORTED_COMPILE_TIME);
                    }
                }

                return dependency;
            };
        }

        return target.getDependencies().stream()
            .map(it -> exportByOutputJarsPatcher.apply(toWellKnownTargetByMavenCoordinates(it)))
            .toList();
    }

    // Locates a target of known kind for every maven coordinates that is also associated with target of unknown kind.
    // This pattern is produced by `java_export` macro from rules_jvm_external
    // It should also work with other macros following similar pattern e.g. macros from selenium repository
    private Map<MavenCoordinatesKey, WorkspaceTargetKey> computeWellKnownTargetKeyByMavenCoordinates() {
        Map<MavenCoordinatesKey, List<WorkspaceTargetKey>> unknownTargetsWithMavenCoordinates = new LinkedHashMap<>();
        for (BuildTarget it : myAllTargets.values()) {
            if (myWellKnownTargetKinds.contains(it.getKind().kind()) || !BuildTargetUtils.isWorkspace(it, myRepoMapping)) {
                continue;
            }
            MavenCoordinatesKey key = mavenCoordinatesKeyOrNull(it);
            if (key == null) {
                continue;
            }
            unknownTargetsWithMavenCoordinates.computeIfAbsent(key, ignored -> new ArrayList<>()).add(it.getKey());
        }
        Map<MavenCoordinatesKey, WorkspaceTargetKey> result = new LinkedHashMap<>();
        Set<MavenCoordinatesKey> ambiguousResults = new HashSet<>();
        for (BuildTarget target : myAllTargets.values()) {
            if (!myWellKnownTargetKinds.contains(target.getKind().kind()) || !BuildTargetUtils.isWorkspace(target, myRepoMapping)) {
                continue;
            }
            MavenCoordinatesKey coordinatesKey = mavenCoordinatesKeyOrNull(target);
            if (coordinatesKey == null) {
                continue;
            }
            List<WorkspaceTargetKey> targetsToReplaceKeys = unknownTargetsWithMavenCoordinates.get(coordinatesKey);
            if (targetsToReplaceKeys == null) {
                continue;
            }
            List<BuildTarget> targetsToReplace = targetsToReplaceKeys.stream().map(myAllTargets::get).filter(Objects::nonNull).toList();
            if (targetsToReplace.isEmpty()) {
                continue;
            }
            String targetGeneratorName = target.getGeneratorName();
            if (targetGeneratorName == null || targetsToReplace.stream().anyMatch(it -> !targetGeneratorName.equals(it.getGeneratorName()))) {
                continue;
            }
            if (ambiguousResults.contains(coordinatesKey)) {
                continue;
            }
            if (result.containsKey(coordinatesKey)) {
                ambiguousResults.add(coordinatesKey);
                continue;
            }
            result.put(coordinatesKey, target.getKey());
        }
        for (MavenCoordinatesKey it : ambiguousResults) {
            result.remove(it);
        }
        return result;
    }

    private DependencyLabel toWellKnownTargetByMavenCoordinates(DependencyLabel receiver) {
        BuildTarget target = myAllTargets.get(receiver.targetKey());
        if (target == null) {
            return receiver;
        }
        if (myWellKnownTargetKinds.contains(target.getKind().kind()) || !BuildTargetUtils.isWorkspace(target, myRepoMapping)) {
            return receiver;
        }
        MavenCoordinatesKey coordinatesKey = mavenCoordinatesKeyOrNull(target);
        if (coordinatesKey == null) {
            return receiver;
        }
        WorkspaceTargetKey libraryKey = myWellKnownTargetKeyByMavenCoordinates.get(coordinatesKey);
        if (libraryKey == null) {
            return receiver;
        }
        return new DependencyLabel(libraryKey, receiver.kind());
    }

    private static @Nullable MavenCoordinatesKey mavenCoordinatesKeyOrNull(BuildTarget receiver) {
        MavenCoordinates coordinates = MavenCoordinatesResolver
            .fromTargetTagsList(receiver.getTags());
        if (coordinates == null) {
            return null;
        }
        return toKey(coordinates, receiver.getKey().configuration());
    }

    // `java_export` (rules_jvm_external) merges every transitive dependency that is NOT itself a maven artifact into the single published jar
    // we mirror that by treating those inlined non-maven deps as exported
    // not exporting those leads to red code when referring those non-maven transitive dependencies
    private List<DependencyLabel> mavenExportDependencies(BuildTarget receiver) {
        List<DependencyLabel> cached = myMavenExportDependenciesCache.get(receiver.getKey());
        if (cached != null) {
            return cached;
        }
        LinkedHashSet<DependencyLabel> value = new LinkedHashSet<>();
        for (DependencyLabel dependency : directDependencies(receiver)) {
            BuildTarget depTarget = myAllTargets.get(dependency.targetKey());
            if (depTarget != null && shouldBeExportedForMavenArtifact(depTarget)) {
                value.add(new DependencyLabel(dependency.targetKey(), DependencyLabelKind.EXPORTED_COMPILE_TIME));
                value.addAll(mavenExportDependencies(depTarget));
            }
            else {
                value.add(dependency);
            }
        }
        List<DependencyLabel> result = new ArrayList<>(value);
        myMavenExportDependenciesCache.put(receiver.getKey(), result);
        return result;
    }

    private boolean shouldBeExportedForMavenArtifact(BuildTarget receiver) {
        return BuildTargetUtils.isWorkspace(receiver, myRepoMapping) && mavenCoordinatesKeyOrNull(receiver) == null;
    }

    private void calculateAllLibraries(
        Map<WorkspaceTargetKey, BuildTarget> targetsToImport
    ) {
        // Avoid creating the same LibraryItem instance several times to avoid O(N^2) (BAZEL-3203)
        // consulo: Ref<LibraryItem?> -> Optional-like holder, a null library is cached too
        HashMap<WorkspaceTargetKey, LibraryRef> libraryItemByIdCache = new HashMap<>();

        Map<WorkspaceTargetKey, List<LibraryItem>> importDependenciesAsLibraries = new LinkedHashMap<>();
        targetsToImport.forEach((key, target) -> {
            List<LibraryItem> libraries = new ArrayList<>();
            for (DependencyLabel dependency : dependencies(target)) {
                WorkspaceTargetKey depKey = dependency.targetKey();
                if (targetsToImport.containsKey(depKey)) {
                    continue; // Dependency target is imported, no need to create library
                }

                BuildTarget libTarget = myAllTargets.get(depKey);
                if (libTarget == null) {
                    continue;
                }

                LibraryItem library = libraryItemByIdCache.computeIfAbsent(depKey, it -> new LibraryRef(createLibrary(depKey, libTarget))).value();
                if (library != null) {
                    libraries.add(library);
                }
            }
            importDependenciesAsLibraries.put(key, libraries);
        });

        Map<WorkspaceTargetKey, Set<Path>> interfacesAndBinariesFromTargetsToImport =
            collectInterfacesAndClasses(targetsToImport.values());
        Map<WorkspaceTargetKey, List<LibraryItem>> outputJarsLibraries =
            calculateOutputJarsLibraries(targetsToImport.values());
        Map<WorkspaceTargetKey, List<LibraryItem>> annotationProcessorLibraries =
            annotationProcessorLibraries(targetsToImport.values());
        Map<WorkspaceTargetKey, List<LibraryItem>> librariesFromToolchains =
            calculateToolchainLibraries(targetsToImport);
        Map<WorkspaceTargetKey, List<LibraryItem>> librariesFromDeps =
            concatenateMaps(List.of(outputJarsLibraries, annotationProcessorLibraries));
        Map<WorkspaceTargetKey, List<LibraryItem>> librariesFromDepsAndTargets =
            concatenateMaps(List.of(librariesFromDeps, librariesFromToolchains, importDependenciesAsLibraries));
        Map<WorkspaceTargetKey, List<LibraryItem>> extraLibrariesFromJdeps =
            jdepsLibraries(
                targetsToImport,
                librariesFromDepsAndTargets,
                interfacesAndBinariesFromTargetsToImport
            );

        Map<WorkspaceTargetKey, List<LibraryItem>> extraLibraries = concatenateMaps(List.of(librariesFromDeps, extraLibrariesFromJdeps));

        Map<WorkspaceTargetKey, List<DependencyLabel>> extraLibDependencies = new LinkedHashMap<>();
        extraLibraries.forEach((key, libs) -> extraLibDependencies.put(
            key,
            libs.stream().map(it -> new DependencyLabel(it.key(), DependencyLabelKind.EXPORTED_COMPILE_TIME)).toList()
        ));
        myExtraLibDependencies = extraLibDependencies;
        Map<WorkspaceTargetKey, List<DependencyLabel>> toolchainDependencies = new LinkedHashMap<>();
        librariesFromToolchains.forEach((key, libs) -> toolchainDependencies.put(
            key,
            libs.stream().map(it -> new DependencyLabel(it.key())).toList()
        ));
        myToolchainDependencies = toolchainDependencies;
        myAllLibraries = concatenateMaps(List.of(importDependenciesAsLibraries, extraLibraries, librariesFromToolchains));
    }

    // consulo: Kotlin Ref<LibraryItem?>
    private record LibraryRef(@Nullable LibraryItem value) {
    }

    private @Nullable JavaToolchainData toolchainInfo(BuildTarget target) {
        JavaToolchainData data = WorkspaceHelpers.findBuildData(target, JavaToolchainData.class);
        if (data != null) {
            return data;
        }
        for (DependencyLabel it : target.getDependencies()) {
            if (it.kind() != DependencyLabelKind.TOOLCHAIN) {
                continue;
            }
            BuildTarget toolchainTarget = myAllTargets.get(it.targetKey());
            if (toolchainTarget == null) {
                continue;
            }
            JavaToolchainData toolchainData = WorkspaceHelpers.findBuildData(toolchainTarget, JavaToolchainData.class);
            if (toolchainData != null) {
                return toolchainData;
            }
        }
        return null;
    }

    private @Nullable String javaVersionFromToolchain(BuildTarget target) {
        JavaToolchainData toolchainInfo = toolchainInfo(target);
        return toolchainInfo != null ? toolchainInfo.sourceVersion() : null;
    }

    private static @Nullable String javaVersionFromJavacOpts(List<String> javacOpts) {
        for (int i = 0; i < javacOpts.size(); i++) {
            String option = javacOpts.get(i);
            int space = option.indexOf(' ');
            String flagName = space == -1 ? option : option.substring(0, space);
            String argument = space == -1 ? "" : option.substring(space + 1);
            if (flagName.equals("-target") || flagName.equals("--target") || flagName.equals("--release")) {
                if (!argument.isBlank()) {
                    return argument;
                }
                return i + 1 < javacOpts.size() ? javacOpts.get(i + 1) : null;
            }
        }
        return null;
    }

    private static boolean hasKnownJvmSources(BuildTarget target) {
        return BuildTarget.nonGeneratedSources(target).anyMatch(JvmTargetEntitiesBuilder::hasJvmSourceExtension);
    }

    private boolean shouldCreateOutputJarsLibrary(BuildTarget target) {
        // Resource-only targets and non-JVM targets never produce output jars worth indexing.
        if (target.getKind().kind().endsWith("_resources") || WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class) == null) {
            return false;
        }

        boolean hasGeneratedSrcJar = target.getSources().getOutputLocations()
            .anyMatch(it -> OutputLocation.isGenerated(it) && BuilderUtils.extension(OutputLocation.getRelativeNioPath(it)).equals("srcjar"));
        boolean hasOnlyNonJvmSources = BuildTarget.nonGeneratedSources(target).findAny().isPresent() && !hasKnownJvmSources(target);
        JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
        boolean isUnknownTargetWithoutSources =
            BuildTarget.nonGeneratedSources(target).findAny().isEmpty() && !myWellKnownTargetKinds.contains(target.getKind().kind()) &&
                !(jvmBuildTarget != null && jvmBuildTarget.hasExecutableInfo());
        JavaProviderData javaProviderData = WorkspaceHelpers.findBuildData(target, JavaProviderData.class);
        boolean hasApiGeneratingPlugins = javaProviderData != null && javaProviderData.hasApiGeneratingPlugins();
        KotlinBuildTarget kotlinBuildTarget = WorkspaceHelpers.findBuildData(target, KotlinBuildTarget.class);
        boolean dependsOnExportedApiGeneratingPlugins =
            (kotlinBuildTarget != null ? kotlinBuildTarget.exportedCompilerPluginTargetsList() : List.<WorkspaceTargetKey>of()).stream()
                .anyMatch(key -> {
                    BuildTarget pluginTarget = myAllTargets.get(key);
                    JavaProviderData pluginData = pluginTarget != null ? WorkspaceHelpers.findBuildData(pluginTarget, JavaProviderData.class) : null;
                    return pluginData != null && pluginData.hasApiGeneratingPlugins();
                });

        return hasGeneratedSrcJar ||
            hasOnlyNonJvmSources ||
            isUnknownTargetWithoutSources ||
            hasApiGeneratingPlugins ||
            dependsOnExportedApiGeneratingPlugins;
    }

    private Map<WorkspaceTargetKey, List<LibraryItem>> calculateOutputJarsLibraries(
        Collection<BuildTarget> targetsToImport
    ) {
        Map<WorkspaceTargetKey, List<LibraryItem>> result = new LinkedHashMap<>();
        for (BuildTarget target : targetsToImport) {
            if (!shouldCreateOutputJarsLibrary(target)) {
                continue;
            }
            LibraryItem library = createLibrary(target.getKey(), target);
            if (library != null) {
                result.put(target.getKey(), List.of(library));
            }
        }
        return result;
    }

    private Map<WorkspaceTargetKey, List<LibraryItem>> annotationProcessorLibraries(Collection<BuildTarget> targetsToImport) {
        Map<WorkspaceTargetKey, LibraryItem> associated = new LinkedHashMap<>();
        for (BuildTarget target : targetsToImport) {
            JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
            if (jvmBuildTarget == null || jvmBuildTarget.generatedJars().isEmpty()) {
                continue;
            }
            WorkspaceTargetKey libKey = target.getKey();
            List<JvmOutputs> generated = jvmBuildTarget.generatedJars();
            KotlinBuildTarget kotlinBuildTarget = WorkspaceHelpers.findBuildData(target, KotlinBuildTarget.class);
            Set<Path> kspSourceJars = kotlinBuildTarget != null
                ? new HashSet<>(JvmTargetEntitiesBuilder.resolvePaths(kotlinBuildTarget.kspSourceJars(), myResolveLocation))
                : Set.of();
            Set<Path> jars = new LinkedHashSet<>();
            Set<Path> sourceJars = new LinkedHashSet<>();
            for (JvmOutputs it : generated) {
                jars.addAll(JvmTargetEntitiesBuilder.resolvePaths(it.binaryJars(), myResolveLocation));
                for (Path sourceJar : JvmTargetEntitiesBuilder.resolvePaths(it.sourceJars(), myResolveLocation)) {
                    // we don't want KSP source jars inside library source JARs,
                    // it won't have it's compiled jar equivalent anyway
                    if (!kspSourceJars.contains(sourceJar)) {
                        sourceJars.add(sourceJar);
                    }
                }
            }
            associated.put(
                libKey,
                createLibrary(
                    // `Label.toString()` round-trips the raw target label, so this matches the pre-refactor `key.label + "_generated"`
                    new WorkspaceTargetKey(Label.synthetic(target.getKey().label().toString() + "_generated"), libKey.configuration(), libKey.aspectIds()),
                    Set.of(),
                    jars,
                    sourceJars
                )
            );
        }
        Map<WorkspaceTargetKey, List<LibraryItem>> result = new LinkedHashMap<>();
        associated.forEach((key, library) -> {
            if (!library.jars().isEmpty() || !library.sourceJars().isEmpty()) {
                result.put(key, List.of(library));
            }
        });
        return result;
    }

    // inlined `toolchainLibraries` from `JvmLanguagePluginMixin`
    private Map<WorkspaceTargetKey, List<LibraryItem>> calculateToolchainLibraries(
        Map<WorkspaceTargetKey, BuildTarget> targetsToImport
    ) {
        HashMap<WorkspaceTargetKey, List<LibraryItem>> result = new HashMap<>();

        for (Map.Entry<WorkspaceTargetKey, LibraryItem> entry : calculateKotlinStdlibLibraries(targetsToImport).entrySet()) {
            result.computeIfAbsent(entry.getKey(), it -> new ArrayList<>()).add(entry.getValue());
        }

        Set<Path> sdkJars = new LinkedHashSet<>();
        for (BuildTarget it : myAllTargets.values()) {
            ScalaBuildTarget scala = WorkspaceHelpers.findBuildData(it, ScalaBuildTarget.class);
            if (scala != null) {
                sdkJars.addAll(JvmTargetEntitiesBuilder.resolvePaths(scala.sdkJars(), myResolveLocation));
            }
        }
        Map<Path, LibraryItem> sdkLibByJar = new LinkedHashMap<>();
        for (Path it : sdkJars) {
            sdkLibByJar.put(it, createLibrary(
                new WorkspaceTargetKey(Label.synthetic(it.getFileName().toString())),
                Set.of(),
                Set.of(it),
                Set.of()
            ));
        }
        Set<Path> testJars = new LinkedHashSet<>();
        for (BuildTarget it : myAllTargets.values()) {
            testJars.addAll(scalatestClasspathJars(it));
        }
        Map<Path, LibraryItem> testLibByJar = new LinkedHashMap<>();
        for (Path it : testJars) {
            testLibByJar.put(it, createLibrary(
                new WorkspaceTargetKey(Label.synthetic(it.getFileName().toString())),
                Set.of(),
                Set.of(it),
                Set.of()
            ));
        }
        targetsToImport.forEach((key, target) -> {
            ScalaBuildTarget scala = WorkspaceHelpers.findBuildData(target, ScalaBuildTarget.class);
            if (scala == null) {
                return;
            }
            LinkedHashSet<LibraryItem> libs = new LinkedHashSet<>();
            for (Path it : JvmTargetEntitiesBuilder.resolvePaths(scala.sdkJars(), myResolveLocation)) {
                LibraryItem lib = sdkLibByJar.get(it);
                if (lib != null) {
                    libs.add(lib);
                }
            }
            for (Path it : scalatestClasspathJars(target)) {
                LibraryItem lib = testLibByJar.get(it);
                if (lib != null) {
                    libs.add(lib);
                }
            }
            if (!libs.isEmpty()) {
                result.computeIfAbsent(key, it -> new ArrayList<>()).addAll(libs);
            }
        });
        return result;
    }

    /**
     * Creates one stdlib library per Kotlin toolchain and maps every Kotlin target to its library.
     * <p>
     * Bazel resolves the Kotlin toolchain per configuration, so the targets of one configuration share the stdlib jars.
     * A target without a configuration id is grouped by its stdlib jar set instead.
     * <p>
     * Module dependencies resolve a library by its label, so the group goes into the synthetic label,
     * as the configuration checksum or as a hash of the jar set. A single group keeps the plain library name.
     * See https://youtrack.jetbrains.com/issue/BAZEL-3549
     */
    private Map<WorkspaceTargetKey, LibraryItem> calculateKotlinStdlibLibraries(
        Map<WorkspaceTargetKey, BuildTarget> targetsToImport
    ) {
        record KotlinStdlib(
            List<Path> jars,
            List<Path> sourceJars
        ) {
        }

        record KeyAndStdlib(WorkspaceTargetKey key, KotlinStdlib stdlib) {
        }

        List<KeyAndStdlib> stdlibByTarget = new ArrayList<>();
        targetsToImport.forEach((key, target) -> {
            KotlinBuildTarget kotlinTarget = WorkspaceHelpers.findBuildData(target, KotlinBuildTarget.class);
            if (kotlinTarget == null) {
                return;
            }
            if (kotlinTarget.stdlibJars().isEmpty()) {
                return;
            }
            stdlibByTarget.add(new KeyAndStdlib(key, new KotlinStdlib(
                JvmTargetEntitiesBuilder.resolvePaths(kotlinTarget.stdlibJars(), myResolveLocation),
                JvmTargetEntitiesBuilder.resolvePaths(kotlinTarget.stdlibInferredSourceJars(), myResolveLocation)
            )));
        });
        if (stdlibByTarget.isEmpty()) {
            return Map.of();
        }

        Map<String, List<KeyAndStdlib>> groups = new LinkedHashMap<>();
        for (KeyAndStdlib it : stdlibByTarget) {
            String groupKey = it.key().configuration().getShortChecksum();
            if (groupKey == null) {
                groupKey = StringUtils.md5Hash(it.stdlib().jars().stream().map(Path::toString).sorted().collect(Collectors.joining(",")), 5);
            }
            groups.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(it);
        }

        HashMap<WorkspaceTargetKey, LibraryItem> result = new HashMap<>();
        for (Map.Entry<String, List<KeyAndStdlib>> group : groups.entrySet()) {
            String suffix = group.getKey();
            List<KeyAndStdlib> entries = group.getValue();
            String name = groups.size() == 1 ? KOTLIN_STDLIB_LIBRARY_NAME : KOTLIN_STDLIB_LIBRARY_NAME + "-" + suffix;
            Set<Path> jars = new LinkedHashSet<>();
            Set<Path> sourceJars = new LinkedHashSet<>();
            for (KeyAndStdlib it : entries) {
                jars.addAll(it.stdlib().jars());
                sourceJars.addAll(it.stdlib().sourceJars());
            }
            LibraryItem library = createLibrary(
                new WorkspaceTargetKey(Label.synthetic(name)),
                Set.of(),
                jars,
                sourceJars
            );
            for (KeyAndStdlib it : entries) {
                result.put(it.key(), library);
            }
        }
        return result;
    }

    /**
     * In some cases, the jar dependencies of a target might be injected by bazel or rules and not are not
     * available via `deps` field of a target. For this reason, we read JavaOutputInfo's jdeps file and
     * filter out jars that have not been included in the target's `deps` list.
     * <p>
     * The old Bazel Plugin performs similar step here
     * https://github.com/bazelbuild/intellij/blob/b68ec8b33aa54ead6d84dd94daf4822089b3b013/java/src/com/google/idea/blaze/java/sync/importer/BlazeJavaWorkspaceImporter.java#L256
     */
    private Map<WorkspaceTargetKey, List<LibraryItem>> jdepsLibraries(
        Map<WorkspaceTargetKey, BuildTarget> targetsToImport,
        Map<WorkspaceTargetKey, List<LibraryItem>> libraryDependencies,
        Map<WorkspaceTargetKey, Set<Path>> interfacesAndBinariesFromTargetsToImport
    ) {
        Map<WorkspaceTargetKey, Set<Path>> targetsToJdepsJars =
            getAllJdepsDependencies(targetsToImport, libraryDependencies);
        HashMap<WorkspaceTargetKey, LibraryItem> libraryNameToLibraryValueMap = new HashMap<>();
        Map<WorkspaceTargetKey, List<LibraryItem>> result = new LinkedHashMap<>();
        for (Map.Entry<WorkspaceTargetKey, Set<Path>> target : targetsToJdepsJars.entrySet()) {
            Set<Path> interfacesAndBinariesFromTarget =
                interfacesAndBinariesFromTargetsToImport.getOrDefault(target.getKey(), Set.of());
            List<LibraryItem> libraries = new ArrayList<>();
            for (Path it : target.getValue()) {
                if (interfacesAndBinariesFromTarget.contains(it)) {
                    continue;
                }
                // the synthetic label was precomputed by the plugin (it needs `bazelBin`, only available at map time)
                Label label = myJdepsLabelByJar.get(it);
                if (label == null) {
                    continue;
                }
                WorkspaceTargetKey key = new WorkspaceTargetKey(label);
                libraries.add(libraryNameToLibraryValueMap.computeIfAbsent(key, ignored ->
                    createLibrary(
                        key,
                        Set.of(),
                        Set.of(it),
                        Set.of()
                    )
                ));
            }
            result.put(target.getKey(), libraries);
        }
        return result;
    }

    private Map<WorkspaceTargetKey, Set<Path>> getAllJdepsDependencies(
        Map<WorkspaceTargetKey, BuildTarget> targetsToImport,
        Map<WorkspaceTargetKey, List<LibraryItem>> libraryDependencies
    ) {
        Map<WorkspaceTargetKey, Set<Path>> jdepsJars = new LinkedHashMap<>();
        targetsToImport.forEach((key, target) -> {
            JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
            Set<Path> jars = new LinkedHashSet<>();
            if (jvmBuildTarget != null) {
                for (JdepsJar it : jvmBuildTarget.jdepsJars()) {
                    Path jar = myResolveLocation.apply(it.jar());
                    if (jar != null) {
                        jars.add(jar);
                    }
                }
            }
            if (!jars.isEmpty()) {
                jdepsJars.put(key, jars);
            }
        });

        Set<Path> allJdepsJars = new HashSet<>();
        for (Set<Path> it : jdepsJars.values()) {
            allJdepsJars.addAll(it);
        }

        Map<WorkspaceTargetKey, Set<Path>> outputJarsFromTransitiveDepsCache = new HashMap<>();
        Map<WorkspaceTargetKey, Set<Path>> result = new LinkedHashMap<>();
        jdepsJars.forEach((targetKey, jarsFromJdeps) -> {
            Set<Path> transitiveJdepsJars =
                getJdepsJarsFromTransitiveDependencies(
                    targetKey,
                    targetsToImport,
                    libraryDependencies,
                    outputJarsFromTransitiveDepsCache,
                    allJdepsJars,
                    new HashSet<>()
                );
            Set<Path> jars = new LinkedHashSet<>(jarsFromJdeps);
            jars.removeAll(transitiveJdepsJars);
            if (!jars.isEmpty()) {
                result.put(targetKey, jars);
            }
        });
        return result;
    }

    private Set<Path> getJdepsJarsFromTransitiveDependencies(
        WorkspaceTargetKey target,
        Map<WorkspaceTargetKey, BuildTarget> targetsToImport,
        Map<WorkspaceTargetKey, List<LibraryItem>> libraryDependencies,
        Map<WorkspaceTargetKey, Set<Path>> outputJarsFromTransitiveDepsCache,
        Set<Path> allJdepsJars,
        Set<WorkspaceTargetKey> visited
    ) {
        Set<Path> cached = outputJarsFromTransitiveDepsCache.get(target);
        if (cached != null) {
            return cached;
        }

        if (!visited.add(target)) { // prevent STOFL if dependency cycle
            return Set.of();
        }

        BuildTarget buildTarget = targetsToImport.get(target);
        List<Path> jarsFromTargets = new ArrayList<>();
        if (buildTarget != null) {
            jarsFromTargets.addAll(getTargetOutputJarsList(buildTarget));
            jarsFromTargets.addAll(getTargetInterfaceJarsList(buildTarget));
        }
        List<LibraryItem> targetLibraries = libraryDependencies.getOrDefault(target, List.of());
        for (LibraryItem it : targetLibraries) {
            jarsFromTargets.addAll(WorkspaceLibraries.getAllJars(it));
        }
        Set<Path> outputJars = new LinkedHashSet<>();
        for (Path it : jarsFromTargets) {
            if (allJdepsJars.contains(it)) {
                outputJars.add(it);
            }
        }

        List<WorkspaceTargetKey> dependencies = new ArrayList<>();
        if (buildTarget != null) {
            for (DependencyLabel it : dependencies(buildTarget)) {
                dependencies.add(it.targetKey());
            }
        }
        for (LibraryItem it : targetLibraries) {
            if (!it.key().equals(target)) {
                dependencies.add(it.key());
            }
        }

        for (WorkspaceTargetKey dependency : dependencies) {
            outputJars.addAll(getJdepsJarsFromTransitiveDependencies(
                dependency,
                targetsToImport,
                libraryDependencies,
                outputJarsFromTransitiveDepsCache,
                allJdepsJars,
                visited
            ));
        }

        outputJarsFromTransitiveDepsCache.put(target, outputJars);
        return outputJars;
    }

    private @Nullable LibraryItem createLibrary(
        WorkspaceTargetKey key,
        BuildTarget target
    ) {
        Set<Path> outputs = new LinkedHashSet<>(getTargetOutputJarsList(target));
        outputs.addAll(getIntellijPluginJars(target));
        Set<Path> rawSources = getSourceJarPaths(target);
        Set<Path> sources = new LinkedHashSet<>(rawSources);
        sources.removeAll(outputs);

        Set<Path> interfaceJars = new LinkedHashSet<>(getTargetInterfaceJarsList(target));
        if (isEmptyJarList(outputs) && isEmptyJarList(interfaceJars) && isEmptyJarList(sources)) {
            return null;
        }

        MavenCoordinates mavenCoordinates = MavenCoordinatesResolver.fromTargetTagsList(target.getTags());
        if (mavenCoordinates == null && !outputs.isEmpty()) {
            Path outputJar = outputs.iterator().next();
            mavenCoordinates = MavenCoordinatesResolver.resolveMavenCoordinates(key.label(), outputJar);
        }

        return createLibrary(
            key,
            interfaceJars,
            outputs,
            sources,
            mavenCoordinates,
            containsAnyInternalJars(target)
        );
    }

    private static LibraryItem createLibrary(
        WorkspaceTargetKey key,
        Collection<Path> ijars,
        Collection<Path> jars,
        Collection<Path> sourceJars
    ) {
        return createLibrary(key, ijars, jars, sourceJars, null, false);
    }

    private static LibraryItem createLibrary(
        WorkspaceTargetKey key,
        Collection<Path> ijars,
        Collection<Path> jars,
        Collection<Path> sourceJars,
        @Nullable MavenCoordinates mavenCoordinates,
        boolean containsInternalJars
    ) {
        return new LibraryItem(
            key,
            List.copyOf(ijars),
            List.copyOf(jars),
            List.copyOf(sourceJars),
            mavenCoordinates,
            containsInternalJars
        );
    }

    private static boolean isEmptyJarList(Collection<Path> receiver) {
        if (receiver.isEmpty()) {
            return true;
        }
        if (receiver.size() != 1) {
            return false;
        }
        Path fileName = receiver.iterator().next().getFileName();
        return fileName != null && fileName.toString().equals("empty.jar");
    }

    private Set<Path> getIntellijPluginJars(BuildTarget target) {
        JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
        return resolvePathSet(jvmBuildTarget != null ? jvmBuildTarget.intellijPluginJars() : null);
    }

    private Set<Path> getSourceJarPaths(BuildTarget target) {
        JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
        return resolvePathSet(jvmBuildTarget != null ? jvmBuildTarget.outputSourceJars() : null);
    }

    private List<Path> getTargetOutputJarsList(BuildTarget target) {
        // proto generator put the generated jar into `javaProvider.fullCompileJarsList`
        // See test `plugins/bazel/integrationTests/testProjects/protobufStrictDepsTest`
        if (target.getKind().kind().equals("scala_proto_library")) {
            JavaProviderData javaProviderData = WorkspaceHelpers.findBuildData(target, JavaProviderData.class);
            return javaProviderData != null ? JvmTargetEntitiesBuilder.resolvePaths(javaProviderData.fullCompileJars(), myResolveLocation) : List.of();
        }

        return new ArrayList<>(outputBinaryJars(target));
    }

    private Set<Path> outputBinaryJars(BuildTarget receiver) {
        JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(receiver, JvmBuildTarget.class);
        return resolvePathSet(jvmBuildTarget != null ? jvmBuildTarget.binaryOutputs() : null);
    }

    private List<Path> getTargetInterfaceJarsList(BuildTarget target) {
        JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
        return jvmBuildTarget != null ? JvmTargetEntitiesBuilder.resolvePaths(jvmBuildTarget.outputInterfaceJars(), myResolveLocation) : List.of();
    }

    private Set<Path> resolvePathSet(@Nullable OutputLocationCollection receiver) {
        return receiver != null ? new LinkedHashSet<>(JvmTargetEntitiesBuilder.resolvePaths(receiver, myResolveLocation)) : Set.of();
    }

    private boolean containsAnyInternalJars(BuildTarget target) {
        JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
        return jvmBuildTarget != null && containsInternalJars(jvmBuildTarget, myRepoMapping);
    }

    private Map<WorkspaceTargetKey, Set<Path>> collectInterfacesAndClasses(Collection<BuildTarget> targets) {
        Map<WorkspaceTargetKey, Set<Path>> result = new LinkedHashMap<>();
        for (BuildTarget target : targets) {
            Set<Path> jars = new LinkedHashSet<>(getTargetInterfaceJarsList(target));
            jars.addAll(getTargetOutputJarsList(target));
            result.put(target.getKey(), jars);
        }
        return result;
    }

    private List<Path> scalatestClasspathJars(BuildTarget receiver) {
        ScalaBuildTarget scala = WorkspaceHelpers.findBuildData(receiver, ScalaBuildTarget.class);
        if (scala == null) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        for (Label label : scala.scalatestClasspathTargets()) {
            WorkspaceTargetKey key = new WorkspaceTargetKey(label, receiver.getKey().configuration(), receiver.getKey().aspectIds());
            BuildTarget target = myAllTargets.get(key);
            JavaProviderData javaProviderData = target != null ? WorkspaceHelpers.findBuildData(target, JavaProviderData.class) : null;
            if (javaProviderData != null) {
                result.addAll(JvmTargetEntitiesBuilder.resolvePaths(javaProviderData.fullCompileJars(), myResolveLocation));
            }
        }
        return result;
    }

    private static <K, V> Map<K, List<V>> concatenateMaps(Collection<Map<K, List<V>>> maps) {
        Set<K> keys = new LinkedHashSet<>();
        for (Map<K, List<V>> it : maps) {
            keys.addAll(it.keySet());
        }
        Map<K, List<V>> result = new LinkedHashMap<>();
        for (K key : keys) {
            List<V> values = new ArrayList<>();
            for (Map<K, List<V>> it : maps) {
                values.addAll(it.getOrDefault(key, List.of()));
            }
            result.put(key, values);
        }
        return result;
    }

    private static MavenCoordinatesKey toKey(MavenCoordinates receiver, @Nullable WorkspaceConfigurationId configuration) {
        return new MavenCoordinatesKey(receiver, configuration);
    }
}

record MavenCoordinatesKey(
    MavenCoordinates coordinates,
    @Nullable WorkspaceConfigurationId configuration
) {
}
