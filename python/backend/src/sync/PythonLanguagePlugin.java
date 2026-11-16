package com.intellij.bazel.python.backend.sync;

import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.python.debug.PythonDebugUtils;
import org.jetbrains.bazel.python.lang.PythonBuildTarget;
import org.jetbrains.bazel.python.lang.PythonLanguageClass;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.server.model.BazelSyncProject;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSyncConfig;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@ExtensionImpl
public class PythonLanguagePlugin implements LanguagePlugin {
    @Override
    public Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes() {
        return Set.of(PythonBuildTarget.class);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguages() {
        return Set.of(PythonLanguageClass.PYTHON);
    }

    @Override
    public List<LanguageClass> collectUsedLanguages(TargetIdeInfo target) {
        if (target.hasPythonTargetInfo()) {
            return List.of(PythonLanguageClass.PYTHON);
        }
        return List.of();
    }

    @Override
    public CompletableFuture<List<WorkspaceSyncConfig>> createSyncConfigs(Project project, ProjectView projectView) {
        PythonWorkspaceSyncConfig config = PythonWorkspaceSyncConfig.INSTANCE;
        return CompletableFuture.completedFuture(List.of(config));
    }

    @Override
    public CompletableFuture<List<BuildTargetData>> mapBuildTargetData(
        BazelServerFacade server,
        TargetIdeInfo target
    ) {
        if (!target.hasPythonTargetInfo()) {
            return CompletableFuture.completedFuture(List.of());
        }
        IntellijIdeInfo.PythonTargetInfo pythonTarget = target.getPythonTargetInfo();
        // consulo: the suspend calls of upstream run sequentially, in the same order
        return resolveGeneratedSources(pythonTarget, server).thenCompose(generatedSources ->
            server.getOutputParser().parse(target.getSrcsList()).thenCompose(sources -> {
                List<OutputLocation> allSources = new ArrayList<>(sources);
                generatedSources.getOutputLocations().forEach(allSources::add);
                return prepareGeneratedSources(server, allSources);
            }).thenCompose(generatedSourceOverrides -> {
                Path runnerScript;
                if (target.hasExecutableInfo()) {
                    runnerScript = server.getBazelPathsResolver().resolve(target.getExecutableInfo().getExecutableFile(), RepoMapping.NoLocalRepositories);
                }
                else {
                    runnerScript = null;
                }
                return parseInterpreter(pythonTarget, server).thenCompose(interpreter ->
                    server.getOutputParser().parse(getExternalSources(server, target)).thenCompose(externalSourceLocations -> {
                        List<OutputLocation> sitePackages = new ArrayList<>();
                        for (OutputLocation it : externalSourceLocations) {
                            sitePackages.add(toSitePackagesDirectory(it));
                        }
                        OutputLocationCollection externalSources = OutputLocationCollectionBuilder.ofLocations(sitePackages);
                        ArtifactLocation mainFileArtifact = MainSourceFinder.findMainFile(target, pythonTarget);
                        CompletableFuture<@Nullable OutputLocation> mainFileFuture = mainFileArtifact != null
                            ? server.getOutputParser().parse(mainFileArtifact).thenApply(it -> it)
                            : CompletableFuture.completedFuture(null);
                        return mainFileFuture.thenApply(mainFile -> List.<BuildTargetData>of(
                            new PythonBuildTarget(
                                pythonTarget.getVersion().isEmpty() ? null : pythonTarget.getVersion(),
                                interpreter,
                                StringsCollection.of(pythonTarget.getImportsList()),
                                generatedSources,
                                externalSources,
                                mainFile,
                                pythonTarget.getMainModule(),
                                runnerScript,
                                StringsCollection.of(PythonDebugUtils.extractPythonTargetArgs(target)),
                                generatedSourceOverrides
                            )
                        ));
                    })
                );
            })
        );
    }

    private CompletableFuture<Map<OutputLocation, OutputLocation>> prepareGeneratedSources(
        BazelServerFacade server,
        List<OutputLocation> sources
    ) {
        OutputLocationResolver resolver = DefaultOutputLocationResolver.createExecrootResolving(server.getBazelInfo());
        List<Pair<OutputLocation, Path>> locations = new ArrayList<>();
        for (OutputLocation location : new LinkedHashSet<>(sources)) {
            if (!OutputLocation.isGenerated(location)) {
                continue;
            }
            Path resolved = resolver.resolve(location, RepoMapping.NoLocalRepositories);
            if (resolved != null && PythonLanguageClass.PYTHON.equals(LanguageClass.fromExtension(extension(resolved)))) {
                locations.add(Pair.create(location, resolved));
            }
        }
        // consulo: coroutineScope { async { } }.awaitAll() -> the futures of all hard links, awaited together
        List<CompletableFuture<@Nullable Pair<OutputLocation, OutputLocation>>> futures = new ArrayList<>();
        for (Pair<OutputLocation, Path> entry : locations) {
            OutputLocation location = entry.getFirst();
            Path path = entry.getSecond();
            futures.add(server.getOutFileHardLinks().createOutputFileHardLink(path).thenApply(hardLink -> {
                Path effectivePath = hardLink != null ? hardLink : path.toAbsolutePath();
                if (!Objects.equals(effectivePath, server.getOutputResolver().resolve(location, RepoMapping.NoLocalRepositories))) {
                    return Pair.<OutputLocation, OutputLocation>create(location, new OutputLocation.Host(effectivePath.toAbsolutePath().toString()));
                }
                else {
                    return null;
                }
            }));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            Map<OutputLocation, OutputLocation> result = new LinkedHashMap<>();
            for (CompletableFuture<@Nullable Pair<OutputLocation, OutputLocation>> future : futures) {
                Pair<OutputLocation, OutputLocation> it = future.join();
                if (it != null) {
                    result.put(it.getFirst(), it.getSecond());
                }
            }
            return result;
        });
    }

