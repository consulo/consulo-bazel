package com.intellij.bazel.python.backend;

import com.intellij.bazel.python.backend.sync.PythonWorkspaceSyncConfig;
import com.jetbrains.python.impl.sdk.PythonSdkType;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.bazel.python.localize.BazelPythonBackendLocalize;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.content.library.Library;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.util.ModuleRootModificationUtil;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.python.module.extension.BasePyMutableModuleExtension;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.python.lang.PythonBuildTarget;
import org.jetbrains.bazel.python.lang.PythonLanguageClass;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.GlobalNamingContextBuilder;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameProducer;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameSpace;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.CommonWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetMerger;
import org.jetbrains.bazel.target.BuildTargetUtils;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource;
import org.jetbrains.bazel.workspacemodel.entities.extensions;
import org.jetbrains.bazel.workspacemodel.entities.extensions.BazelModuleExtensionEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetLabelList;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetSourceRootTypeId;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.utils.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
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
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

public class BazelPythonWorkspaceImporter implements BazelWorkspaceImporter {
    // consulo: upstream private top-level constants of BazelPythonWorkspaceImporter.kt.
    // SourceRootTypeId("python-source") / ("python-resource") -> the id of the Consulo ContentFolderTypeProvider
    // (ContentFolderTypeProvider.byId), which is what WorkspaceModelTargetSourceRootTypeId stores
    private static final String PYTHON_SOURCE_ROOT_TYPE = "PRODUCTION";
    private static final String PYTHON_RESOURCE_ROOT_TYPE = "PRODUCTION_RESOURCE";
    // consulo: ModuleTypeId(PYTHON_MODULE_ID) -> the Python module extension (BasePyMutableModuleExtension) is enabled on the module
    private static final NameProducer NAME_PRODUCER = new NameProducer("python");

    static final Logger logger = Logger.getInstance(BazelPythonWorkspaceImporter.class);

    // consulo: the upstream nested `Factory` is the top-level BazelPythonWorkspaceImporterFactory (an @ExtensionImpl cannot be nested)

    private final WorkspaceImporterContext myContext;

    private @Nullable CommonWorkspaceSyncConfig myCommonSyncConfig;
    private @Nullable PythonWorkspaceSyncConfig myPythonSyncConfig;
    private Map<WorkspaceTargetKey, BuildTarget> myAllPythonTargets = Map.of();
    private Map<Path, Path> myGeneratedSourcePaths = Map.of();
    private Map<Path, Path> myGeneratedSourceRoots = Map.of();

    private @Nullable OutputLocation myDefaultInterpreter = null;
    private @Nullable String myDefaultVersion = null;
    private Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> myExternalSourceDependenciesByTarget = Map.of();

    public BazelPythonWorkspaceImporter(WorkspaceImporterContext context) {
        myContext = context;
    }

    public WorkspaceImporterContext getContext() {
        return myContext;
    }

    @Override
    public LocalizeValue getImporterName() {
        return BazelPythonBackendLocalize.pythonWorkspaceImporter();
    }

