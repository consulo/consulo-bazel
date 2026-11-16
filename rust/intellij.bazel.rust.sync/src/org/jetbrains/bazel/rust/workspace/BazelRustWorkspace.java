package org.jetbrains.bazel.rust.workspace;

import java.util.Arrays;
import consulo.platform.Platform;
import consulo.content.bundle.SdkTable;
import consulo.content.bundle.Sdk;
import consulo.logging.Logger;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.rust.protocol.RustDepKind;
import org.jetbrains.bazel.rust.protocol.RustDepKindInfo;
import org.jetbrains.bazel.rust.protocol.RustDependency;
import org.jetbrains.bazel.rust.protocol.RustPackage;
import org.jetbrains.bazel.rust.protocol.RustRawDependency;
import org.jetbrains.bazel.rust.protocol.RustTarget;
import org.jetbrains.bazel.rust.protocol.RustWorkspaceResult;
import org.jetbrains.bazel.rust.sync.Module;
import org.jetbrains.bazel.rust.sync.RustLanguagePlugin;
import org.jetbrains.bazel.rust.sync.RustModule;
import org.jetbrains.bazel.rust.sync.RustToolchain;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jspecify.annotations.Nullable;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.model.ExternalCargoProjectData;
import org.rust.cargo.api.model.ExternalRustToolchain;
import org.rust.cargo.api.toolchain.CargoMetadata;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.stdext.HashCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Rust crates of a {@link WorkspaceSnapshot}, as a Cargo workspace for consulo-rust's external project API
 * ({@code CargoProjectsService.attachExternalCargoProject}).
 * <p>
 * consulo: Consulo-only. Upstream's BSP server answered the {@code buildTarget/rustWorkspace} request with
 * {@code RustLanguagePlugin.toRustWorkspaceResult} and the IDE side (intellij-rust) translated the result into its Cargo model.
 * The result is computed here from the snapshot the same way and translated into consulo-rust's {@link CargoWorkspaceData}.
 */
public final class BazelRustWorkspace {
    private static final String RUST_BUNDLE_TYPE_ID = "RUST";

    private static final Logger LOG = Logger.getInstance(BazelRustWorkspace.class);

    private final WorkspaceSnapshot mySnapshot;
    private final List<Module> myRequestTargets;
    private final List<Module> myAllTargets;
    private final List<RustToolchain> myToolchains;
    private final OutputLocationResolver myOutputResolver;
    private final LocalRepositoryMapping myLocalRepositories;

    private BazelRustWorkspace(
        WorkspaceSnapshot snapshot,
        List<Module> requestTargets,
        List<Module> allTargets,
        List<RustToolchain> toolchains
    ) {
        mySnapshot = snapshot;
        myRequestTargets = requestTargets;
        myAllTargets = allTargets;
        myToolchains = toolchains;
        // the crate roots, build script outputs and proc macro dylibs are exec root paths, like upstream's BazelPathsResolver
        myOutputResolver = DefaultOutputLocationResolver.createExecrootResolving(snapshot.bazelInfo());
        myLocalRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
    }