    private List<ArtifactLocation> getExternalSources(
        BazelServerFacade server,
        TargetIdeInfo targetInfo
    ) {
        return BazelSyncProject.getSourcesList(targetInfo)
            .filter(it -> server.getBazelPathsResolver().isExternal(it, RepoMapping.NoLocalRepositories))
            .toList();
    }

    private static CompletableFuture<@Nullable OutputLocation> parseInterpreter(
        IntellijIdeInfo.PythonTargetInfo receiver,
        BazelServerFacade server
    ) {
        if (!receiver.getInterpreterPath().isEmpty()) {
            return CompletableFuture.completedFuture(server.getOutputParser().parseExecrootPath(receiver.getInterpreterPath()));
        }
        else if (receiver.hasInterpreter() && !receiver.getInterpreter().getRelativePath().isEmpty()) {
            return server.getOutputParser().parse(receiver.getInterpreter()).thenApply(it -> it);
        }
        else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static CompletableFuture<OutputLocationCollection> resolveGeneratedSources(
        IntellijIdeInfo.PythonTargetInfo receiver,
        BazelServerFacade server
    ) {
        return server.getOutputParser().parse(receiver.getGeneratedSourcesList()).thenApply(roots -> {
            List<ArtifactLocation> generatedSourcesList = receiver.getGeneratedSourcesList();
            List<Pair<Path, OutputLocation>> files = new ArrayList<>();
            for (int i = 0; i < Math.min(generatedSourcesList.size(), roots.size()); i++) {
                ArtifactLocation artifact = generatedSourcesList.get(i);
                OutputLocation root = roots.get(i);
                Path rootFile = server.getBazelPathsResolver().resolve(artifact, RepoMapping.NoLocalRepositories);
                // some code gen rules return directories. we need to figure out what files are there
                if (Files.isDirectory(rootFile)) {
                    try (Stream<Path> stream = Files.walk(rootFile)) {
                        for (Path file : stream.toList()) {
                            files.add(Pair.create(file, OutputLocation.mapPath(root, it -> PathUtil.rebase(file, rootFile, it))));
                        }
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                else {
                    files.add(Pair.create(rootFile, root));
                }
            }
            List<OutputLocation> result = new ArrayList<>();
            for (Pair<Path, OutputLocation> entry : files) {
                Path file = entry.getFirst();
                OutputLocation location = entry.getSecond();
                if (!PythonLanguageClass.PYTHON.equals(LanguageClass.fromExtension(extension(file)))) {
                    continue;
                }
                // If type annotation exists - use it instead of generated .py file
                // https://peps.python.org/pep-0484/#the-type-of-class-objects
                if (extension(file).equals("py") && Files.exists(file.resolveSibling(nameWithoutExtension(file) + ".pyi"))) {
                    result.add(OutputLocation.mapPath(location, it -> it.resolveSibling(nameWithoutExtension(it) + ".pyi")));
                }
                else {
                    result.add(location);
                }
            }
            return OutputLocationCollectionBuilder.ofLocations(result);
        });
    }

    // consulo: upstream private top-level declarations of PythonLanguagePlugin.kt

    // PyNames.SITE_PACKAGES
    private static final String SITE_PACKAGES = "site-packages";

    // the closest `site-packages` directory that contains the location, or the location itself
    private static OutputLocation toSitePackagesDirectory(OutputLocation receiver) {
        return OutputLocation.mapPath(receiver, path -> PathUtil.allAncestorsSequence(path)
            .filter(it -> it.getFileName() != null && it.getFileName().toString().equals(SITE_PACKAGES))
            .findFirst()
            .orElse(path));
    }

    // consulo: Kotlin Path.extension
    private static String extension(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? FileUtil.getExtension(fileName.toString()) : "";
    }

    // consulo: Kotlin Path.nameWithoutExtension
    private static String nameWithoutExtension(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? FileUtil.getNameWithoutExtension(fileName.toString()) : "";
    }
}
