// consulo: upstream server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/languages/rust/RustDependencyResolver.kt (removed
// in 4699fc65a)
package org.jetbrains.bazel.rust.sync;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.rust.protocol.RustDepKind;
import org.jetbrains.bazel.rust.protocol.RustDepKindInfo;
import org.jetbrains.bazel.rust.protocol.RustDependency;
import org.jetbrains.bazel.rust.protocol.RustPackage;
import org.jetbrains.bazel.rust.protocol.RustRawDependency;
import org.jetbrains.bazel.rust.protocol.RustTarget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class RustDependencyResolver {
    // consulo: upstream top-level data class RustDependencies of RustDependencyResolver.kt
    public record RustDependencies(Map<String, List<RustDependency>> dependencies, Map<String, List<RustRawDependency>> rawDependencies) {
    }

    // consulo: upstream Kotlin Pair<String, List<T>> of the resolvers
    private record PackageDependencies<T>(String packageId, List<T> dependencies) {
    }

    public RustDependencyResolver() {
    }

    // We need to resolve all dependencies and provide a list of new bazel targets
    // to be transformed into packages.
    public RustDependencies rustDependencies(List<RustPackage> rustPackages, List<Module> rustBspTargets) {
        Map<RustPackage, List<Module>> associatedBspTargets = groupBspTargetsByPackage(rustBspTargets, rustPackages);
        Map<RustPackage, List<Module>> associatedRawBspTargets = groupBspRawTargetsByPackage(rustBspTargets, rustPackages);

        Map<String, List<RustDependency>> rustDependencies = resolveDependencies(associatedBspTargets);
        Map<String, List<RustRawDependency>> rustRawDependencies = resolveRawDependencies(associatedRawBspTargets);

        return new RustDependencies(rustDependencies, rustRawDependencies);
    }

    private Map<RustPackage, List<Module>> groupBspTargetsByPackage(List<Module> rustBspTargets, List<RustPackage> rustPackages) {
        // consulo: Kotlin associateBy keeps the last element of equal keys
        Map<Label, Module> rustBspTargetsMappedToLabel = new LinkedHashMap<>();
        for (Module it : rustBspTargets) {
            rustBspTargetsMappedToLabel.put(it.label(), it);
        }
        Map<RustPackage, List<Module>> result = new LinkedHashMap<>();
        for (RustPackage it : rustPackages) {
            List<Module> modules = new ArrayList<>();
            for (RustTarget pkgTarget : it.resolvedTargets()) {
                Module module = rustBspTargetsMappedToLabel.get(Label.parse(it.id() + ":" + pkgTarget.name()));
                if (module != null) {
                    modules.add(module);
                }
            }
            result.put(it, modules);
        }
        return result;
    }

    private Map<RustPackage, List<Module>> groupBspRawTargetsByPackage(List<Module> rustBspTargets, List<RustPackage> rustPackages) {
        Map<RustPackage, List<Module>> result = new LinkedHashMap<>();
        for (RustPackage pkg : rustPackages) {
            // consulo: upstream `it.label.packagePath.toString()`, see Module.packagePath
            result.put(pkg, rustBspTargets.stream().filter(it -> Module.packagePath(it.label()).equals(pkg.id())).toList());
        }
        return result;
    }

    private Map<String, List<RustDependency>> resolveDependencies(Map<RustPackage, List<Module>> associatedBspTargets) {
        return resolveRustDependencies(associatedBspTargets, this::resolveBspDependencies);
    }

    private PackageDependencies<RustDependency> resolveBspDependencies(RustPackage rustPackage, List<Label> directDependencies) {
        List<RustDependency> dependencies =
            directDependencies.stream()
                .map(this::createDependency)
                .filter(it -> !rustPackage.id().equals(it.pkg()))
                .toList();
        return new PackageDependencies<>(rustPackage.id(), dependencies);
    }

    private RustDependency createDependency(Label bazelPackageTargetInfo) {
        RustDependency dep =
            new RustDependency(
                // consulo: upstream `bazelPackageTargetInfo.packagePath.toString()`, the id of the package, see Module.packagePath
                Module.packagePath(bazelPackageTargetInfo),
                bazelPackageTargetInfo.getTargetName(),
                List.of(new RustDepKindInfo(RustDepKind.NORMAL))
            );
        return dep;
    }

    private Map<String, List<RustRawDependency>> resolveRawDependencies(Map<RustPackage, List<Module>> associatedRawBspTargets) {
        return resolveRustDependencies(associatedRawBspTargets, this::resolveRawBspDependencies);
    }

    private PackageDependencies<RustRawDependency> resolveRawBspDependencies(RustPackage rustPackage, List<Label> directDependencies) {
        return new PackageDependencies<>(
            rustPackage.id(),
            directDependencies.stream()
                .map(it ->
                    new RustRawDependency(
                        it.toString(),
                        false,
                        true,
                        Set.<String>of()
                    )
                )
                .toList()
        );
    }

    private <T> Map<String, List<T>> resolveRustDependencies(
        Map<RustPackage, List<Module>> associatedBspTargets,
        BiFunction<RustPackage, List<Label>, PackageDependencies<T>> resolver
    ) {
        // consulo: Kotlin `toMap()` keeps the last pair of equal keys: the dependencies of the last target of a package win
        Map<String, List<T>> map = new LinkedHashMap<>();
        for (Map.Entry<RustPackage, List<Module>> it : associatedBspTargets.entrySet()) {
            for (PackageDependencies<T> pair : resolvePackageDependencies(it.getKey(), it.getValue(), resolver)) {
                map.put(pair.packageId(), pair.dependencies());
            }
        }
        Map<String, List<T>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<T>> entry : map.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private <T> List<PackageDependencies<T>> resolvePackageDependencies(
        RustPackage rustPackage,
        List<Module> bspTargets,
        BiFunction<RustPackage, List<Label>, PackageDependencies<T>> resolver
    ) {
        return bspTargets.stream()
            .map(bspTarget -> resolver.apply(rustPackage, bspTarget.directDependencies()))
            .toList();
    }
}
