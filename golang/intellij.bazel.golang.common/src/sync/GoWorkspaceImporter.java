package org.jetbrains.bazel.golang.sync;

import com.goide.sdk.GoSdkType;
import com.goide.sdk.GoSdkUtil;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.content.bundle.SdkUtil;
import consulo.go.module.extension.GoMutableModuleExtension;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.util.ModuleRootModificationUtil;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.util.interner.Interner;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity.BazelGoTargetEntity;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity.ImportPathId;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class GoWorkspaceImporter implements BazelWorkspaceImporter {
    // consulo: the upstream nested `Factory` is the top-level GoWorkspaceImporterFactory (an @ExtensionImpl cannot be nested)

    private final WorkspaceImporterContext myContext;

    private Map<WorkspaceTargetKey, GoBuildTarget> myGoTargets = Map.of();
    private Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> myGoTargetDependencies = Map.of();
    private final Interner<String> myImportPathInterner = Interner.createWeakInterner();

    public GoWorkspaceImporter(WorkspaceImporterContext context) {
        myContext = context;
    }

    public WorkspaceImporterContext getContext() {
        return myContext;
    }

    public Map<WorkspaceTargetKey, GoBuildTarget> getGoTargets() {
        return myGoTargets;
    }

    public Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> getGoTargetDependencies() {
        return myGoTargetDependencies;
    }

    @Override
    public LocalizeValue getImporterName() {
        return BazelPluginLocalize.consoleTaskModelGoImporter();
    }

    @Override
    public CompletableFuture<WorkspaceImporterResult> doImport(WorkspaceImporterPhase phase, WorkspaceSnapshot snapshot, TaskId taskId) {
        // consulo: an exception of a failed phase -> a failed future
        try {
            if (phase instanceof WorkspaceImporterPhase.Initialize) {
                int importDepth = WorkspaceHelpers.getCommonSyncConfig(snapshot).importDepth();
                List<Pair<BuildTarget, GoBuildTarget>> goBuildTargetList = WorkspaceHelpers.filterBuildTarget(
                        snapshot.targetGraph().findAllTargetsAtDepth(importDepth, importDepth < 0, true)
                            .map(it -> snapshot.targets().findTargetByKey(it))
                            .filter(Objects::nonNull),
                        GoBuildTarget.class
                    )
                    .toList();
                Map<WorkspaceTargetKey, GoBuildTarget> goTargets = new LinkedHashMap<>();
                Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> goTargetDependencies = new LinkedHashMap<>();
                for (Pair<BuildTarget, GoBuildTarget> it : goBuildTargetList) {
                    goTargets.put(it.getFirst().getKey(), it.getSecond());
                    goTargetDependencies.put(
                        it.getFirst().getKey(),
                        it.getFirst().getDependencies().stream().map(DependencyLabel::targetKey).toList()
                    );
                }
                myGoTargets = goTargets;
                myGoTargetDependencies = goTargetDependencies;
                if (myGoTargets.isEmpty()) {
                    return CompletableFuture.completedFuture(WorkspaceImporterResult.Abort);
                }
            }
            else if (phase instanceof WorkspaceImporterPhase.WorkspaceApply apply) {
                onWorkspaceApply(snapshot, apply.builder(), apply.entitySource());
            }
            else if (phase instanceof WorkspaceImporterPhase.PostProcessing) {
                return onPostProcessing(snapshot, taskId).thenApply(ignored -> WorkspaceImporterResult.Success);
            }
            return CompletableFuture.completedFuture(WorkspaceImporterResult.Success);
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void onWorkspaceApply(
        WorkspaceSnapshot snapshot,
        BazelModelBuilder builder,
        BazelEntitySource entitySource
    ) {
        addGoWorkspaceModule(builder, snapshot, entitySource);
        addGoPackageEntities(builder, snapshot, entitySource);
    }

    /**
     * Go plugin expects all sources to be under a content root.
     * Otherwise, e.g., Go SDK references aren't resolved, and source files aren't indexed.
     * By the way, we *don't* need to add source roots, those are specific to Java/Python and are ignored by the Go plugin.
     */
    private void addGoWorkspaceModule(
        BazelModelBuilder builder,
        WorkspaceSnapshot snapshot,
        BazelEntitySource entitySource
    ) {
        Project project = myContext.project();

        Path workspacePath = org.jetbrains.bazel.config.project.getRootDir(project).toNioPath();

        Set<Path> goSourcesParentDirectorySet = new LinkedHashSet<>();
        for (GoBuildTarget it : myGoTargets.values()) {
            it.sources().getOutputLocations()
                .map(location -> myContext.outputResolver().resolve(location, RepoMapping.getLocalRepositories(snapshot.repoMapping())))
                .filter(Objects::nonNull)
                .filter(source -> source.startsWith(workspacePath))  // External files are handled in GoExternalLibraryManager
                .map(Path::getParent)
                .filter(Objects::nonNull)
                .forEach(goSourcesParentDirectorySet::add);
        }
        List<Path> goSourcesParentDirectories = PathUtil.filterPathsThatDontContainEachOther(goSourcesParentDirectorySet);
        if (goSourcesParentDirectories.isEmpty()) {
            return;
        }

        // consulo: ModuleEntity(name, dependencies = [ModuleSourceDependency]) -> a Bazel-owned module of the builder;
        // the module source dependency is implicit in Consulo, the entity source is tracked by BazelModelBuilder
        Module module = builder.getOrCreateModule(GoWorkspaceModuleUtil.GO_WORKSPACE_MODULE_NAME, null);
        ModifiableRootModel model = builder.getRootModel(module);
        for (Path directory : goSourcesParentDirectories) {
            // consulo: ContentRootEntity(url, excludedPatterns = []) -> a content entry
            model.addContentEntry(VirtualFileUtil.pathToUrl(directory.toString()));
        }

        // consulo: a module is a Go module when its Go module extension is enabled (upstream GoModuleSettings.isGoSupportEnabled,
        // set by GoModuleSupportPostSyncHook). The root model is rebuilt by every sync, so the extension is enabled here;
        // the SDK of the extension is set after the commit, see setAsUsed
        GoMutableModuleExtension goExtension = model.getExtensionWithoutCheck(GoMutableModuleExtension.class);
        if (goExtension != null) {
            goExtension.setEnabled(true);
        }
    }

    private void addGoPackageEntities(
        BazelModelBuilder builder,
        WorkspaceSnapshot snapshot,
        BazelEntitySource entitySource
    ) {
        Map<WorkspaceTargetKey, String> inferredImportPath = inferImportPath(myGoTargets);

        Map<String, List<Map.Entry<WorkspaceTargetKey, GoBuildTarget>>> targetsByImportPath = new LinkedHashMap<>();
        for (Map.Entry<WorkspaceTargetKey, GoBuildTarget> it : myGoTargets.entrySet()) {
            String importPath = inferredImportPath.get(it.getKey());
            targetsByImportPath.computeIfAbsent(importPath != null ? importPath : it.getValue().importPath(), key -> new ArrayList<>())
                .add(it);
        }

        // consulo: the entities are collected into GoPackageEntities, the project data of the builder
        List<BazelGoPackageEntity> packageEntities = new ArrayList<>();
        List<BazelGoTargetEntity> targetEntities = new ArrayList<>();
        for (Map.Entry<String, List<Map.Entry<WorkspaceTargetKey, GoBuildTarget>>> entry : targetsByImportPath.entrySet()) {
            String importPath = entry.getKey();
            List<Map.Entry<WorkspaceTargetKey, GoBuildTarget>> goTargets = entry.getValue();

            Set<Path> sources = new LinkedHashSet<>();
            for (Map.Entry<WorkspaceTargetKey, GoBuildTarget> target : goTargets) {
                target.getValue().sources().getOutputLocations()
                    .map(location -> myContext.outputResolver().resolve(location, RepoMapping.getLocalRepositories(snapshot.repoMapping())))
                    .filter(Objects::nonNull)
                    .forEach(sources::add);
            }

            Set<String> directDepsImportPaths = new LinkedHashSet<>();
            goTargets.stream()
                .flatMap(it -> myGoTargetDependencies.getOrDefault(it.getKey(), List.of()).stream())
                .distinct()
                .map(it -> {
                    String inferred = inferredImportPath.get(it);
                    if (inferred != null) {
                        return inferred;
                    }
                    GoBuildTarget dependency = myGoTargets.get(it);
                    return dependency != null ? dependency.importPath() : null;
                })
                .filter(Objects::nonNull)
                .map(myImportPathInterner::intern)
                .forEach(directDepsImportPaths::add);

            // Group targets with the same importpath, see doc for BazelGoPackageEntity
            BazelGoPackageEntity packageEntity = new BazelGoPackageEntity(
                myImportPathInterner.intern(importPath),
                sources.stream().map(it -> VirtualFileUtil.pathToUrl(it.toString())).toList(),
                directDepsImportPaths
            );
            packageEntities.add(packageEntity);
            ImportPathId importPathId = packageEntity.getSymbolicId();

            for (Map.Entry<WorkspaceTargetKey, GoBuildTarget> target : goTargets) {
                targetEntities.add(new BazelGoTargetEntity(
                    WorkspaceModelTargetKey.of(target.getKey()),
                    importPathId
                ));
            }
        }
        builder.putProjectData(GoPackageEntities.class, new GoPackageEntities(packageEntities, targetEntities));
    }

    private Map<WorkspaceTargetKey, String> inferImportPath(Map<WorkspaceTargetKey, GoBuildTarget> targetsToImport) {
        Map<WorkspaceTargetKey, String> inferredImportPath = new HashMap<>();

        for (Map.Entry<WorkspaceTargetKey, GoBuildTarget> entry : targetsToImport.entrySet()) {
            WorkspaceTargetKey key = entry.getKey();
            GoBuildTarget goTarget = entry.getValue();
            List<WorkspaceTargetKey> embedList = goTarget.embed();

            // From https://github.com/bazel-contrib/rules_go/blob/master/docs/go/core/rules.md#go_library-importpath:
            // importpath must either be specified in go_library or inherited from one of the libraries in embed.
            String importPath;
            if (!goTarget.importPath().isEmpty()) {
                importPath = goTarget.importPath();
            }
            else {
                String inferred = embedList.stream()
                    .map(embed -> {
                        GoBuildTarget embedTarget = targetsToImport.get(embed);
                        return embedTarget != null ? embedTarget.importPath() : null;
                    })
                    .filter(it -> it != null && !it.isEmpty())
                    .findFirst()
                    .orElse("");
                if (!inferred.isEmpty()) {
                    inferredImportPath.put(key, inferred);
                }
                importPath = inferred;
            }

            if (!importPath.isEmpty()) {
                // go_source doesn't have importpath (https://github.com/bazel-contrib/rules_go/blob/master/docs/go/core/rules.md#go_source)
                // But we can infer it from the library that embeds it
                for (WorkspaceTargetKey embed : embedList) {
                    GoBuildTarget embedTarget = targetsToImport.get(embed);
                    if (embedTarget != null && embedTarget.importPath().isEmpty()) {
                        inferredImportPath.put(embed, importPath);
                    }
                }
            }
        }
        return inferredImportPath;
    }

    private CompletableFuture<Void> onPostProcessing(WorkspaceSnapshot snapshot, TaskId taskId) {
        return calculateAndAddGoSdk(snapshot, myContext.project(), taskId).thenRun(() -> {
            // TODO consulo: GoWrongSdkConfigurationNotificationProvider.disableNotification(project) is missing, Consulo's
            //  WrongSdkConfigurationNotificationProvider has no way to disable the notification
            GoExternalLibraryManager.getInstance(myContext.project()).update();
        });
    }

    private CompletableFuture<Void> calculateAndAddGoSdk(
        WorkspaceSnapshot snapshot, Project project,
        TaskId taskId
    ) {
        return util.withSubtask(
            util.getSyncConsole(project),
            myContext.progressReporter(),
            taskId.subTask("calculate-and-add-go-sdk"),
            BazelPluginLocalize.consoleTaskModelCalculateAddGoFetchedSdk(),
            subtaskId -> {
                Path path = myGoTargets
                    .values()
                    .stream()
                    .map(goTarget -> {
                        OutputLocation sdkHomePath = goTarget.sdkHomePath();
                        return sdkHomePath != null
                            ? myContext.outputResolver().resolve(sdkHomePath, RepoMapping.getLocalRepositories(snapshot.repoMapping()))
                            : null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
                String homePath;
                if (path != null) {
                    homePath = path.toString();
                }
                else {
                    // consulo: IntelliJ GoSdkUtil.suggestSdkDirectory() returns the directory, the Consulo one returns candidate paths
                    homePath = GoSdkUtil.suggestSdkDirectory().stream()
                        .filter(it -> GoSdkType.getInstance().isValidSdkHome(it))
                        .findFirst()
                        .orElse(null);
                }
                if (homePath == null) {
                    return CompletableFuture.completedFuture(null);
                }
                // consulo: upstream GoSdk.fromHomePath(path).setAsUsed(project)
                return setAsUsed(homePath, project);
            }
        );
    }

    /**
     * consulo: upstream {@code GoSdk.setAsUsed(project)} sets the project Go SDK with {@code GoSdkService.setSdk}. Consulo has no
     * project Go SDK, the SDK belongs to the Go module extension: an SDK of the SDK table with this home (created if needed) is set
     * to the extension of the Go workspace module.
     */
    private static CompletableFuture<Void> setAsUsed(String homePath, Project project) {
        return project.getUIAccess().giveAsync(() -> {
            Sdk sdk = findOrCreateGoSdk(homePath);
            Module workspaceModule = GoWorkspaceModuleUtil.findModule(project);
            if (sdk == null || workspaceModule == null) {
                return;
            }
            // consulo: upstream edtWriteAction { }; updateModel commits in a write action itself
            ModuleRootModificationUtil.updateModel(workspaceModule, model -> {
                GoMutableModuleExtension goExtension = model.getExtensionWithoutCheck(GoMutableModuleExtension.class);
                if (goExtension == null) {
                    return;
                }
                goExtension.getInheritableSdk().set(null, sdk);
                if (model.findModuleExtensionSdkEntry(goExtension) == null) {
                    model.addModuleExtensionSdkEntry(goExtension);
                }
            });
        }).thenApply(ignored -> null);
    }

    // consulo: like GoModuleDataService.chooseSdk of consulo-google-go, an existing Go SDK with this home or a new one
    private static @Nullable Sdk findOrCreateGoSdk(String homePath) {
        GoSdkType sdkType = GoSdkType.getInstance();
        Path home = Path.of(homePath).toAbsolutePath().normalize();
        for (Sdk sdk : SdkTable.getInstance().getSdksOfType(sdkType)) {
            String sdkHomePath = sdk.getHomePath();
            if (sdkHomePath != null && Path.of(sdkHomePath).toAbsolutePath().normalize().equals(home)) {
                return sdk;
            }
        }
        // consulo: GoSdkType is a plain SdkType, so its String isValidSdkHome overload is supported
        if (!sdkType.isValidSdkHome(homePath)) {
            return null;
        }
        return SdkUtil.createAndAddSDK(homePath, sdkType, UIAccess.current());
    }
}
