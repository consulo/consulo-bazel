// consulo: upstream server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/languages/rust/RustLanguagePlugin.kt (removed in
// 4699fc65a), adapted to the LanguagePlugin of the aspect based sync
package org.jetbrains.bazel.rust.sync;

import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.RustCrateInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.RustToolchainInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.rust.protocol.RustDependency;
import org.jetbrains.bazel.rust.protocol.RustPackage;
import org.jetbrains.bazel.rust.protocol.RustRawDependency;
import org.jetbrains.bazel.rust.protocol.RustTarget;
import org.jetbrains.bazel.rust.protocol.RustWorkspaceResult;
import org.jetbrains.bazel.rust.sync.RustDependencyResolver.RustDependencies;
import org.jetbrains.bazel.rust.sync.RustModule.RustCrateLocation;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// consulo: upstream `class RustLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<RustModule>()`.
// A LanguagePlugin is an extension now: the paths are parsed with the OutputLocationParser of the server while mapping and
// resolved by the importer (see toRustWorkspaceResult)
@ExtensionImpl
public class RustLanguagePlugin implements LanguagePlugin {
    // consulo: upstream `private val rustPackageResolver = RustPackageResolver(bazelPathsResolver)` is created by
    // toRustWorkspaceResult, which gets the path resolution of the importer
    private final RustDependencyResolver myRustDependencyResolver = new RustDependencyResolver();

    public RustLanguagePlugin() {
    }

    private static @Nullable RustCrateInfo getRustCrateInfoOrNull(TargetIdeInfo receiver) {
        return receiver.hasRustCrateInfo() ? receiver.getRustCrateInfo() : null;
    }

    // consulo: upstream `override fun applyModuleData(moduleData: RustModule, buildTarget: BuildTarget) {}` (BSP build target
    // data) has no counterpart, the snapshot keeps the RustModule itself