    @Override
    public CompletableFuture<WorkspaceImporterResult> doImport(WorkspaceImporterPhase phase, WorkspaceSnapshot snapshot, TaskId taskId) {
        // consulo: an exception thrown by a phase fails the returned future
        try {
            if (phase instanceof WorkspaceImporterPhase.Initialize initialize) {
                return onInitialize(snapshot, initialize.naming());
            }
            else if (phase instanceof WorkspaceImporterPhase.WorkspaceApply apply) {
                return CompletableFuture.completedFuture(onWorkspaceApply(snapshot, apply.builder(), apply.entitySource(), apply.naming()));
            }
            else if (phase instanceof WorkspaceImporterPhase.PostProcessing) {
                return onPostProcessing(snapshot, taskId);
            }
            else {
                return CompletableFuture.completedFuture(WorkspaceImporterResult.Success);
            }
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<WorkspaceImporterResult> onInitialize(
        WorkspaceSnapshot snapshot,
        GlobalNamingContextBuilder naming
    ) {
        myCommonSyncConfig = snapshot.syncConfigs().stream()
            .filter(CommonWorkspaceSyncConfig.class::isInstance)
            .map(CommonWorkspaceSyncConfig.class::cast)
            .findFirst()
            .orElseThrow(IllegalStateException::new);
        myPythonSyncConfig = snapshot.syncConfigs().stream()
            .filter(PythonWorkspaceSyncConfig.class::isInstance)
            .map(PythonWorkspaceSyncConfig.class::cast)
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        int importDepth = WorkspaceHelpers.getCommonSyncConfig(snapshot).importDepth();
        List<BuildTarget> targets = snapshot.targetGraph().findAllTargetsAtDepth(importDepth, importDepth < 0, true)
            .map(it -> snapshot.targets().findTargetByKey(it))
            .filter(Objects::nonNull)
            .filter(it -> WorkspaceHelpers.hasBuildData(it, PythonBuildTarget.class))
            .toList();
        Map<WorkspaceTargetKey, BuildTarget> allPythonTargets = new LinkedHashMap<>();
        for (BuildTarget it : new WorkspaceTargetMerger(PythonMergeFunctions.pythonTargetMergeFunctions).mergeByTargetKey(targets)) {
            allPythonTargets.put(it.getKey(), it);
        }
        myAllPythonTargets = allPythonTargets;

        return collectGeneratedSourcePaths(snapshot).thenApply(ignored -> {
            PythonBuildTarget defaultPythonTarget = WorkspaceHelpers.filterBuildTarget(WorkspaceHelpers.getAllTargets(snapshot), PythonBuildTarget.class)
                .filter(it -> it.getSecond().interpreter() != null)
                .map(it -> it.getSecond())
                .findFirst()
                .orElse(null);
            myDefaultInterpreter = defaultPythonTarget != null ? defaultPythonTarget.interpreter() : null;
            myDefaultVersion = defaultPythonTarget != null ? defaultPythonTarget.version() : null;

            Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> externalSourceDependenciesByTarget = new LinkedHashMap<>();
            WorkspaceHelpers.filterBuildTarget(WorkspaceHelpers.getAllTargets(snapshot), PythonBuildTarget.class)
                .map(it -> it.getFirst().getKey())
                .filter(it -> myAllPythonTargets.containsKey(it))
                .forEach(workspaceTargetKey -> externalSourceDependenciesByTarget.put(
                    workspaceTargetKey,
                    WorkspaceHelpers.filterBuildTarget(
                            snapshot.targetGraph().findAllTransitiveSuccessorsWithoutRootTargets(workspaceTargetKey)
                                .map(it -> snapshot.targets().findTargetByKey(it))
                                .filter(Objects::nonNull),
                            PythonBuildTarget.class
                        )
                        .filter(it -> nonLocalExternalSources(it.getSecond(), snapshot.repoMapping()).findAny().isPresent())
                        .map(it -> it.getFirst().getKey())
                        .distinct()
                        .toList()
                ));
            myExternalSourceDependenciesByTarget = externalSourceDependenciesByTarget;

            for (WorkspaceTargetKey key : myAllPythonTargets.keySet()) {
                naming.declare(NameSpace.MODULE, NAME_PRODUCER, key);
            }
            // one library per external source dependency target, see `onWorkspaceApply`
            for (WorkspaceTargetKey key : externalSourceDependencyKeys()) {
                naming.declare(NameSpace.LIBRARY, NAME_PRODUCER, key);
            }

            return WorkspaceImporterResult.Success;
        });
    }

    private CompletableFuture<Void> collectGeneratedSourcePaths(WorkspaceSnapshot snapshot) {
        Map<OutputLocation, OutputLocation> overrides = new LinkedHashMap<>();
        for (BuildTarget target : myAllPythonTargets.values()) {
            PythonBuildTarget pythonTarget = WorkspaceHelpers.findBuildData(target, PythonBuildTarget.class);
            if (pythonTarget != null) {
                overrides.putAll(pythonTarget.generatedSourceOverrides());
            }
        }
        Set<OutputLocation> locationsWithoutOverrides = new LinkedHashSet<>();
        for (BuildTarget target : myAllPythonTargets.values()) {
            PythonBuildTarget pythonTarget = WorkspaceHelpers.findBuildData(target, PythonBuildTarget.class);
            Map<OutputLocation, OutputLocation> targetOverrides = pythonTarget != null ? pythonTarget.generatedSourceOverrides() : Map.of();
            generatedSources(target).filter(it -> !targetOverrides.containsKey(it)).forEach(locationsWithoutOverrides::add);
        }
        OutputLocation.OutputLocationResolver execrootResolver = DefaultOutputLocationResolver.createExecrootResolving(myContext.bazelInfo());
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        Map<Path, Path> roots = new LinkedHashMap<>();
        List<OutputLocation> locations = myAllPythonTargets.values().stream()
            .flatMap(it -> generatedSources(it))
            .filter(OutputLocation::isGenerated)
            .distinct()
            .toList();

        // consulo: findPreparedOutputFile is asynchronous; the lookups run together, the results are applied in the upstream order
        List<@Nullable Path> originals = new ArrayList<>();
        List<CompletableFuture<@Nullable Path>> preparedFutures = new ArrayList<>();
        for (OutputLocation location : locations) {
            Path original = execrootResolver.resolve(location, localRepositories);
            if (original == null || !PythonLanguageClass.PYTHON.equals(LanguageClass.fromExtension(extension(original)))) {
                originals.add(null);
                preparedFutures.add(CompletableFuture.completedFuture(null));
                continue;
            }
            originals.add(original);
            preparedFutures.add(myContext.outFileHardLinks().findPreparedOutputFile(original));
        }
        return CompletableFuture.allOf(preparedFutures.toArray(CompletableFuture[]::new)).thenAccept(ignored -> {
            Map<Path, Path> generatedSourcePaths = new LinkedHashMap<>();
            for (int i = 0; i < locations.size(); i++) {
                OutputLocation location = locations.get(i);
                Path original = originals.get(i);
                if (original == null) {
                    continue;
                }
                Path cached = myContext.outputResolver().resolve(location, localRepositories);
                OutputLocation override = overrides.get(location);
                Path fallback = override != null ? myContext.outputResolver().resolve(override, localRepositories) : null;
                Path effective = preparedFutures.get(i).join();
                if (effective == null && cached != null && locationsWithoutOverrides.contains(location) && canReplaceFallback(cached, fallback)) {
                    effective = cached;
                }
                if (effective == null) {
                    effective = fallback;
                }
                if (effective == null) {
                    effective = cached;
                }
                if (effective == null) {
                    continue;
                }
                if (effective.equals(cached)) {
                    collectGeneratedSourceRoots(original, effective, roots);
                }
                generatedSourcePaths.put(original, effective);
            }
            myGeneratedSourcePaths = generatedSourcePaths;
            myGeneratedSourceRoots = roots;
        });
    }

    private boolean canReplaceFallback(Path cached, @Nullable Path fallback) {
        if (fallback == null) {
            return true;
        }
        if (!Files.isRegularFile(cached)) {
            return false;
        }
        if (!Files.exists(fallback)) {
            return true;
        }
        try {
            return Files.isSameFile(cached, fallback);
        }
        catch (IOException e) {
            return false;
        }
    }

    private void collectGeneratedSourceRoots(Path original, Path effective, Map<Path, Path> roots) {
        Path bazelBin = myContext.bazelInfo().bazelBin();
        if (original.equals(effective) || !original.startsWith(bazelBin) || !effective.endsWith(bazelBin.relativize(original))) {
            return;
        }
        Path effectiveRoot = effective.getParent();
        for (Path root = original.getParent(); root != null; root = root.getParent()) {
            if (!root.startsWith(bazelBin)) {
                break;
            }
            Path mappedRoot = effectiveRoot;
            if (mappedRoot == null) {
                break;
            }
            if (roots.putIfAbsent(root, mappedRoot) != null) {
                break;
            }
            effectiveRoot = mappedRoot.getParent();
        }
    }

    private Stream<OutputLocation> generatedSources(BuildTarget target) {
        PythonBuildTarget pythonTarget = WorkspaceHelpers.findBuildData(target, PythonBuildTarget.class);
        return Stream.concat(
            target.getSources().getOutputLocations().filter(OutputLocation::isGenerated),
            pythonTarget != null ? pythonTarget.generatedSources().getOutputLocations() : Stream.empty()
        );
    }

    private List<WorkspaceTargetKey> externalSourceDependencyKeys() {
        Set<WorkspaceTargetKey> result = new LinkedHashSet<>();
        for (WorkspaceTargetKey it : myAllPythonTargets.keySet()) {
            result.addAll(myExternalSourceDependenciesByTarget.getOrDefault(it, List.of()));
        }
        return new ArrayList<>(result);
    }

    private WorkspaceImporterResult onWorkspaceApply(
        WorkspaceSnapshot snapshot,
        BazelModelBuilder builder,
        BazelEntitySource entitySource,
        GlobalNamingContext naming
    ) {
        Map<WorkspaceTargetKey, BuildTarget> allTargetsByKey = new LinkedHashMap<>();
        WorkspaceHelpers.getAllTargets(snapshot).forEach(it -> allTargetsByKey.put(it.getKey(), it));
        Map<WorkspaceTargetKey, Library> sourceDependencyLibraries = new LinkedHashMap<>();
        for (WorkspaceTargetKey dependencyKey : externalSourceDependencyKeys()) {
            // calculate one external source path list per iteration to avoid keeping everything in memory at once
            BuildTarget dependencyTarget = allTargetsByKey.get(dependencyKey);
            if (dependencyTarget == null) {
                continue;
            }
            List<Path> externalSources = getExternalSourcePaths(myContext, snapshot, dependencyTarget);
            if (externalSources.isEmpty()) {
                continue;
            }
            Library library = addSourceDependencyLibrary(builder, dependencyKey, naming, externalSources, entitySource);
            if (library != null) {
                sourceDependencyLibraries.put(dependencyKey, library);
            }
        }

        // consulo: the classic modifiable models read the module table (addModuleOrderEntry), which needs read access on this pool
        //   thread; upstream MutableEntityStorage needs no lock
        ReadAction.run(() -> {
            for (Map.Entry<WorkspaceTargetKey, BuildTarget> entry : myAllPythonTargets.entrySet()) {
                WorkspaceTargetKey targetKey = entry.getKey();
                BuildTarget target = entry.getValue();
                List<Library> libraries = new ArrayList<>();
                for (WorkspaceTargetKey it : myExternalSourceDependenciesByTarget.getOrDefault(targetKey, List.of())) {
                    Library library = sourceDependencyLibraries.get(it);
                    if (library != null) {
                        libraries.add(library);
                    }
                }
                addModuleEntityFromTarget(
                    snapshot,
                    builder,
                    target,
                    naming.findOrFallback(NameSpace.MODULE, NAME_PRODUCER, targetKey),
                    entitySource,
                    naming,
                    libraries
                );
            }
        });
        return WorkspaceImporterResult.Success;
    }

    private List<Path> getExternalSourcePaths(WorkspaceImporterContext context, WorkspaceSnapshot snapshot, BuildTarget target) {
        PythonBuildTarget pythonTarget = WorkspaceHelpers.findBuildData(target, PythonBuildTarget.class);
        if (pythonTarget == null) {
            return List.of();
        }
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        return nonLocalExternalSources(pythonTarget, snapshot.repoMapping())
            .map(it -> context.outputResolver().resolve(it, localRepositories))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private static Stream<OutputLocation> nonLocalExternalSources(PythonBuildTarget receiver, RepoMapping repoMapping) {
        return receiver.externalSources().getOutputLocations().filter(it -> !OutputLocation.isUserCode(it, repoMapping));
    }

    private CompletableFuture<WorkspaceImporterResult> onPostProcessing(WorkspaceSnapshot snapshot, TaskId taskId) {
        /*
         * Because of PY-86494, the interpreter update fails to add SDK paths unless there's at least one module in the project.
         * Hence, we're forced to do it in post-processing, after WSM has been applied already.
         */
        return calculateAndAddSdksWithProgress(snapshot, taskId).thenCompose(ignored -> {
            List<BuildTarget> pyTargets = myAllPythonTargets.values().stream()
                .filter(it -> WorkspaceHelpers.hasBuildData(it, PythonBuildTarget.class))
                .toList();

            return myContext.project().getInstance(PythonResolveIndexService.class)
                .updatePythonResolveIndex(myContext, snapshot, pyTargets, myGeneratedSourcePaths);
        }).thenApply(ignored -> WorkspaceImporterResult.Success);
    }

    private CompletableFuture<Void> calculateAndAddSdksWithProgress(WorkspaceSnapshot snapshot, TaskId taskId) {
        // consulo: SequentialProgressReporter.indeterminateStep(text) -> the progress indicator of the context
        myContext.progressReporter().setIndeterminate(true);
        myContext.progressReporter().setText(BazelPythonBackendLocalize.progressBarCalculatePythonSdkInfos());
        return util.withSubtask(
            myContext.taskConsole(),
            taskId.subTask("calculate-and-add-all-python-sdk-infos"),
            BazelPythonBackendLocalize.consoleTaskModelCalculatePythonSdks(),
            subtaskId -> {
                // The module SDKs are written under the configuration lock, as every other write of an interpreter.
                // TODO consulo: withSdkConfigurationLock(project) { } is missing in consulo-python, the interpreters are set without it
                return setInterpreters(snapshot);
            }
        );
    }

    /**
     * Sets an interpreter on the module of each Python target. The importer builds one interpreter for each Python binary,
     * owned by the Bazel project root, and sets it on every module that uses that binary.
     */
    private CompletableFuture<Void> setInterpreters(WorkspaceSnapshot snapshot) {
        Project project = myContext.project();
        Map<WorkspaceTargetKey, Path> binaryByTarget = new LinkedHashMap<>();
        WorkspaceHelpers.filterBuildTarget(WorkspaceHelpers.getAllTargets(snapshot), PythonBuildTarget.class).forEach(it -> {
            Path binary = interpreterPath(snapshot, it.getSecond());
            if (binary != null) {
                binaryByTarget.put(it.getFirst().getKey(), binary);
            }
        });
        // consulo: the modules of the classic project model with their BazelModuleExtensionEntity data instead of the module
        // entities of the workspace model snapshot; reading the module list needs read access on this pool thread
        Map<Path, List<Module>> modulesByBinary = new LinkedHashMap<>();
        for (Module module : ReadAction.compute(() -> ModuleManager.getInstance(project).getModules())) {
            BazelModuleExtensionEntity bazelModuleExtension = extensions.getBazelModuleExtension(module);
            Path binary = bazelModuleExtension != null ? binaryByTarget.get(bazelModuleExtension.getTargetKey()) : null;
            if (binary == null) {
                continue;
            }
            modulesByBinary.computeIfAbsent(binary, it -> new ArrayList<>()).add(module);
        }
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
        for (Map.Entry<Path, List<Module>> entry : modulesByBinary.entrySet()) {
            Path binary = entry.getKey();
            List<Module> modules = entry.getValue();
            result = result.thenCompose(ignored -> createInterpreter(binary, project)).handle((interpreter, error) -> {
                if (error != null) {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                    logger.warn("Failed to create an interpreter for " + binary + ": " + cause);
                    return CompletableFuture.<Void>completedFuture(null);
                }
                return project.getUIAccess().giveAsync(() -> {
                    for (Module module : modules) {
                        // consulo: PyProject.setPythonInterpreter(interpreter) -> the SDK of the Python module extension (the root
                        // model is rebuilt by every sync, so the SDK is set after the commit)
                        ModuleRootModificationUtil.updateModel(module, model -> {
                            BasePyMutableModuleExtension pythonExtension = model.getExtensionWithoutCheck(BasePyMutableModuleExtension.class);
                            if (pythonExtension == null) {
                                logger.warn("Failed to set the interpreter " + binary + " on " + module.getName() + ": no Python module extension");
                                return;
                            }
                            pythonExtension.setEnabled(true);
                            pythonExtension.getInheritableSdk().set(null, interpreter);
                            if (model.findModuleExtensionSdkEntry(pythonExtension) == null) {
                                model.addModuleExtensionSdkEntry(pythonExtension);
                            }
                        });
                    }
                }).<Void>thenApply(it -> null);
            }).thenCompose(it -> it);
        }
        return result;
    }

    /**
     * The Python binary of {@code pythonTarget}, or of the default target when it has none.
     */
    private @Nullable Path interpreterPath(WorkspaceSnapshot snapshot, PythonBuildTarget pythonTarget) {
        OutputLocation interpreterLocation = pythonTarget.interpreter() != null ? pythonTarget.interpreter() : myDefaultInterpreter;
        if (interpreterLocation == null) {
            return null;
        }
        return myContext.outputResolver().resolve(interpreterLocation, RepoMapping.getLocalRepositories(snapshot.repoMapping()));
    }

    private @Nullable Library addSourceDependencyLibrary(
        BazelModelBuilder builder,
        WorkspaceTargetKey dependencyTarget,
        GlobalNamingContext naming,
        List<Path> sourceDependencies,
        BazelEntitySource entitySource
    ) {
        // consulo: LibraryRoot(url, LibraryRootTypeId.SOURCES) -> a VFS url with SourcesOrderRootType
        List<String> roots = sourceDependencies.stream()
            .distinct()
            .map(it -> VirtualFileUtil.pathToUrl(it.toString()))
            .toList();
        if (roots.isEmpty()) {
            return null;
        }

        String name = naming.findOrFallback(NameSpace.LIBRARY, NAME_PRODUCER, dependencyTarget);
        // consulo: every library is a project library (upstream LibraryTableId.ProjectLibraryTableId). Upstream returns a library
        // already added by this sync (`builder.resolve(LibraryId)`); the names of this importer are unique (one per declared key),
        // so the library is created (or the one of the previous sync is reused and its roots are replaced)
        Library library = builder.getOrCreateLibrary(name);
        Library.ModifiableModel libraryModel = builder.getLibraryModifiableModel(library);
        for (String it : roots) {
            libraryModel.addRoot(it, SourcesOrderRootType.ID);
        }
        return library;
    }

    private Module addModuleEntityFromTarget(
        WorkspaceSnapshot snapshot,
        BazelModelBuilder builder,
        BuildTarget target,
        String moduleName,
        BazelEntitySource entitySource,
        GlobalNamingContext naming,
        List<Library> sourceDependencyLibraries
    ) {
        // consulo: ModuleEntity(name, dependencies, entitySource) -> a Bazel-owned module of the builder
        Module module = builder.getOrCreateModule(moduleName, null);
        ModifiableRootModel model = builder.getRootModel(module);

        // consulo: this.type = PYTHON_MODULE_TYPE -> the Python module extension
        BasePyMutableModuleExtension pythonExtension = model.getExtensionWithoutCheck(BasePyMutableModuleExtension.class);
        if (pythonExtension != null) {
            pythonExtension.setEnabled(true);
        }

        for (ContentRootData it : getContentRootEntities(snapshot, target)) {
            addContentRoot(model, it);
        }

        for (DependencyLabel it : target.getDependencies()) {
            String dependencyName = naming.findOrFallback(NameSpace.MODULE, NAME_PRODUCER, it.targetKey());
            // consulo: modules are referenced by name like the upstream ModuleId, a module written later resolves after the commit
            Module dependencyModule = builder.findModule(dependencyName);
            ModuleOrderEntry entry = dependencyModule != null
                ? model.addModuleOrderEntry(dependencyModule)
                : model.addInvalidModuleEntry(dependencyName);
            entry.setExported(true);
            entry.setScope(DependencyScope.COMPILE);  // Python does not have the runtime/compile scope separation
            entry.setProductionOnTestDependency(true);
        }

        // consulo: ModuleSourceDependency is implicit (the first order entry of a Consulo root model)

        for (Library it : sourceDependencyLibraries) {
            LibraryOrderEntry entry = model.addLibraryEntry(it);
            entry.setExported(true);
            entry.setScope(DependencyScope.COMPILE);
        }

        // The module gets its interpreter in post-processing, through PyProject.setPythonInterpreter. The importer does not
        // name an SDK here.

        builder.putModuleData(
            module,
            BazelModuleExtensionEntity.class,
            new BazelModuleExtensionEntity(
                WorkspaceModelTargetKey.of(target.getKey()),
                new WorkspaceModelTargetSourceRootTypeId(PYTHON_SOURCE_ROOT_TYPE),
                new WorkspaceModelTargetLabelList(
                    StrictDependencyCheckedType.OFF,
                    List.of()
                )
            )
        );
        return module;
    }

    private List<ContentRootData> getContentRootEntities(
        WorkspaceSnapshot snapshot,
        BuildTarget target
    ) {
        List<ContentRootData> sourceContentRootEntities = getSourceContentRootEntities(snapshot, target);
        List<ContentRootData> resourceContentRootEntities = getResourceContentRootEntities(snapshot, target);

        List<ContentRootData> result = new ArrayList<>(sourceContentRootEntities);
        result.addAll(resourceContentRootEntities);
        return result;
    }

    private List<ContentRootData> getSourceContentRootEntities(
        WorkspaceSnapshot snapshot,
        BuildTarget target
    ) {
        List<ContentRootData> result = new ArrayList<>();
        for (Path it : computeSourceRootPaths(snapshot, target)) {
            result.add(toContentRoot(it, PYTHON_SOURCE_ROOT_TYPE));
        }
        return result;
    }

    private List<ContentRootData> getResourceContentRootEntities(
        WorkspaceSnapshot snapshot,
        BuildTarget target
    ) {
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        return target.getResources().getOutputLocations()
            .map(it -> myContext.outputResolver().resolve(it, localRepositories))
            .filter(Objects::nonNull)
            .map(resource -> toContentRoot(resource, PYTHON_RESOURCE_ROOT_TYPE))
            .toList();
    }

    /**
     * Returns the directories against which the {@code imports} of {@code target} resolve: the root of its repository and the
     * directory of the outputs of that repository under {@code bazel-bin}. A repository that is neither the main nor a local
     * repository gets no directories, so its {@code imports} make no source roots.
     */
    private List<Path> importBasePaths(WorkspaceSnapshot snapshot, BuildTarget target) {
        ResolvedLabel label = BuildTarget.getId(target) instanceof ResolvedLabel resolvedLabel ? resolvedLabel : null;
        if (label == null || label.isMainWorkspace()) {
            VirtualFile projectRootDir = BazelProjectInfoService.getBazelProjectInfo(myContext.project()).getProjectRootDir();
            Set<Path> result = new LinkedHashSet<>();
            if (projectRootDir != null) {
                result.add(projectRootDir.toNioPath());
            }
            result.add(myContext.bazelInfo().bazelBin());
            return new ArrayList<>(result);
        }
        if (!BuildTargetUtils.isWorkspace(target, snapshot.repoMapping())) {
            return List.of();
        }
        BazelPathsResolver pathsResolver = new BazelPathsResolver(myContext.bazelInfo());
        Set<Path> result = new LinkedHashSet<>();
        result.add(pathsResolver.toRepositoryPath(label.repo(), snapshot.repoMapping()));
        result.add(pathsResolver.toBinRepositoryPath(label.repo(), snapshot.repoMapping()));
        return new ArrayList<>(result);
    }

    private Set<Path> computeSourceRootPaths(WorkspaceSnapshot snapshot, BuildTarget target) {
        // imports for generated files should be resolved against bazel-bin
        List<Path> basePaths = importBasePaths(snapshot, target);
        Set<Path> importRoots = new LinkedHashSet<>();
        for (Path importPath : PythonImportUtils.assembleExplicitImportsPaths(target)) {
            for (Path base : basePaths) {
                Path resolved = base.resolve(importPath).normalize();
                if (!resolved.startsWith(base)) {
                    continue;
                }
                Path generatedSourceRoot = myGeneratedSourceRoots.get(resolved);
                Path root = generatedSourceRoot != null ? generatedSourceRoot : resolved;
                if (Files.isDirectory(root)) {
                    importRoots.add(root);
                    break;
                }
            }
        }
        OutputLocation.OutputLocationResolver execrootResolver = DefaultOutputLocationResolver.createExecrootResolving(myContext.bazelInfo());
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        Set<Path> individualFiles = new LinkedHashSet<>();
        Stream.concat(target.getSources().getOutputLocations(), generatedSources(target)).forEach(location -> {
            Path original = execrootResolver.resolve(location, localRepositories);
            Path generatedSourcePath = original != null ? myGeneratedSourcePaths.get(original) : null;
            Path path = generatedSourcePath != null ? generatedSourcePath : myContext.outputResolver().resolve(location, localRepositories);
            if (path != null && !PathUtil.isUnder(path, importRoots)) {
                individualFiles.add(path);
            }
        });
        Set<Path> result = new LinkedHashSet<>(importRoots);
        result.addAll(individualFiles);
        return result;
    }

    /**
     * consulo: upstream ContentRootEntity(url, excludedPatterns = []) with one SourceRootEntity(url, rootTypeId); the classic
     * content entry is added to the root model by {@link #addContentRoot}
     */
    private record ContentRootData(String url, String rootType) {
    }

    private static ContentRootData toContentRoot(Path receiver, String rootType) {
        String url = VirtualFileUtil.pathToUrl(receiver.toString());
        return new ContentRootData(url, rootType);
    }

    private static void addContentRoot(ModifiableRootModel model, ContentRootData contentRoot) {
        ContentEntry entry = model.addContentEntry(contentRoot.url());
        ContentFolderTypeProvider rootType = ContentFolderTypeProvider.byId(contentRoot.rootType());
        if (rootType != null) {
            entry.addFolder(contentRoot.url(), rootType);
        }
    }

    /**
     * An interpreter for {@code binary}, owned by the project working directory, the Bazel project root. It is not registered
     * yet: setting it on a module registers it.
     * <p>
     * consulo: upstream createLocalSdkGuessingTypeByPath(binary, ProjectOnly(project), name); here an existing Python SDK with
     * this home or a new SDK of the SDK table (a failed future when the binary is not a valid Python SDK home)
     */
    private CompletableFuture<Sdk> createInterpreter(Path binary, Project project) {
        PythonSdkType sdkType = PythonSdkType.getInstance();
        Path home = binary.toAbsolutePath().normalize();
        for (Sdk sdk : PythonSdkType.getAllSdks()) {
            String sdkHomePath = sdk.getHomePath();
            if (sdkHomePath != null && Path.of(sdkHomePath).toAbsolutePath().normalize().equals(home)) {
                return CompletableFuture.completedFuture(sdk);
            }
        }
        // consulo: PythonSdkType is a PlatformAwareSdkType, the String isValidSdkHome overload is not supported
        if (!sdkType.isValidSdkHome(Platform.current(), binary)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("not a valid Python SDK home"));
        }
        Sdk sdk;
        try {
            sdk = SdkTable.getInstance().createSdk(binary, chooseSdkName(binary, project.getName()), sdkType);
            sdkType.setupSdkPaths(sdk);
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
        // consulo: the SDK is added to the SDK table in a write action on the UI thread, like the JDKs of the Java importer
        return project.getUIAccess().giveAsync(() -> WriteAction.compute(() -> {
            SdkTable.getInstance().addSdk(sdk);
            return sdk;
        }));
    }

    // consulo: upstream public top-level function of BazelPythonWorkspaceImporter.kt (PythonBinary is a Path)
    public static String chooseSdkName(Path interpreter, String projectName) {
        return projectName + "-python-" + StringUtils.md5Hash(interpreter.toString(), 5);
    }

    // consulo: Kotlin Path.extension
    private static String extension(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? consulo.util.io.FileUtil.getExtension(fileName.toString()) : "";
    }
}
