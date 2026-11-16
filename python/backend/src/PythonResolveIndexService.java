package com.intellij.bazel.python.backend;

import com.jetbrains.python.PyNames;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.QualifiedName;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.python.lang.PythonBuildTarget;
import org.jetbrains.bazel.python.lang.PythonLanguageClass;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.target.BuildTargetUtils;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class PythonResolveIndexService {
    private final Project myProject;
    private final AtomicReference<ResolveIndexSnapshot> myResolveIndexSnapshotRef;

    @Inject
    public PythonResolveIndexService(Project project) {
        myProject = project;
        myResolveIndexSnapshotRef = new AtomicReference<>(newSnapshot(load(pyIndexStoragePath(project))));
    }

    public Map<QualifiedName, Path> getResolveIndex() {
        return myResolveIndexSnapshotRef.get().qualifiedNamesToPaths();
    }

    public @Nullable QualifiedName findShortestQualifiedName(Path path) {
        Map<Path, QualifiedName> shortestQualifiedNamesByPath = myResolveIndexSnapshotRef.get().shortestQualifiedNamesByPath();
        QualifiedName qualifiedName = shortestQualifiedNamesByPath.get(path);
        if (qualifiedName != null) {
            return qualifiedName;
        }
        Path parent = isPackageInitFile(path) ? path.getParent() : null;
        return parent != null ? shortestQualifiedNamesByPath.get(parent) : null;
    }

    public @Nullable QualifiedName findShortestQualifiedName(VirtualFile file) {
        Path path = toNioPathOrNull(file);
        return path != null ? findShortestQualifiedName(path) : null;
    }

    public Map<String, Path> findDirectChildren(QualifiedName qualifiedName) {
        Map<String, Path> children = myResolveIndexSnapshotRef.get().directChildrenByQualifiedName().get(qualifiedName);
        return children != null ? children : Map.of();
    }

    public GlobalSearchScope getStubScope() {
        return myResolveIndexSnapshotRef.get().getStubScope();
    }

    public CompletableFuture<Void> updatePythonResolveIndex(
        WorkspaceImporterContext context,
        WorkspaceSnapshot snapshot,
        List<BuildTarget> pythonTargets,
        Map<Path, Path> generatedSourcePaths
    ) {
        return buildIndex(context, snapshot, pythonTargets, generatedSourcePaths).thenAccept(nameToPathIndex -> {
            updateResolveIndexSnapshot(nameToPathIndex);
            store(pyIndexStoragePath(myProject), nameToPathIndex);
        });
    }

    public void updateResolveIndexSnapshot(Map<QualifiedName, Path> newNameToPathIndex) {
        myResolveIndexSnapshotRef.set(newSnapshot(newNameToPathIndex));
    }

    private ResolveIndexSnapshot newSnapshot(Map<QualifiedName, Path> resolveIndex) {
        return new ResolveIndexSnapshot(
            resolveIndex,
            buildShortestQualifiedNamesByPath(resolveIndex),
            buildDirectChildrenByQualifiedName(resolveIndex),
            () -> buildStubScope(resolveIndex.values())
        );
    }

    private GlobalSearchScope buildStubScope(Collection<Path> paths) {
        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
        Set<VirtualFile> files = new LinkedHashSet<>();
        for (Path path : paths) {
            VirtualFile it = virtualFileManager.findFileByNioPath(path);
            if (it == null) {
                continue;
            }
            if (it.isDirectory()) {
                files.addAll(findPackageInitFiles(it));
            }
            else {
                files.add(it);
            }
        }
        return files.isEmpty() ? GlobalSearchScope.EMPTY_SCOPE : GlobalSearchScope.filesWithLibrariesScope(myProject, files);
    }

    private CompletableFuture<Map<QualifiedName, Path>> buildIndex(
        WorkspaceImporterContext context,
        WorkspaceSnapshot snapshot,
        List<BuildTarget> pythonTargets,
        Map<Path, Path> generatedSourcePaths
    ) {
        Path rootDir = Path.of(org.jetbrains.bazel.config.project.getRootDir(myProject).getPath());
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        OutputLocationResolver execrootResolver = DefaultOutputLocationResolver.createExecrootResolving(context.bazelInfo());

        Function<OutputLocation, @Nullable Path> resolve = location -> context.outputResolver().resolve(location, localRepositories);

        Function<BuildTarget, List<Path>> workspaceRelativePythonSources = target ->
            target.getSources().getOutputLocations()
                .filter(it -> isPythonLanguage(it) && OutputLocation.isSource(it) && OutputLocation.isUserCode(it, snapshot.repoMapping()))
                .map(resolve)
                .filter(Objects::nonNull)
                .filter(it -> isPythonFile(it))
                .map(it -> relativeToOrNull(it, rootDir))
                .filter(Objects::nonNull)
                .toList();

        List<List<Path>> importsByTarget = new ArrayList<>();
        for (BuildTarget it : pythonTargets) {
            importsByTarget.add(PythonImportUtils.assembleExplicitImportsPaths(it));
        }
        Set<List<Path>> workspaceImportsSet = new LinkedHashSet<>();
        for (int i = 0; i < importsByTarget.size(); i++) {
            if (BuildTargetUtils.isWorkspace(pythonTargets.get(i), snapshot.repoMapping()) && !importsByTarget.get(i).isEmpty()) {
                workspaceImportsSet.add(importsByTarget.get(i));
            }
        }
        List<List<Path>> workspaceImports = new ArrayList<>(workspaceImportsSet);
        Set<Path> importRoots = new HashSet<>();
        for (List<Path> it : workspaceImports) {
            importRoots.addAll(it);
        }
        Map<Path, List<Path>> sourcesByImportRoot = new HashMap<>();
        if (!importRoots.isEmpty()) {
            Set<Path> distinctSources = new LinkedHashSet<>();
            for (BuildTarget it : pythonTargets) {
                distinctSources.addAll(workspaceRelativePythonSources.apply(it));
            }
            for (Path source : distinctSources) {
                // consulo: currentCoroutineContext().ensureActive() -> ProgressManager.checkCanceled()
                ProgressManager.checkCanceled();
                for (int length = 1; length <= source.getNameCount(); length++) {
                    Path prefix = source.subpath(0, length);
                    if (importRoots.contains(prefix)) {
                        sourcesByImportRoot.computeIfAbsent(prefix, ignored -> new ArrayList<>()).add(source);
                    }
                }
            }
        }
        Map<List<Path>, List<Path>> sharedSourcesByImports = new LinkedHashMap<>();
        for (List<Path> imports : workspaceImports) {
            List<Path> sources = new ArrayList<>();
            for (Path it : imports) {
                sources.addAll(sourcesByImportRoot.getOrDefault(it, List.of()));
            }
            sharedSourcesByImports.put(imports, sources);
        }
        Map<List<Path>, @Nullable Map<QualifiedName, Path>> sharedIndexesByImports = new LinkedHashMap<>();
        for (Map.Entry<List<Path>, List<Path>> entry : sharedSourcesByImports.entrySet()) {
            List<Path> imports = entry.getKey();
            List<Path> sources = entry.getValue();
            ProgressManager.checkCanceled();
            if (sources.isEmpty()) {
                sharedIndexesByImports.put(imports, null);
            }
            else {
                Map<Path, Path> sourcesToAbsolute = new LinkedHashMap<>();
                for (Path it : sources) {
                    sourcesToAbsolute.put(it, rootDir.resolve(it));
                }
                sharedIndexesByImports.put(
                    imports,
                    expandPathsToQualifiedNames(PythonImportUtils.assembleQualifiedNameImportPaths(imports), sourcesToAbsolute)
                );
            }
        }

        List<CompletableFuture<IndexContribution>> targetContributionFutures = new ArrayList<>();
        for (int index = 0; index < pythonTargets.size(); index++) {
            BuildTarget target = pythonTargets.get(index);
            List<Path> explicitImportsPaths = importsByTarget.get(index);
            targetContributionFutures.add(BazelCoroutineService.getInstance(myProject).startAsync(() -> {
                List<PythonImportUtils.ImportsPath> qualifiedNameImportPaths =
                    PythonImportUtils.assembleQualifiedNameImportPaths(explicitImportsPaths);
                PythonBuildTarget pythonBuildTarget = PythonBuildTarget.extractPythonBuildTarget(target);
                Stream<OutputLocation> generatedSourceFiles = Stream.concat(
                    target.getSources().getOutputLocations().filter(OutputLocation::isGenerated),
                    pythonBuildTarget != null ? pythonBuildTarget.generatedSources().getOutputLocations() : Stream.empty()
                );
                List<OutputLocation> generatedPythonSources = generatedSourceFiles.distinct().filter(it -> isPythonLanguage(it)).toList();
                Map<QualifiedName, Path> sharedIndex;
                if (generatedPythonSources.isEmpty() && BuildTargetUtils.isWorkspace(target, snapshot.repoMapping())) {
                    sharedIndex = sharedIndexesByImports.get(explicitImportsPaths);
                }
                else {
                    sharedIndex = null;
                }
                if (sharedIndex != null) {
                    return CompletableFuture.completedFuture(new IndexContribution(sharedIndex, explicitImportsPaths));
                }
                else {
                    Map<Path, Path> sourcesRelativePathToAbsolutePath = new LinkedHashMap<>();
                    if (BuildTargetUtils.isWorkspace(target, snapshot.repoMapping())) {
                        List<Path> sharedSources = sharedSourcesByImports.getOrDefault(explicitImportsPaths, List.of());
                        List<Path> sources = sharedSources.isEmpty() ? workspaceRelativePythonSources.apply(target) : sharedSources;
                        for (Path path : sources) {
                            sourcesRelativePathToAbsolutePath.put(path, rootDir.resolve(path));
                        }
                    }
                    else {
                        target.getSources().getOutputLocations()
                            .filter(it -> isPythonLanguage(it))
                            .forEach(location -> {
                                Path relativePath = OutputLocation.toRepoRelativePath(location);
                                if (relativePath == null) {
                                    return;
                                }
                                Path path = resolve.apply(location);
                                if (path == null || !isPythonFile(path)) {
                                    return;
                                }
                                sourcesRelativePathToAbsolutePath.put(relativePath, path);
                            });
                    }
                    Map<Path, Path> generatedSourcesRelativePathToAbsolutePath = new LinkedHashMap<>();
                    for (OutputLocation location : generatedPythonSources) {
                        Path relativePath = OutputLocation.toRepoRelativePath(location);
                        if (relativePath == null) {
                            continue;
                        }
                        Path path = execrootResolver.resolve(location, localRepositories);
                        if (path == null) {
                            continue;
                        }
                        Path generatedSourcePath = generatedSourcePaths.get(path);
                        generatedSourcesRelativePathToAbsolutePath.put(relativePath, generatedSourcePath != null ? generatedSourcePath : path.toAbsolutePath());
                    }
                    Map<Path, Path> allSourcesRelativePathToAbsolutePath = new LinkedHashMap<>(sourcesRelativePathToAbsolutePath);
                    allSourcesRelativePathToAbsolutePath.putAll(generatedSourcesRelativePathToAbsolutePath);
                    return CompletableFuture.completedFuture(new IndexContribution(
                        expandPathsToQualifiedNames(qualifiedNameImportPaths, allSourcesRelativePathToAbsolutePath),
                        null
                    ));
                }
            }));
        }

        // consulo: awaitAll()
        return CompletableFuture.allOf(targetContributionFutures.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<IndexContribution> targetContributions = new ArrayList<>();
            for (CompletableFuture<IndexContribution> it : targetContributionFutures) {
                targetContributions.add(it.join());
            }

            Map<List<Path>, Integer> lastSharedIndexByImports = new HashMap<>();
            for (int index = 0; index < targetContributions.size(); index++) {
                List<Path> sharedImports = targetContributions.get(index).sharedImports();
                if (sharedImports != null) {
                    lastSharedIndexByImports.put(sharedImports, index);
                }
            }
            Set<List<Path>> seenSharedImports = new HashSet<>();
            Map<QualifiedName, Path> qualifiedNamesResolverMap = new HashMap<>();
            // Keep the first write for map iteration order and the last write for target overwrite order.
            for (int index = 0; index < targetContributions.size(); index++) {
                IndexContribution contribution = targetContributions.get(index);
                List<Path> sharedImports = contribution.sharedImports();
                if (sharedImports != null && !seenSharedImports.add(sharedImports) && !Integer.valueOf(index).equals(lastSharedIndexByImports.get(sharedImports))) {
                    continue;
                }
                qualifiedNamesResolverMap.putAll(contribution.qualifiedNamesToPaths());
            }
            return qualifiedNamesResolverMap;
        });
    }

    /*
     * Expand the relative->absolute path map to include the parent paths
     * e.g. for an entry aaa/bbb/ccc.py -> /absolute/aaa/bbb/ccc.py,
     * this function will also add (aaa/bbb -> /absolute/aaa/bbb) and (aaa -> /absolute/aaa) into the new map,
     * so that intermediate directories are also tracked
     *
     * All intermediate relative paths are converted to QualifiedNames for the result map
     * */
    private Map<QualifiedName, Path> expandPathsToQualifiedNames(
        List<PythonImportUtils.ImportsPath> importsPaths,
        Map<Path, Path> filePaths
    ) {
        Map<QualifiedName, Path> newMap = new HashMap<>();
        for (Map.Entry<Path, Path> entry : filePaths.entrySet()) {
            Path rootRelativePath = entry.getKey();
            Path absolutePath = entry.getValue();
            List<QualifiedName> qualifiedNames = new ArrayList<>();
            if (!importsPaths.isEmpty()) {
                for (PythonImportUtils.ImportsPath it : importsPaths) {
                    Path relativized = it.relativizePath(rootRelativePath);
                    QualifiedName qualifiedName = relativized != null ? toQualifiedName(relativized) : null;
                    if (qualifiedName != null) {
                        qualifiedNames.add(qualifiedName);
                    }
                }
            }
            else {
                QualifiedName qualifiedName = toQualifiedName(rootRelativePath);
                if (qualifiedName != null) {
                    qualifiedNames.add(qualifiedName);
                }
            }

            for (QualifiedName qualifiedName : qualifiedNames) {
                QualifiedName qName = qualifiedName;

                // qualified name of `__init__` is the py package, so walk starts at the package directory
                Path qNamePath = isPackageInitFile(absolutePath) ? Objects.requireNonNull(absolutePath.getParent()) : absolutePath;

                while (qName.getComponentCount() > 0) {
                    if (newMap.containsKey(qName)) {
                        break;
                    }

                    newMap.put(qName, qNamePath);

                    qName = qName.removeLastComponent();
                    qNamePath = Objects.requireNonNull(qNamePath.getParent());
                }
            }
        }

        return newMap;
    }

    private Map<Path, QualifiedName> buildShortestQualifiedNamesByPath(Map<QualifiedName, Path> resolveIndex) {
        Map<Path, QualifiedName> result = new HashMap<>();
        for (Map.Entry<QualifiedName, Path> entry : resolveIndex.entrySet()) {
            QualifiedName qualifiedName = entry.getKey();
            Path path = entry.getValue();
            QualifiedName existingQualifiedName = result.get(path);
            if (existingQualifiedName == null || qualifiedName.getComponentCount() < existingQualifiedName.getComponentCount()) {
                // keep the shortest matching qualified name
                result.put(path, qualifiedName);
            }
        }
        return result;
    }

    private Map<QualifiedName, Map<String, Path>> buildDirectChildrenByQualifiedName(Map<QualifiedName, Path> resolveIndex) {
        Map<QualifiedName, Map<String, Path>> result = new HashMap<>();
        for (Map.Entry<QualifiedName, Path> entry : resolveIndex.entrySet()) {
            QualifiedName qualifiedName = entry.getKey();
            Path path = entry.getValue();
            String childName = qualifiedName.getLastComponent();
            if (childName == null) {
                continue;
            }
            result.computeIfAbsent(qualifiedName.removeLastComponent(), ignored -> new LinkedHashMap<>()).putIfAbsent(childName, path);
        }
        return result;
    }

    // consulo: upstream companion object members

    private static final Logger logger = Logger.getInstance(PythonResolveIndexService.class);

    private static void store(Path storagePath, Map<QualifiedName, Path> map) {
        if (map.isEmpty()) {
            deleteIfExists(storagePath);
            return;
        }

        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(storagePath)) {
                for (Map.Entry<QualifiedName, Path> entry : map.entrySet()) {
                    writer.append(entry.getKey().toString()).append(":").append(entry.getValue().toString()).append('\n');
                }
            }
        }
        catch (Throwable ex) {
            logger.warn("Failed to store Python resolve index to " + storagePath, ex);
            deleteIfExists(storagePath);
        }
    }

    private static Map<QualifiedName, Path> load(Path storagePath) {
        if (!Files.exists(storagePath)) {
            return Map.of();
        }

        try {
            Map<QualifiedName, Path> result = new LinkedHashMap<>();
            try (BufferedReader reader = Files.newBufferedReader(storagePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    // consulo: Kotlin destructuring of the split result fails for a line without a separator
                    if (parts.length < 2) {
                        throw new IndexOutOfBoundsException("Index 1 out of bounds for length " + parts.length);
                    }
                    result.put(QualifiedName.fromDottedString(parts[0]), Path.of(parts[1]));
                }
            }
            return result;
        }
        catch (Throwable ex) {
            logger.warn("Failed to load Python resolve index from " + storagePath, ex);
            deleteIfExists(storagePath);
            return Map.of();
        }
    }

    // consulo: upstream private top-level declarations of PythonResolveIndexService.kt

    private static final int PYINDEX_STORAGE_VERSION = 2;

    // consulo: IntelliJ Project.getProjectDataPath(name), like PersistentWorkspaceSnapshotService
    private static Path pyIndexStoragePath(Project project) {
        return ProjectUtil.getProjectCachePath(project, "projects").resolve("bazel-pyindex-v" + PYINDEX_STORAGE_VERSION + ".db");
    }

    private static @Nullable QualifiedName toQualifiedName(Path receiver) {
        String separator = FileSystems.getDefault().getSeparator();
        if (!isPythonLanguage(receiver)) {
            return null;
        }

        String path = receiver.toString();
        int dotIndex = path.lastIndexOf('.');
        String withoutExtension = dotIndex >= 0 ? path.substring(0, dotIndex) : path; // remove extension
        String relativePath = withoutExtension.endsWith(separator + PyNames.INIT)
            ? withoutExtension.substring(0, withoutExtension.length() - (separator + PyNames.INIT).length())
            : withoutExtension;

        List<String> relativePathParts = Arrays.asList(relativePath.split(java.util.regex.Pattern.quote(separator), -1));
        if (relativePathParts.stream().anyMatch(it -> it.contains("."))) {
            return null;
        }
        List<String> components = new ArrayList<>();
        for (String part : relativePathParts) {
            for (String it : part.split("\\.", -1)) {
                if (!it.isEmpty()) {
                    components.add(it);
                }
            }
        }
        return QualifiedName.fromComponents(components);
    }

    private static boolean isPackageInitFile(Path receiver) {
        return nameWithoutExtension(receiver).equals(PyNames.INIT) && isPythonLanguage(receiver);
    }

    public static List<VirtualFile> findPackageInitFiles(VirtualFile receiver) {
        List<VirtualFile> result = new ArrayList<>();
        VirtualFile initPy = receiver.findChild(PyNames.INIT_DOT_PY);
        if (initPy != null) {
            result.add(initPy);
        }
        // consulo: PyNames.INIT_DOT_PYI is missing in consulo-python
        VirtualFile initPyi = receiver.findChild(PyNames.INIT + ".pyi");
        if (initPyi != null) {
            result.add(initPyi);
        }
        return result;
    }

    private static boolean isPythonFile(Path receiver) {
        return Files.isRegularFile(receiver) && isPythonLanguage(receiver);
    }

    private static boolean isPythonLanguage(Path receiver) {
        return PythonLanguageClass.PYTHON.equals(LanguageClass.fromExtension(extension(receiver)));
    }

    // we only care about the extension, so the relative path of the location is enough
    private static boolean isPythonLanguage(OutputLocation receiver) {
        return isPythonLanguage(OutputLocation.getRelativeNioPath(receiver));
    }

    @org.jetbrains.annotations.TestOnly
    public static @Nullable String findBazelPythonShortestQualifiedName(Project receiver, Path path) {
        QualifiedName qualifiedName = receiver.getInstance(PythonResolveIndexService.class).findShortestQualifiedName(path);
        return qualifiedName != null ? qualifiedName.toString() : null;
    }

    @org.jetbrains.annotations.TestOnly
    public static boolean hasBazelPythonQualifiedName(Project receiver, String qualifiedName) {
        return receiver.getInstance(PythonResolveIndexService.class).getResolveIndex().keySet().stream()
            .anyMatch(it -> it.toString().equals(qualifiedName));
    }

    @org.jetbrains.annotations.TestOnly
    public static void updateBazelPythonResolveIndex(Project receiver, Map<QualifiedName, Path> resolveIndex) {
        receiver.getInstance(PythonResolveIndexService.class).updateResolveIndexSnapshot(resolveIndex);
    }

    // consulo: Kotlin Path / VirtualFile helpers

    private static String extension(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? FileUtil.getExtension(fileName.toString()) : "";
    }

    private static String nameWithoutExtension(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? FileUtil.getNameWithoutExtension(fileName.toString()) : "";
    }

    // consulo: Kotlin Path.relativeToOrNull
    private static @Nullable Path relativeToOrNull(Path receiver, Path base) {
        try {
            return base.relativize(receiver);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        }
        catch (Throwable e) {
            logger.warn(e);
        }
    }

    // consulo: IntelliJ VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}

