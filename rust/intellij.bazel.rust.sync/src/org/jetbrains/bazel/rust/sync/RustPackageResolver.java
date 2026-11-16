// consulo: upstream server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/languages/rust/RustPackageResolver.kt (removed in
// 4699fc65a)
package org.jetbrains.bazel.rust.sync;

import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.rust.protocol.RustCrateType;
import org.jetbrains.bazel.rust.protocol.RustPackage;
import org.jetbrains.bazel.rust.protocol.RustTarget;
import org.jetbrains.bazel.rust.protocol.RustTargetKind;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

// consulo: upstream `class RustPackageResolver(val bazelPathsResolver: BazelPathsResolver)`; the OutputLocations of the
// RustModules are resolved with the resolver of the importer
public class RustPackageResolver {
    private final Function<OutputLocation, @Nullable Path> myResolveLocation;

    public RustPackageResolver(Function<OutputLocation, @Nullable Path> resolveLocation) {
        myResolveLocation = resolveLocation;
    }

    private record RustTargetModule(Module module, RustModule rustModule) {
    }

    private record Version(
        String major,
        String minor,
        String patch,
        @Nullable String preRelease
    ) {
        // consulo: overload for the upstream default arguments
        Version(String major) {
            this(major, "", "", "");
        }
    }

    public List<RustPackage> rustPackages(List<Module> rustBspTargets) {
        Map<String, List<Module>> grouped = new LinkedHashMap<>();
        for (Module it : rustBspTargets) {
            grouped.computeIfAbsent(Module.packagePath(it.label()), key -> new ArrayList<>()).add(it);
        }
        List<RustPackage> result = new ArrayList<>();
        for (Map.Entry<String, List<Module>> it : grouped.entrySet()) {
            result.add(resolveSinglePackage(it));
        }
        return result;
    }

    private RustPackage resolveSinglePackage(Map.Entry<String, List<Module>> packageData) {
        String rustPackageId = packageData.getKey();
        List<Module> rustTargets = packageData.getValue();
        List<RustTargetModule> allRustTargetsWithData = serveTargetWithRustData(rustTargets);
        // With removed `duplicates` for the same crate root.
        List<RustTargetModule> rustTargetsWithData = removeConflictingRustTargets(allRustTargetsWithData);

        // Targets have already resolved versions, but packages don't,
        // as there is no such thing as package in Rust in Bazel.
        String version = resolvePackageVersion(rustTargetsWithData);
        boolean isFromWorkspace = isPackageFromWorkspace(rustTargetsWithData);
        String origin = resolvePackageOrigin(isFromWorkspace);
        String edition = resolvePackageEdition(rustTargetsWithData);
        String source = resolvePackageSource(isFromWorkspace);
        String pkgBaseDir = resolvePackageBaseDir(rustTargetsWithData);
        String procMacroArtifact = resolveProcMacroArtifact(rustTargetsWithData);
        String rootUrl = resolvePackageRootUrl(rustTargetsWithData);

        List<RustTarget> targets = rustTargetsWithData.stream().map(this::parseSingleTarget).toList();
        List<RustTarget> allTargets = allRustTargetsWithData.stream().map(this::parseSingleTarget).toList();
        Map<String, Set<String>> allFeatures = resolvePackageFeatures(rustTargetsWithData);
        Map<String, String> env = resolvePackageEnv(rustPackageId, pkgBaseDir, version);

        RustPackage rustPackage =
            new RustPackage(
                rustPackageId,
                rootUrl,
                rustPackageId,
                version,
                origin,
                edition,
                targets,
                allTargets,
                allFeatures,
                new LinkedHashSet<>(allFeatures.keySet()),
                source,
                null,
                env,
                null,
                procMacroArtifact
            );
        return rustPackage;
    }

    private List<RustTargetModule> serveTargetWithRustData(List<Module> rustTargets) {
        List<RustTargetModule> result = new ArrayList<>();
        for (Module it : rustTargets) {
            RustModule rustModule = it.languageData();
            if (rustModule == null) {
                continue;
            }
            result.add(new RustTargetModule(it, rustModule));
        }
        return result;
    }