    /**
     * The request targets are the Rust targets within the import depth (upstream: the targets of the {@code rustWorkspace}
     * request), all targets are every Rust target of the snapshot, which {@code findAllRelatedRustTargets} walks the dependencies
     * through.
     */
    public static BazelRustWorkspace of(WorkspaceSnapshot snapshot) {
        BazelPathsResolver pathsResolver = new BazelPathsResolver(snapshot.bazelInfo());

        int importDepth = WorkspaceHelpers.getCommonSyncConfig(snapshot).importDepth();
        List<BuildTarget> requestBuildTargets = WorkspaceHelpers.filterBuildTarget(
                snapshot.targetGraph().findAllTargetsAtDepth(importDepth, importDepth < 0, true)
                    .map(it -> snapshot.targets().findTargetByKey(it))
                    .filter(Objects::nonNull),
                RustModule.class
            )
            .map(it -> it.getFirst())
            .toList();

        List<Module> allTargets = new ArrayList<>();
        Map<Label, Module> modulesByLabel = new LinkedHashMap<>();
        List<Pair<BuildTarget, RustModule>> allRustTargets =
            WorkspaceHelpers.filterBuildTarget(WorkspaceHelpers.getAllTargets(snapshot), RustModule.class).toList();
        for (Pair<BuildTarget, RustModule> it : allRustTargets) {
            Module module = toModule(it.getFirst(), it.getSecond(), snapshot, pathsResolver);
            allTargets.add(module);
            modulesByLabel.put(module.label(), module);
        }

        List<Module> requestTargets = new ArrayList<>();
        Set<Label> requestLabels = new LinkedHashSet<>();
        List<RustToolchain> toolchains = new ArrayList<>();
        List<RustToolchain> procMacroToolchains = new ArrayList<>();
        for (BuildTarget target : requestBuildTargets) {
            Label label = BuildTarget.getId(target);
            Module module = modulesByLabel.get(label);
            if (module != null && requestLabels.add(label)) {
                requestTargets.add(module);
            }
            RustToolchain toolchain = RustToolchain.extractRustToolchain(target);
            RustModule rustModule = RustModule.extractRustModule(target);
            if (toolchain != null && rustModule != null) {
                // a proc macro is built for the exec platform, its toolchain is only the fallback
                (rustModule.kind().equals("proc-macro") ? procMacroToolchains : toolchains).add(toolchain);
            }
        }
        toolchains.addAll(procMacroToolchains);
        return new BazelRustWorkspace(snapshot, requestTargets, allTargets, toolchains);
    }

    private static Module toModule(BuildTarget target, RustModule rustModule, WorkspaceSnapshot snapshot, BazelPathsResolver pathsResolver) {
        Label label = BuildTarget.getId(target);
        Path baseDirectory = pathsResolver.toDirectoryPath(Label.assumeResolved(label), snapshot.repoMapping()).normalize();
        return new Module(
            label,
            target.getDependencies().stream().map(DependencyLabel::targetKey).map(it -> it.label()).distinct().toList(),
            target.getKind().languageClasses(),
            VirtualFileUtil.pathToUrl(baseDirectory.toString()),
            rustModule
        );
    }

    public boolean isEmpty() {
        return myRequestTargets.isEmpty();
    }

    /**
     * The Bazel package directories of the Rust targets in the main workspace, without the ones below another one: the content
     * roots of the Rust workspace module.
     */
    public List<Path> getWorkspaceCrateDirectories() {
        Path workspaceRoot = mySnapshot.bazelInfo().workspaceRoot();
        Set<Path> directories = new LinkedHashSet<>();
        for (Module module : myRequestTargets) {
            RustModule rustModule = module.languageData();
            if (rustModule == null || !rustModule.fromWorkspace()) {
                continue;
            }
            Path directory = Path.of(VirtualFileUtil.urlToPath(module.baseDirectory()));
            if (directory.startsWith(workspaceRoot)) {
                directories.add(directory);
            }
        }
        return PathUtil.filterPathsThatDontContainEachOther(directories);
    }