    @Override
    public Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes() {
        return Set.of(RustModule.class, RustToolchain.class);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguages() {
        return Set.of(RustLanguageClass.RUST);
    }

    @Override
    public List<LanguageClass> collectUsedLanguages(IntellijIdeInfo.TargetIdeInfo target) {
        return target.hasRustCrateInfo() ? List.of(RustLanguageClass.RUST) : List.of();
    }

    @Override
    public CompletableFuture<List<BuildTargetData>> mapBuildTargetData(BazelServerFacade server, TargetIdeInfo target) {
        // consulo: upstream resolveModule(targetInfo) was called by the BSP server for every target
        List<BuildTargetData> result = new ArrayList<>();
        RustModule rustModule = resolveModule(server.getOutputParser(), target);
        if (rustModule != null) {
            result.add(rustModule);
        }
        RustToolchain rustToolchain = resolveToolchain(target);
        if (rustToolchain != null) {
            result.add(rustToolchain);
        }
        return CompletableFuture.completedFuture(result);
    }

    public @Nullable RustModule resolveModule(OutputLocationParser parser, TargetIdeInfo targetInfo) {
        RustCrateInfo rustCrateInfo = getRustCrateInfoOrNull(targetInfo);
        if (rustCrateInfo == null) {
            return null;
        }
        RustCrateLocation location = resolveTargetLocation(rustCrateInfo);
        String kind;
        if (targetInfo.getKind().equals("rust_test")) {
            kind = "test";
        }
        else {
            kind = rustCrateInfo.getKind();
        }
        OutputLocation crateRoot = resolveTargetCrateRoot(parser, rustCrateInfo, location);

        // consulo: the protobuf lists are copied, the snapshot persists the record
        return new RustModule(
            rustCrateInfo.getCrateId(),
            location,
            rustCrateInfo.getFromWorkspace(),
            rustCrateInfo.getName(),
            kind,
            rustCrateInfo.getEdition(),
            List.copyOf(rustCrateInfo.getCrateFeaturesList()),
            List.copyOf(rustCrateInfo.getDependenciesCrateIdsList()),
            crateRoot,
            rustCrateInfo.getVersion(),
            // consulo: upstream kept the exec root paths and resolved them in RustPackageResolver.resolveProcMacroArtifact
            List.copyOf(parser.parseExecrootPath(rustCrateInfo.getProcMacroArtifactsList())),
            // consulo: Consulo-only, the build script output directory (an exec root path) and the cfg options of the crate
            rustCrateInfo.getOutDir().isEmpty() ? null : parser.parseExecrootPath(rustCrateInfo.getOutDir()),
            List.copyOf(rustCrateInfo.getCfgOptionsList())
        );
    }

    private RustCrateLocation resolveTargetLocation(RustCrateInfo rustCrateInfo) {
        if (rustCrateInfo.getLocation() == IntellijIdeInfo.RustCrateLocation.WORKSPACE_DIR) {
            return RustCrateLocation.WORKSPACE_DIR;
        }
        else {
            return RustCrateLocation.EXEC_ROOT;
        }
    }

    private OutputLocation resolveTargetCrateRoot(OutputLocationParser parser, RustCrateInfo rustCrateInfo, RustCrateLocation location) {
        // consulo: upstream resolved the crate root against the workspace (WORKSPACE_DIR) or the exec root (EXEC_ROOT) to a URI.
        // The snapshot keeps an OutputLocation, resolved by the importer: the exec root path of a crate root in the workspace
        // directory parses to a workspace location, the path of an external or generated crate root to an external or output one
        return parser.parseExecrootPath(rustCrateInfo.getCrateRoot());
    }

    // consulo: Consulo-only, the toolchain the Consulo aspect read from the Rustc action of the crate. The paths stay exec root
    // relative: they are generated symlinks of the sysroot, the importer resolves them against the execution root
    private @Nullable RustToolchain resolveToolchain(TargetIdeInfo targetInfo) {
        RustCrateInfo rustCrateInfo = getRustCrateInfoOrNull(targetInfo);
        if (rustCrateInfo == null || !rustCrateInfo.hasToolchain()) {
            return null;
        }
        RustToolchainInfo toolchain = rustCrateInfo.getToolchain();
        return new RustToolchain(
            toolchain.hasRustc() ? toExecrootPath(toolchain.getRustc()) : null,
            toolchain.hasCargo() ? toExecrootPath(toolchain.getCargo()) : null,
            toolchain.getSysroot().isEmpty() ? null : toolchain.getSysroot(),
            toolchain.getTargetTriple().isEmpty() ? null : toolchain.getTargetTriple(),
            toolchain.getVersion().isEmpty() ? null : toolchain.getVersion()
        );
    }

    // consulo: the exec root path of a file of the toolchain (ArtifactLocation.from_file: root_path + relative_path)
    private static @Nullable String toExecrootPath(ArtifactLocation location) {
        String path = OutputLocation.joinNonEmpty(location.getRootPath(), location.getRelativePath());
        return path.isEmpty() ? null : path;
    }

    /**
     * consulo: upstream {@code toRustWorkspaceResult(requestTargets, allTargets)}; {@code resolveLocation} resolves the
     * {@link OutputLocation}s of the {@link RustModule}s (upstream the BazelPathsResolver of the plugin).
     */
    public RustWorkspaceResult toRustWorkspaceResult(
        List<Module> requestTargets,
        List<Module> allTargets,
        Function<OutputLocation, @Nullable Path> resolveLocation
    ) {
        RustPackageResolver rustPackageResolver = new RustPackageResolver(resolveLocation);
        Map<Label, Module> allModules = new HashMap<>();
        for (Module it : allTargets) {
            allModules.put(it.label(), it);
        }
        List<Module> modules = findAllRelatedRustTargets(requestTargets, allModules, new HashSet<>(), new ArrayList<>());
        List<RustPackage> packages = rustPackageResolver.rustPackages(modules);
        RustDependencies rustDependencies = myRustDependencyResolver.rustDependencies(packages, modules);
        Map<String, List<RustDependency>> dependencies = rustDependencies.dependencies();
        Map<String, List<RustRawDependency>> rawDependencies = rustDependencies.rawDependencies();
        List<Label> resolvedTargets = new ArrayList<>();
        for (RustPackage it : packages) {
            if (!it.origin().equals("WORKSPACE")) {
                continue;
            }
            for (RustTarget it2 : it.resolvedTargets()) {
                resolvedTargets.add(Label.parse(it.id() + ':' + it2.name()));
            }
        }

        return new RustWorkspaceResult(
            packages,
            rawDependencies,
            dependencies,
            resolvedTargets
        );
    }

    /**
     * Find all targets that are needed to be resolved.
     * It includes all targets from `targets` and all their dependencies.
     * <p>
     * Let's assume we have a following project:
     * <pre>
     *           B    A
     *           |   / \
     *           C  D   E
     *           \ /    |
     *            F     G
     * </pre>
     * We want to resolve all targets related to B. We only need B, C and F.
     * <p>
     * There is no need to revisit already visited targets - we can use `visited` set for that.
     */
    private List<Module> findAllRelatedRustTargets(
        List<Module> targets,
        Map<Label, Module> allModules,
        Set<Label> visited,
        List<Module> result
    ) {
        // consulo: Kotlin tailrec -> a loop
        List<Module> current = targets;
        while (!current.isEmpty()) {
            for (Module it : current) {
                visited.add(it.label());
            }
            result.addAll(current);

            List<Module> newTargets = new ArrayList<>();
            for (Module module : current) {
                newTargets.addAll(
                    filterVisitedRustDependencies(
                        module.directDependencies(),
                        allModules,
                        visited
                    )
                );
            }
            current = newTargets;
        }
        return result;
    }

    private List<Module> filterVisitedRustDependencies(
        List<Label> dependencies,
        Map<Label, Module> allModules,
        Set<Label> visited
    ) {
        List<Module> result = new ArrayList<>();
        for (Label dependency : dependencies) {
            Module it = allModules.get(dependency);
            if (it == null) {
                continue;
            }
            if (visited.contains(it.label())) {
                continue;
            }
            if (it.languages().contains(RustLanguageClass.RUST)) {
                result.add(it);
            }
        }
        return result;
    }
}