    private List<RustTargetModule> removeConflictingRustTargets(List<RustTargetModule> allRustTargets) {
        Map<OutputLocation, List<RustTargetModule>> grouped = new LinkedHashMap<>();
        for (RustTargetModule it : allRustTargets) {
            grouped.computeIfAbsent(it.rustModule().crateRoot(), key -> new ArrayList<>()).add(it);
        }
        List<RustTargetModule> result = new ArrayList<>();
        for (List<RustTargetModule> targets : grouped.values()) {
            result.add(mergeTargetsWithSameCrateRoot(targets));
        }
        return result;
    }

    private RustTargetModule mergeTargetsWithSameCrateRoot(List<RustTargetModule> rustTargets) {
        if (rustTargets.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge an empty list of targets.");
        }

        //  We need some heuristic here. As we need the proper label, lets take the target with
        //  the most features. It can be changes as a union of features set, but we need
        //  to give it a proper label.
        // consulo: Kotlin maxByOrNull keeps the first of equal elements. A `rust_test(crate = ...)` has the crate root and the
        // features of its library, and the order of the snapshot targets is not the order of the BSP server: of equal targets the
        // one that is not a test is kept, otherwise the package can lose its library target
        RustTargetModule result = rustTargets.get(0);
        for (RustTargetModule it : rustTargets) {
            int features = it.rustModule().crateFeatures().size();
            int resultFeatures = result.rustModule().crateFeatures().size();
            if (features > resultFeatures
                || features == resultFeatures && result.rustModule().kind().equals("test") && !it.rustModule().kind().equals("test")) {
                result = it;
            }
        }
        return result;
    }

    private String resolvePackageVersion(List<RustTargetModule> rustTargets) {
        return rustTargets.stream().map(it -> it.rustModule().version()).max(Comparator.naturalOrder()).orElseThrow();
    }

    private boolean isPackageFromWorkspace(List<RustTargetModule> rustTargets) {
        boolean isFromWorkspace = rustTargets.stream().anyMatch(it -> it.rustModule().fromWorkspace());
        if (!rustTargets.stream().allMatch(it -> it.rustModule().fromWorkspace() == isFromWorkspace)) {
            throw new IllegalArgumentException("All targets within a single package must have the same origin.");
        }
        return isFromWorkspace;
    }

    private String resolvePackageOrigin(boolean isFromWorkspace) {
        if (isFromWorkspace) {
            return "WORKSPACE";
        }
        else {
            return "DEPENDENCY";
        }
    }

    private String resolvePackageEdition(List<RustTargetModule> rustTargets) {
        return rustTargets.stream().map(it -> it.rustModule().edition()).max(Comparator.naturalOrder()).orElseThrow();
    }

    private @Nullable String resolvePackageSource(boolean isFromWorkspace) {
        if (isFromWorkspace) {
            return null;
        }
        else {
            // We assume that the external packages are hosted on the crates.io package registry.
            return "bazel+https://github.com/rust-lang/crates.io-index";
        }
    }

    private String resolvePackageBaseDir(List<RustTargetModule> rustTargets) {
        List<String> baseDirs = rustTargets.stream().map(it -> it.module().baseDirectory()).distinct().toList();
        if (baseDirs.size() != 1) {
            throw new IllegalArgumentException("All targets within a single package must have the same base directory.");
        }
        return baseDirs.get(0);
    }

    private @Nullable String resolveProcMacroArtifact(List<RustTargetModule> rustTargets) {
        // We take only the first procedural macro as Cargo does not allow
        // more than one library in a single package.
        // consulo: upstream `bazelPathsResolver.pathToDirectoryUri(it).path` of the exec root path
        return rustTargets.stream()
            .flatMap(it -> it.rustModule().procMacroArtifacts().stream())
            .map(myResolveLocation)
            .filter(Objects::nonNull)
            .map(Path::toString)
            .findFirst()
            .orElse(null);
    }