    public ExternalCargoProjectData toExternalCargoProjectData() {
        RustWorkspaceResult result = LanguagePlugin.getEpName().findExtensionOrFail(RustLanguagePlugin.class)
            .toRustWorkspaceResult(myRequestTargets, myAllTargets, this::resolve);

        Map<Label, Module> modulesByLabel = new LinkedHashMap<>();
        Map<String, List<Module>> modulesByPackage = new LinkedHashMap<>();
        for (Module module : myAllTargets) {
            modulesByLabel.put(module.label(), module);
            modulesByPackage.computeIfAbsent(Module.packagePath(module.label()), key -> new ArrayList<>()).add(module);
        }

        List<CargoWorkspaceData.Package> packages = new ArrayList<>();
        for (RustPackage rustPackage : result.packages()) {
            packages.add(toPackage(rustPackage, modulesByLabel, modulesByPackage.getOrDefault(rustPackage.id(), List.of())));
        }

        Map<String, Set<CargoWorkspaceData.Dependency>> dependencies = new LinkedHashMap<>();
        for (Map.Entry<String, List<RustDependency>> entry : result.dependencies().entrySet()) {
            Set<CargoWorkspaceData.Dependency> packageDependencies = new LinkedHashSet<>();
            for (RustDependency dependency : entry.getValue()) {
                // the name of a dependency is the name its crate is used with, upstream sent the target name of the label; without
                // a rename consulo-rust uses the crate name of the library target of the dependency, which is the crate name
                packageDependencies.add(new CargoWorkspaceData.Dependency(dependency.pkg(), null, toDepKinds(dependency.depKinds())));
            }
            dependencies.put(entry.getKey(), packageDependencies);
        }

        Map<String, List<CargoMetadata.RawDependency>> rawDependencies = new LinkedHashMap<>();
        for (Map.Entry<String, List<RustRawDependency>> entry : result.rawDependencies().entrySet()) {
            List<CargoMetadata.RawDependency> packageRawDependencies = new ArrayList<>();
            for (RustRawDependency it : entry.getValue()) {
                packageRawDependencies.add(new CargoMetadata.RawDependency(
                    it.name(),
                    it.rename(),
                    it.kind(),
                    it.target(),
                    it.optional(),
                    it.usesDefaultFeatures(),
                    List.copyOf(it.features())
                ));
            }
            rawDependencies.put(entry.getKey(), packageRawDependencies);
        }

        Path workspaceRoot = mySnapshot.bazelInfo().workspaceRoot();
        CargoWorkspaceData workspaceData = new CargoWorkspaceData(
            packages,
            dependencies,
            rawDependencies,
            VirtualFileUtil.pathToUrl(workspaceRoot.toString())
        );
        // the cfg options of the compilation target come from rustc of the toolchain (`rustc --print cfg --target ...`)
        return new ExternalCargoProjectData(findManifest(workspaceRoot), workspaceData, null, toExternalToolchain());
    }

    private CargoWorkspaceData.Package toPackage(
        RustPackage rustPackage,
        Map<Label, Module> modulesByLabel,
        List<Module> packageModules
    ) {
        List<CargoWorkspaceData.Target> targets = new ArrayList<>();
        for (RustTarget target : rustPackage.resolvedTargets()) {
            Module module = modulesByLabel.get(Label.parse(rustPackage.id() + ":" + target.name()));
            RustModule rustModule = module != null ? module.languageData() : null;
            targets.add(new CargoWorkspaceData.Target(
                target.crateRootUrl(),
                // the crate name: `crate_name` of the rule, which upstream's target name (the name of the label) is not
                rustModule != null ? rustModule.name() : target.name(),
                toTargetKind(target),
                toEdition(target.edition()),
                target.doctest(),
                // upstream sent the crate features as the required features, they are the enabled features of the package
                List.of()
            ));
        }

        Map<String, List<String>> features = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : rustPackage.features().entrySet()) {
            features.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        // the `--cfg` options of the package's crates, the `feature` ones are the enabled features
        List<String> cfgOptions = new ArrayList<>();
        String outDirUrl = null;
        for (Module module : packageModules) {
            RustModule rustModule = module.languageData();
            if (rustModule == null) {
                continue;
            }
            for (String cfgOption : rustModule.cfgOptions()) {
                if (!cfgOption.startsWith("feature=") && !cfgOptions.contains(cfgOption)) {
                    cfgOptions.add(cfgOption);
                }
            }
            OutputLocation outDir = rustModule.outDir();
            Path outDirPath = outDir != null ? resolve(outDir) : null;
            if (outDirUrl == null && outDirPath != null) {
                outDirUrl = VirtualFileUtil.pathToUrl(outDirPath.toString());
            }
        }

        Map<String, String> env = new LinkedHashMap<>();
        if (rustPackage.env() != null) {
            env.putAll(rustPackage.env());
        }
        // upstream joined the package URI and the package id, `env!("CARGO_MANIFEST_DIR")` expects the directory of the package
        env.put("CARGO_MANIFEST_DIR", VirtualFileUtil.urlToPath(rustPackage.rootUrl()));

        return new CargoWorkspaceData.Package(
            rustPackage.id(),
            rustPackage.rootUrl(),
            rustPackage.name(),
            rustPackage.version(),
            targets,
            rustPackage.source(),
            rustPackage.origin().equals("WORKSPACE") ? PackageOrigin.WORKSPACE : PackageOrigin.DEPENDENCY,
            toEdition(rustPackage.edition()),
            features,
            rustPackage.enabledFeatures(),
            cfgOptions.isEmpty() ? null : CfgOptions.parse(cfgOptions),
            env,
            outDirUrl,
            toProcMacroArtifact(rustPackage.procMacroArtifact())
        );
    }