// consulo: upstream private top-level classes of PythonResolveIndexService.kt

final class ResolveIndexSnapshot {
    private final Map<QualifiedName, Path> myQualifiedNamesToPaths;
    private final Map<Path, QualifiedName> myShortestQualifiedNamesByPath;
    private final Map<QualifiedName, Map<String, Path>> myDirectChildrenByQualifiedName;
    private final Supplier<GlobalSearchScope> myStubScope; // search scope for Python stubs

    ResolveIndexSnapshot(
        Map<QualifiedName, Path> qualifiedNamesToPaths,
        Map<Path, QualifiedName> shortestQualifiedNamesByPath,
        Map<QualifiedName, Map<String, Path>> directChildrenByQualifiedName,
        Supplier<GlobalSearchScope> computeStubScope
    ) {
        myQualifiedNamesToPaths = qualifiedNamesToPaths;
        myShortestQualifiedNamesByPath = shortestQualifiedNamesByPath;
        myDirectChildrenByQualifiedName = directChildrenByQualifiedName;
        myStubScope = LazyValue.notNull(computeStubScope);
    }

    Map<QualifiedName, Path> qualifiedNamesToPaths() {
        return myQualifiedNamesToPaths;
    }

    Map<Path, QualifiedName> shortestQualifiedNamesByPath() {
        return myShortestQualifiedNamesByPath;
    }

    Map<QualifiedName, Map<String, Path>> directChildrenByQualifiedName() {
        return myDirectChildrenByQualifiedName;
    }

    GlobalSearchScope getStubScope() {
        return myStubScope.get();
    }
}

record IndexContribution(
    Map<QualifiedName, Path> qualifiedNamesToPaths,
    @Nullable List<Path> sharedImports
) {
}
