package org.jetbrains.bazel.workspace.importer;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.GlobalNamingContextBuilder;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameProducer;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameSpace;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetMerger;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTargetTag;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.WorkspaceLibraries.LibraryItem;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class JvmImportPlan {
    public static final NameProducer JVM_NAME_PRODUCER = new NameProducer("jvm");

    // merge aspect-only duplicates so the whole pipeline sees one target per (label, configuration);
    // `mergeByTargetKey` strips the aspect ids, so every key below is already aspect-free
    private final List<BuildTarget> myTargets;

    private final List<LibraryItem> myAllLibraries;

    private final Map<Path, WorkspaceTargetKey> myJarToSourceModule;

    // Each library maps to the distinct source modules that produce some of its
    // jars. The import replaces those jars with a module dependency.
    private final Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> myLibraryShadowedProducers;

    private final List<LibraryItem> myLibraries;

    private final List<WorkspaceTargetKey> myModuleKeys;

    private final List<WorkspaceTargetKey> myLibraryKeys;

    private final Map<WorkspaceTargetKey, String> myPreferredModuleNames;

    public JvmImportPlan(
        Collection<BuildTarget> rawTargets,
        Map<WorkspaceTargetKey, JvmResolvedTarget> jvmResolved,
        Function<OutputLocation, @Nullable Path> resolveLocation
    ) {
        myTargets = new WorkspaceTargetMerger(JvmMergeFunctions.jvmTargetMergeFunctions).mergeByTargetKey(rawTargets);

        Map<WorkspaceTargetKey, LibraryItem> allLibraries = new LinkedHashMap<>();
        for (JvmResolvedTarget resolved : jvmResolved.values()) {
            for (LibraryItem it : resolved.libraries()) {
                allLibraries.putIfAbsent(it.key(), it);
            }
        }
        myAllLibraries = new ArrayList<>(allLibraries.values());

        // build `jar -> source module` map
        Map<Path, List<WorkspaceTargetKey>> owners = new LinkedHashMap<>();
        for (BuildTarget target : myTargets) {
            if (target.getSources().getOutputLocations().noneMatch(JvmTargetEntitiesBuilder::hasJvmSourceExtension)) {
                continue;
            }
            JvmBuildTarget jvmTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
            if (jvmTarget == null) {
                continue;
            }
            List<Path> jars = new ArrayList<>(JvmTargetEntitiesBuilder.resolvePaths(jvmTarget.binaryOutputs(), resolveLocation));
            jars.addAll(JvmTargetEntitiesBuilder.resolvePaths(jvmTarget.outputInterfaceJars(), resolveLocation));
            for (Path jar : jars) {
                owners.computeIfAbsent(jar, it -> new ArrayList<>()).add(target.getKey());
            }
        }
        // keep only single `jar -> source module` mapping
        Map<Path, WorkspaceTargetKey> jarToSourceModule = new LinkedHashMap<>();
        for (Map.Entry<Path, List<WorkspaceTargetKey>> entry : owners.entrySet()) {
            Set<WorkspaceTargetKey> distinct = new LinkedHashSet<>(entry.getValue());
            if (distinct.size() == 1) {
                jarToSourceModule.put(entry.getKey(), distinct.iterator().next());
            }
        }
        myJarToSourceModule = jarToSourceModule;

        Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> libraryShadowedProducers = new LinkedHashMap<>();
        for (LibraryItem lib : myAllLibraries) {
            Set<WorkspaceTargetKey> producers = new LinkedHashSet<>();
            List<Path> allJars = new ArrayList<>(lib.jars());
            allJars.addAll(lib.ijars());
            for (Path it : allJars) {
                WorkspaceTargetKey producer = shadowingProducerOf(lib, it);
                if (producer != null) {
                    producers.add(producer);
                }
            }
            if (!producers.isEmpty()) {
                libraryShadowedProducers.put(lib.key(), new ArrayList<>(producers));
            }
        }
        myLibraryShadowedProducers = libraryShadowedProducers;

        List<LibraryItem> libraries = new ArrayList<>();
        for (LibraryItem lib : myAllLibraries) {
            if (!myLibraryShadowedProducers.containsKey(lib.key())) {
                libraries.add(lib);
                continue;
            }
            // Keep only the jars that no other source module produces. The shadowed
            // jars come from module dependencies instead. See [JavaCustomPackagingTest]
            List<Path> keptJars = lib.jars().stream().filter(it -> shadowingProducerOf(lib, it) == null).toList();
            List<Path> keptIjars = lib.ijars().stream().filter(it -> shadowingProducerOf(lib, it) == null).toList();
            if (keptJars.isEmpty() && keptIjars.isEmpty()) {
                continue;
            }
            libraries.add(new LibraryItem(lib.key(), keptIjars, keptJars, lib.sourceJars(), lib.mavenCoordinates(), lib.containsInternalJars()));
        }
        myLibraries = libraries;

        myModuleKeys = myTargets.stream()
            .filter(it -> JavaLanguageClass.isJvmTarget(it.getKind()))
            .map(BuildTarget::getKey)
            .distinct()
            .toList();

        myLibraryKeys = myLibraries.stream().map(LibraryItem::key).distinct().toList();

        Map<WorkspaceTargetKey, String> preferredModuleNames = new HashMap<>();
        for (BuildTarget target : myTargets) {
            String preferred = preferredModuleName(target);
            if (preferred != null) {
                preferredModuleNames.put(target.getKey(), preferred);
            }
        }
        myPreferredModuleNames = preferredModuleNames;
    }

    public List<BuildTarget> getTargets() {
        return myTargets;
    }

    // A jar shadows a library entry when a source module (not the library's own
    // module) produces it. That jar belongs to the module, not to the library.
    // Own-module jars stay. Annotation-processor output is a self-referential
    // library, so it must not shadow itself.
    private @Nullable WorkspaceTargetKey shadowingProducerOf(LibraryItem receiver, Path jar) {
        WorkspaceTargetKey producer = myJarToSourceModule.get(jar);
        if (producer == null || DependencyBuilder.stripAspects(producer).equals(DependencyBuilder.stripAspects(receiver.key()))) {
            return null;
        }
        return producer;
    }

    public Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> getLibraryShadowedProducers() {
        return myLibraryShadowedProducers;
    }

    public List<LibraryItem> getLibraries() {
        return myLibraries;
    }

    public List<WorkspaceTargetKey> getModuleKeys() {
        return myModuleKeys;
    }

    public List<WorkspaceTargetKey> getLibraryKeys() {
        return myLibraryKeys;
    }

    public void declareNames(GlobalNamingContextBuilder naming) {
        for (WorkspaceTargetKey key : myModuleKeys) {
            naming.declare(NameSpace.MODULE, JVM_NAME_PRODUCER, key, myPreferredModuleNames.get(key));
        }
        for (WorkspaceTargetKey key : myLibraryKeys) {
            naming.declare(NameSpace.LIBRARY, JVM_NAME_PRODUCER, key);
        }
    }

    private static @Nullable String preferredModuleName(BuildTarget receiver) {
        // first try name hint from `BuildTargetTag.MODULE_NAME_HINT`
        // parse following tag: ide-module-name=<module-name>
        List<String> hints = receiver.getTags().stream().filter(it -> it.startsWith(BuildTargetTag.MODULE_NAME_HINT + "=")).toList();
        if (hints.size() == 1) {
            String hint = hints.get(0);
            String name = hint.substring(hint.indexOf('=') + 1);
            if (!name.isBlank()) {
                return name;
            }
        }

        // rules_kotlin `moduleName` attribute, but only when the BUILD file sets it explicitly
        KotlinBuildTarget kotlinTarget = WorkspaceHelpers.findBuildData(receiver, KotlinBuildTarget.class);
        String moduleName = kotlinTarget == null ? null : kotlinTarget.moduleName();
        if (kotlinTarget != null && moduleName != null) {
            KotlinBuildTarget kotlin = kotlinTarget;
            String name = moduleName;
            boolean accepted = !name.isBlank() &&
                !isRulesKotlinDerivedModuleName(receiver.getKey().label(), name) &&
                // a target with `associates` compiles into the associate module, so it reports the associate name
                kotlin.associates().stream().noneMatch(it -> isRulesKotlinDerivedModuleName(it.label(), name));
            if (!accepted) {
                moduleName = null;
            }
        }
        if (moduleName != null) {
            // TODO: remove test classification naming after fixing jps-to-bazel to not duplicate `moduleName`
            return WorkspaceTarget.isTestTarget(receiver) ? moduleName + "-test" : moduleName;
        }

        // fallback to default module name resolver
        return null;
    }

    // rules_kotlin always provide `moduleName` regardless of user setting explicitly or not
    // we only want to use `moduleName` as module naming hint when it's NOT the rules_kotlin provided one
    // implemented based on `kotlin/internal/utils/utils.bzl`
    private static boolean isRulesKotlinDerivedModuleName(Label receiver, String moduleName) {
        String packagePart = String.join("_", receiver.packagePath().pathSegments());
        String targetPart = receiver.getTargetName().replace('/', '_');
        return moduleName.equals(packagePart + "-" + targetPart) || (packagePart.isEmpty() && moduleName.equals(targetPart));
    }

}