    private static CargoWorkspace.TargetKind toTargetKind(RustTarget target) {
        return switch (target.kind()) {
            case LIB -> {
                EnumSet<CargoWorkspace.LibKind> kinds = EnumSet.noneOf(CargoWorkspace.LibKind.class);
                if (target.crateTypes() != null) {
                    for (org.jetbrains.bazel.rust.protocol.RustCrateType crateType : target.crateTypes()) {
                        kinds.add(switch (crateType) {
                            case LIB -> CargoWorkspace.LibKind.LIB;
                            case RLIB -> CargoWorkspace.LibKind.RLIB;
                            case DYLIB -> CargoWorkspace.LibKind.DYLIB;
                            case CDYLIB -> CargoWorkspace.LibKind.CDYLIB;
                            case STATICLIB -> CargoWorkspace.LibKind.STATICLIB;
                            case PROC_MACRO -> CargoWorkspace.LibKind.PROC_MACRO;
                            case BIN, UNKNOWN -> CargoWorkspace.LibKind.UNKNOWN;
                        });
                    }
                }
                if (kinds.isEmpty()) {
                    kinds.add(CargoWorkspace.LibKind.LIB);
                }
                yield new CargoWorkspace.TargetKind.Lib(kinds);
            }
            case BIN -> CargoWorkspace.TargetKind.Bin.INSTANCE;
            case TEST -> CargoWorkspace.TargetKind.Test.INSTANCE;
            case EXAPMLE -> CargoWorkspace.TargetKind.ExampleBin.INSTANCE;
            case BENCH -> CargoWorkspace.TargetKind.Bench.INSTANCE;
            case CUSTOM_BUILD -> CargoWorkspace.TargetKind.CustomBuild.INSTANCE;
            case UNKNOWN -> CargoWorkspace.TargetKind.Unknown.INSTANCE;
        };
    }

    private static CargoWorkspace.Edition toEdition(String edition) {
        for (CargoWorkspace.Edition it : CargoWorkspace.Edition.values()) {
            if (it.getPresentation().equals(edition)) {
                return it;
            }
        }
        return CargoWorkspace.Edition.DEFAULT;
    }

