package org.jetbrains.bazel.workspace.importer;

import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.util.interner.Interner;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.DependencyLabel.DependencyLabelKind;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JvmDependency;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.target.BuildTargetUtils;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Resolves per-target JVM dependencies (including the strict-dependency export closure).
 * <p>
 * Intended construction lifecycle: one {@link DependencyBuilder} per import pass, shared across all
 * per-target builds so the strict-dependency closure is memoized once.
 */
// RC: replaces the dependency-resolution and exported-closure slice of `ProjectDetailsToModuleDetailsTransformer`
// (incl. its `DependenciesClosure` memo), plus the `Dependency` wrapper
public class DependencyBuilder {
    // RC: those were present in original MMM
    private static final Interner<ModuleDependencyItem> dependencyInterner = Interner.createWeakInterner();
    // consulo: upstream also interns the LibraryId symbolic ids (idInterner), here the library id is its name

    private final Collection<BuildTarget> myTargets;
    private final Map<WorkspaceTargetKey, JvmResolvedTarget> myJvmResolved;
    private final RepoMapping myRepoMapping;
    private final Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> myLibraryShadowedProducers;

    private final Map<WorkspaceTargetKey, List<Label>> myStrictDependencies;

    public DependencyBuilder(
        Collection<BuildTarget> targets,
        Map<WorkspaceTargetKey, JvmResolvedTarget> jvmResolved,
        RepoMapping repoMapping
    ) {
        this(targets, jvmResolved, repoMapping, Map.of());
    }

    public DependencyBuilder(
        Collection<BuildTarget> targets,
        Map<WorkspaceTargetKey, JvmResolvedTarget> jvmResolved,
        RepoMapping repoMapping,
        Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> libraryShadowedProducers
    ) {
        myTargets = targets;
        myJvmResolved = jvmResolved;
        myRepoMapping = repoMapping;
        myLibraryShadowedProducers = libraryShadowedProducers;
        myStrictDependencies = calculateExportedDependenciesTransitiveClosure();
    }

    public record Resolved(
        List<DependencyLabel> dependencies,
        StrictDependencyCheckedType strictDependenciesCheck,
        List<Label> strictDependencies
    ) {
    }

    public Resolved resolve(BuildTarget target) {
        JvmResolvedTarget resolved = myJvmResolved.get(stripAspects(target.getKey()));
        List<DependencyLabel> deps;
        if (resolved != null) {
            LinkedHashSet<DependencyLabel> result = new LinkedHashSet<>();
            for (JvmDependency dep : resolved.jvmDependencies()) {
                // TODO: investigate "experimental_prune_transitive_deps" option in .bazelrc
                if (dep instanceof JvmDependency.ModuleDependency moduleDependency) {
                    // TODO: Ultimate monorepo specifics. Drop shortly
                    if (target.getKind().kind().equals("jvm_library") || target.getKind().kind().equals("_jvm_library_jps")) {
                        result.add(moduleDependency.dependency());
                    }
                    else {
                        result.add(export(moduleDependency.dependency()));
                    }
                }
                else if (dep instanceof JvmDependency.LibraryDependency libraryDependency) {
                    for (DependencyLabel it : redirectLibraryShadow(target.getKey(), libraryDependency.dependency())) {
                        result.add(export(it));
                    }
                }
            }
            deps = new ArrayList<>(result);
        }
        else {
            deps = target.getDependencies();
        }
        List<Label> strictDependencies = myStrictDependencies.get(stripAspects(target.getKey()));
        return new Resolved(
            deps,
            checkStrictDependencies(target),
            strictDependencies != null ? strictDependencies : List.of()
        );
    }

    private StrictDependencyCheckedType checkStrictDependencies(BuildTarget target) {
        if (!BuildTargetUtils.isWorkspace(target, myRepoMapping)) {
            return StrictDependencyCheckedType.OFF;
        }
        JvmBuildTarget jvmBuildTarget = JvmBuildTarget.extractJvmBuildTarget(target);
        return jvmBuildTarget != null ? jvmBuildTarget.checkStrictDependencies() : StrictDependencyCheckedType.OFF;
    }