    private String resolvePackageRootUrl(List<RustTargetModule> rustTargets) {
        return rustTargets.stream()
            .map(it -> it.module().baseDirectory())
            .findFirst()
            .orElse("Missing package root path");
    }

    private RustTarget parseSingleTarget(RustTargetModule module) {
        Module genericData = module.module();
        RustModule rustData = module.rustModule();
        RustTarget buildTarget =
            new RustTarget(
                genericData.label().target().toString(),
                resolveUrl(rustData.crateRoot()),
                parseTargetKind(rustData.kind()),
                rustData.edition(),
                // We set `doctest` always to false, as in Bazel doctests
                // are separate targets (`rust_doc_test`) that don't have CrateInfo
                // and therefore are not send in RustWorkspaceResult.
                false,
                parseCrateTypes(rustData.kind()),
                new LinkedHashSet<>(rustData.crateFeatures())
            );
        return buildTarget;
    }

    // consulo: upstream RustModule.crateRoot was already a URI, the OutputLocation is resolved here to a VFS url
    private String resolveUrl(OutputLocation location) {
        Path path = myResolveLocation.apply(location);
        return VirtualFileUtil.pathToUrl(path != null ? path.toString() : OutputLocation.toExecrootPath(location));
    }

    private RustTargetKind parseTargetKind(String kind) {
        return switch (kind) {
            case "bin" -> RustTargetKind.BIN;
            case "test" -> RustTargetKind.TEST;
            case "rlib" -> RustTargetKind.LIB;
            case "proc-macro" -> RustTargetKind.LIB;
            default -> RustTargetKind.UNKNOWN;
        };
    }

    private List<RustCrateType> parseCrateTypes(String kind) {
        return switch (kind) {
            case "rlib" -> List.of(RustCrateType.RLIB);
            case "proc-macro" -> List.of(RustCrateType.PROC_MACRO);
            default -> List.of();
        };
    }

    private Map<String, Set<String>> resolvePackageFeatures(List<RustTargetModule> rustTargets) {
        List<String> allFeaturesAsStrings = new ArrayList<>();
        for (RustTargetModule it : rustTargets) {
            allFeaturesAsStrings.addAll(it.rustModule().crateFeatures());
        }
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String it : allFeaturesAsStrings) {
            result.put(it, Set.of());
        }
        return result;
    }

    private Map<String, String> resolvePackageEnv(
        String rustPackage,
        String pkgBaseDir,
        String version
    ) {
        Version splitVersion = splitVersion(version);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("CARGO_MANIFEST_DIR", pkgBaseDir + rustPackage.substring(Math.min(1, rustPackage.length())));
        result.put("CARGO", "cargo");
        result.put("CARGO_PKG_VERSION", version);
        result.put("CARGO_PKG_VERSION_MAJOR", splitVersion.major());
        result.put("CARGO_PKG_VERSION_MINOR", splitVersion.minor());
        result.put("CARGO_PKG_VERSION_PATCH", splitVersion.patch());
        result.put("CARGO_PKG_VERSION_PRE", splitVersion.preRelease() != null ? splitVersion.preRelease() : "");
        result.put("CARGO_PKG_AUTHORS", "");
        result.put("CARGO_PKG_NAME", rustPackage);
        result.put("CARGO_PKG_DESCRIPTION", "");
        result.put("CARGO_PKG_REPOSITORY", "");
        result.put("CARGO_PKG_LICENSE", "");
        result.put("CARGO_PKG_LICENSE_FILE", "");
        result.put("CARGO_CRATE_NAME", rustPackage);
        return result;
    }

    private Version splitVersion(String version) {
        // consulo: Kotlin `val (major, minor, rest) = version.split(".")` throws when there are less than three parts
        // and ignores the parts after the third one
        String[] parts = version.split("\\.", -1);
        if (parts.length < 3) {
            return new Version(version);
        }
        String major = parts[0];
        String minor = parts[1];
        String rest = parts[2];
        String[] splitRest = rest.split("-", -1);
        return new Version(major, minor, splitRest[0], splitRest.length > 1 ? splitRest[1] : null);
    }
}