    private static List<CargoWorkspace.DepKindInfo> toDepKinds(@Nullable List<RustDepKindInfo> depKinds) {
        if (depKinds == null || depKinds.isEmpty()) {
            return List.of(new CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Unclassified, null));
        }
        List<CargoWorkspace.DepKindInfo> result = new ArrayList<>();
        for (RustDepKindInfo it : depKinds) {
            CargoWorkspace.DepKind kind = switch (it.kind()) {
                case RustDepKind.NORMAL -> CargoWorkspace.DepKind.Normal;
                case RustDepKind.DEV -> CargoWorkspace.DepKind.Development;
                case RustDepKind.BUILD -> CargoWorkspace.DepKind.Build;
                default -> CargoWorkspace.DepKind.Unclassified;
            };
            result.add(new CargoWorkspace.DepKindInfo(kind, it.target()));
        }
        return result;
    }

    // the dylib in the output tree, consulo-rust copies the artifact of a Cargo build to a temporary directory, Bazel replaces
    // its outputs instead of writing them in place
    private static CargoWorkspaceData.@Nullable ProcMacroArtifact toProcMacroArtifact(@Nullable String path) {
        if (path == null) {
            return null;
        }
        Path artifact = Path.of(path);
        if (!Files.isRegularFile(artifact)) {
            // the proc macro dylibs are outputs of the build output group: "Build and resync" builds them
            return null;
        }
        try {
            return new CargoWorkspaceData.ProcMacroArtifact(artifact, HashCode.ofFile(artifact));
        }
        catch (IOException e) {
            LOG.warn("Cannot read the proc macro artifact " + artifact, e);
            return null;
        }
    }

    private @Nullable ExternalRustToolchain toExternalToolchain() {
        Path execRoot = mySnapshot.bazelInfo().execRoot();
        // consulo: the aspect does not report the standard library sources (rules_rust ships them only with its
        //   rust_analyzer_toolchain, which no crate resolves), so they come from the sysroot when it has them, else from a Rust
        //   bundle of the IDE; Bazel's .rust-workspace module has no Rust extension, so consulo-rust can't find the bundle itself
        @Nullable Path bundleHome = findRustBundleHome();
        @Nullable String targetTriple = null;
        for (RustToolchain toolchain : myToolchains) {
            if (targetTriple == null) {
                targetTriple = toolchain.targetTriple();
            }
            String rustc = toolchain.rustc();
            if (rustc == null) {
                continue;
            }
            Path rustcPath = execRoot.resolve(rustc);
            if (!Files.isRegularFile(rustcPath)) {
                // the sysroot is an output of the build output group: "Build and resync" generates it
                LOG.info("rustc of the Bazel toolchain is not built yet: " + rustcPath);
                continue;
            }
            String sysroot = toolchain.sysroot();
            Path sysrootPath = sysroot != null ? execRoot.resolve(sysroot) : null;
            if (sysrootPath != null && !Files.isDirectory(sysrootPath)) {
                sysrootPath = null;
            }
            return new ExternalRustToolchain(rustcPath, sysrootPath, findStdlibSources(sysrootPath, bundleHome), toolchain.targetTriple());
        }
        // consulo: the Bazel toolchain is not built yet, use the Rust bundle's rustc until "Build and resync"
        if (bundleHome != null) {
            Path rustc = bundleHome.resolve("bin").resolve(Platform.current().os().isWindows() ? "rustc.exe" : "rustc");
            if (Files.isRegularFile(rustc)) {
                return new ExternalRustToolchain(rustc, bundleHome, findStdlibSources(null, bundleHome), targetTriple);
            }
        }
        return null;
    }

    private static @Nullable Path findStdlibSources(@Nullable Path sysroot, @Nullable Path bundleHome) {
        for (Path root : Arrays.asList(sysroot, bundleHome)) {
            if (root == null) {
                continue;
            }
            Path sources = root.resolve("lib").resolve("rustlib").resolve("src").resolve("rust");
            if (Files.isDirectory(sources)) {
                return sources;
            }
        }
        return null;
    }

    // the home of the first Rust bundle (consulo-rust's RustBundleType, id "RUST") of the IDE
    private static @Nullable Path findRustBundleHome() {
        for (Sdk sdk : SdkTable.getInstance().getAllSdks()) {
            if (RUST_BUNDLE_TYPE_ID.equals(sdk.getSdkType().getId())) {
                String home = sdk.getHomePath();
                if (home != null && Files.isDirectory(Path.of(home))) {
                    return Path.of(home);
                }
            }
        }
        return null;
    }

    private @Nullable Path resolve(OutputLocation location) {
        return myOutputResolver.resolve(location, myLocalRepositories);
    }

    // the identity of the project for consulo-rust: the Bazel build file of the workspace root, never a Cargo.toml
    private static Path findManifest(Path workspaceRoot) {
        for (String name : List.of("MODULE.bazel", "WORKSPACE.bazel", "WORKSPACE")) {
            Path manifest = workspaceRoot.resolve(name);
            if (Files.isRegularFile(manifest)) {
                return manifest;
            }
        }
        return workspaceRoot.resolve("MODULE.bazel");
    }
}