    private Map<WorkspaceTargetKey, List<Label>> calculateExportedDependenciesTransitiveClosure() {
        List<WorkspaceTargetKey> strictTargets = myTargets.stream()
            .filter(it -> checkStrictDependencies(it) != StrictDependencyCheckedType.OFF)
            .map(it -> stripAspects(it.getKey()))
            .toList();

        Function<WorkspaceTargetKey, List<DependencyLabel>> fDependencies = key -> {
            JvmResolvedTarget resolved = myJvmResolved.get(key);
            if (resolved == null) {
                return List.of();
            }
            LinkedHashSet<DependencyLabel> result = new LinkedHashSet<>();
            for (JvmDependency it : resolved.jvmDependencies()) {
                for (DependencyLabel dependency : redirectLibraryShadow(key, it.dependency())) {
                    if (!dependency.targetKey().label().equals(key.label()) /* filter out module dependency on library */) {
                        result.add(dependency);
                    }
                }
            }
            return new ArrayList<>(result);
        };

        DependenciesClosure exportedDependenciesClosure = new DependenciesClosure(key ->
            fDependencies.apply(key).stream().filter(DependencyLabel::getExported).map(it -> stripAspects(it.targetKey())).toList()
        );

        Map<WorkspaceTargetKey, List<Label>> result = new LinkedHashMap<>();
        for (WorkspaceTargetKey targetKey : strictTargets) {
            LinkedHashSet<Label> labels = new LinkedHashSet<>();
            for (DependencyLabel it : fDependencies.apply(targetKey)) {
                for (WorkspaceTargetKey key : exportedDependenciesClosure.get(stripAspects(it.targetKey()))) {
                    labels.add(key.label());
                }
                labels.add(it.targetKey().label());
            }
            result.put(targetKey, new ArrayList<>(labels));
        }
        return result;
    }

    private static class DependenciesClosure {
        private final Function<WorkspaceTargetKey, List<WorkspaceTargetKey>> myDeps;
        private final HashMap<WorkspaceTargetKey, Set<WorkspaceTargetKey>> myCache = new HashMap<>();

        DependenciesClosure(Function<WorkspaceTargetKey, List<WorkspaceTargetKey>> deps) {
            myDeps = deps;
        }

        Set<WorkspaceTargetKey> get(WorkspaceTargetKey key) {
            Set<WorkspaceTargetKey> cached = myCache.get(key);
            if (cached != null) {
                return cached;
            }
            // consulo: getOrPut computes the value before storing it, so the recursion does not touch the map while iterating
            List<WorkspaceTargetKey> ds = myDeps.apply(key);
            Set<WorkspaceTargetKey> value = new LinkedHashSet<>(ds);
            for (WorkspaceTargetKey it : ds) {
                value.addAll(get(it));
            }
            myCache.put(key, value);
            return value;
        }
    }

    // Adds a dependency on each source module that produces the library's shadowed
    // jars, and keeps the library dependency for the jars that remain. A library
    // that source modules replace in full holds no jars, so it is absent from the
    // project libraries and its dependency resolves to nothing. A dependency that
    // shadows nothing stays unchanged.
    private List<DependencyLabel> redirectLibraryShadow(WorkspaceTargetKey consumer, DependencyLabel dep) {
        List<WorkspaceTargetKey> producers = myLibraryShadowedProducers.get(dep.targetKey());
        if (producers == null) {
            return List.of(dep);
        }
        List<DependencyLabel> result = new ArrayList<>();
        for (WorkspaceTargetKey it : producers) {
            if (!stripAspects(it).equals(stripAspects(consumer))) {
                result.add(new DependencyLabel(it, dep.kind()));
            }
        }
        result.add(dep);
        return result;
    }

    private static DependencyLabel export(DependencyLabel receiver) {
        return receiver.kind() == DependencyLabelKind.COMPILE ? new DependencyLabel(receiver.targetKey(), DependencyLabelKind.EXPORTED_COMPILE_TIME) : receiver;
    }

    public static WorkspaceTargetKey stripAspects(WorkspaceTargetKey receiver) {
        return new WorkspaceTargetKey(receiver.label(), receiver.configuration(), WorkspaceAspectIds.EMPTY);
    }

    public static ModuleDependencyItem.LibraryDependency toLibraryDependency(String libraryName, boolean exported) {
        return toLibraryDependency(libraryName, exported, DependencyScope.COMPILE);
    }

    public static ModuleDependencyItem.LibraryDependency toLibraryDependency(
        String libraryName,
        boolean exported,
        DependencyScope scope
    ) {
        return (ModuleDependencyItem.LibraryDependency) dependencyInterner.intern(
            new ModuleDependencyItem.LibraryDependency(
                // treat all libraries as project-level libraries
                libraryName,
                exported,
                scope
            )
        );
    }

    /**
     * consulo: replacement of the workspace model {@code ModuleDependencyItem}, applied to the module root model by
     * {@link JvmTargetEntitiesBuilder}. Libraries and modules are referenced by name, like upstream {@code LibraryId} /
     * {@code ModuleId}. {@code ModuleSourceDependency} is implicit in Consulo.
     */
    public sealed interface ModuleDependencyItem
        permits ModuleDependencyItem.LibraryDependency, ModuleDependencyItem.ModuleDependency, ModuleDependencyItem.SdkDependency {

        record LibraryDependency(String library, boolean exported, DependencyScope scope) implements ModuleDependencyItem {
        }

        record ModuleDependency(String module, boolean exported, DependencyScope scope, boolean productionOnTest)
            implements ModuleDependencyItem {
        }

        // Java SDK of the module, upstream SdkDependency(SdkId(name, "JavaSDK"))
        record SdkDependency(String sdkName) implements ModuleDependencyItem {
        }
    }
}
