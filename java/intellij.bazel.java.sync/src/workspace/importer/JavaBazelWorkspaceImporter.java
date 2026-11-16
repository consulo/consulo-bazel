package org.jetbrains.bazel.workspace.importer;

import consulo.application.Application;
import consulo.bazel.java.localize.BazelJavaBackendLocalize;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.content.bundle.Sdk;
import consulo.java.language.module.extension.JavaMutableModuleExtension;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.process.cmd.ParametersListUtil;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.config.project;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.GlobalNamingContextBuilder;
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavacOptionsUpdater;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.CommonWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.workspace.ProjectViewGlobSet;
import org.jetbrains.bazel.workspace.importer.JvmTargetEntitiesBuilder.ImportContext;
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource;
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class JavaBazelWorkspaceImporter implements BazelWorkspaceImporter {
    // consulo: the upstream nested `Factory` is the top-level JavaBazelWorkspaceImporterFactory (an @ExtensionImpl cannot be nested)

    private final WorkspaceImporterContext myContext;

    private @Nullable Map<String, String> myJavacOptions = null;
    private List<BuildTarget> myModuleTargets = List.of();
    private List<BuildTarget> myTargets = List.of();
    private Map<WorkspaceTargetKey, JvmResolvedTarget> myJvmResolved = Map.of();
    private @Nullable JvmImportPlan myPlan;
    private Set<Path> myUniqueJavaHomes = Set.of();
    private @Nullable CommonWorkspaceSyncConfig myCommonSyncConfig;
    private @Nullable JavaWorkspaceSyncConfig myJavaSyncConfig;
    private @Nullable String myDefaultJdkName = null;

    public JavaBazelWorkspaceImporter(WorkspaceImporterContext context) {
        myContext = context;
    }

    public WorkspaceImporterContext getContext() {
        return myContext;
    }

    @Override
    public LocalizeValue getImporterName() {
        return BazelJavaBackendLocalize.workspaceJavaImporterName();
    }

    @Override
    public CompletableFuture<WorkspaceImporterResult> doImport(WorkspaceImporterPhase phase, WorkspaceSnapshot snapshot, TaskId taskId) {
        // consulo: an exception thrown by a phase fails the returned future
        try {
            if (phase instanceof WorkspaceImporterPhase.Initialize initialize) {
                return CompletableFuture.completedFuture(onInitialize(snapshot, initialize.naming()));
            }
            else if (phase instanceof WorkspaceImporterPhase.WorkspaceApply apply) {
                return onWorkspaceApply(snapshot, apply.builder(), apply.entitySource(), apply.naming(), taskId);
            }
            else if (phase instanceof WorkspaceImporterPhase.Finalize) {
                return CompletableFuture.completedFuture(onFinalize(snapshot));
            }
            else {
                return onPostProcessing(snapshot);
            }
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public WorkspaceImporterResult onInitialize(
        WorkspaceSnapshot snapshot,
        GlobalNamingContextBuilder naming
    ) {
        CommonWorkspaceSyncConfig commonSyncConfig = snapshot.syncConfigs().stream()
            .filter(CommonWorkspaceSyncConfig.class::isInstance)
            .map(CommonWorkspaceSyncConfig.class::cast)
            .findFirst()
            .orElseThrow(IllegalStateException::new);
        myCommonSyncConfig = commonSyncConfig;
        JavaWorkspaceSyncConfig javaSyncConfig = snapshot.syncConfigs().stream()
            .filter(JavaWorkspaceSyncConfig.class::isInstance)
            .map(JavaWorkspaceSyncConfig.class::cast)
            .findFirst()
            .orElseThrow(IllegalStateException::new);
        myJavaSyncConfig = javaSyncConfig;

        int importDepth = commonSyncConfig.importDepth();
        myModuleTargets = snapshot.targetGraph().findAllTargetsAtDepth(
                importDepth,
                importDepth < 0,
                true
            )
            .map(it -> snapshot.targets().findTargetByKey(it))
            .filter(Objects::nonNull)
            .toList();
        myTargets = myModuleTargets;
        Function<OutputLocation, @Nullable Path> resolveLocation = locationResolver(myContext, snapshot);
        Map<WorkspaceTargetKey, BuildTarget> allTargets = new LinkedHashMap<>();
        snapshot.targets().allTargets().forEach(it -> allTargets.put(it.getKey(), it));
        Map<WorkspaceTargetKey, BuildTarget> targetsToImport = new LinkedHashMap<>();
        for (BuildTarget it : myModuleTargets) {
            targetsToImport.put(it.getKey(), it);
        }
        myJvmResolved = new JvmBuildTargetResolver(
            allTargets,
            targetsToImport,
            javaSyncConfig,
            snapshot.repoMapping(),
            resolveLocation,
            execrootLocationResolver(myContext, snapshot)
        ).resolveAll();

        JvmImportPlan plan = new JvmImportPlan(myModuleTargets, myJvmResolved, resolveLocation);
        myPlan = plan;
        plan.declareNames(naming);

        // TODO: check why is this even needed - can't we just write SdkEntity into the project workspace model
        //  and avoid the global JDK table altogether?
        Set<Path> uniqueJavaHomes = new LinkedHashSet<>();
        for (JvmResolvedTarget it : myJvmResolved.values()) {
            if (it.javaHome() != null) {
                uniqueJavaHomes.add(it.javaHome());
            }
        }
        myUniqueJavaHomes = uniqueJavaHomes;
        if (!myUniqueJavaHomes.isEmpty()) {
            myDefaultJdkName = JvmTargetEntitiesBuilder.projectNameToJdkName(
                project.getBazelProjectName(myContext.project()),
                myUniqueJavaHomes.iterator().next()
            );
        }
        else {
            Sdk sdk = SdkUtils.getProjectJdkOrMostRecentJdk(myContext.project());
            myDefaultJdkName = sdk != null ? sdk.getName() : null;
        }
        return WorkspaceImporterResult.Success;
    }

    public CompletableFuture<WorkspaceImporterResult> onWorkspaceApply(
        WorkspaceSnapshot snapshot,
        BazelModelBuilder builder, BazelEntitySource entitySource,
        GlobalNamingContext naming,
        TaskId taskId
    ) {
        return util.withSubtask(
                myContext.taskConsole(),
                taskId.subTask("update-internal-model"),
                BazelJavaBackendLocalize.workspaceJavaImporterUpdateInternalModel(),
                subtaskId -> updateInternalModelSubtask(snapshot, builder, entitySource, naming)
            )
            .thenApply(ignored -> WorkspaceImporterResult.Success);
    }

    public WorkspaceImporterResult onFinalize(WorkspaceSnapshot snapshot) {
        return WorkspaceImporterResult.Success;
    }

    public CompletableFuture<WorkspaceImporterResult> onPostProcessing(WorkspaceSnapshot snapshot) {
        CompletableFuture<Void> result = SdkUtils.cleanUpInvalidJdks(myContext.project());
        for (Path it : myUniqueJavaHomes) {
            result = result.thenCompose(ignored -> SdkUtils.addJdkIfNeeded(
                project.getBazelProjectName(myContext.project()),
                it
            ));
        }

        return result.thenApply(ignored -> {
            Map<String, String> javacOptions = Objects.requireNonNull(myJavacOptions, "javacOptions is null but expected to be computed");
            // consulo: Consulo keeps the javac options per module in the Java module extension, they are also applied to the root
            // models in updateInternalModelSubtask
            for (JavacOptionsUpdater it : Application.get().getExtensionList(JavacOptionsUpdater.class)) {
                it.updateJavacOptions(myContext.project(), javacOptions);
            }
            return WorkspaceImporterResult.Success;
        });
    }

    private CompletableFuture<Void> updateInternalModelSubtask(
        WorkspaceSnapshot snapshot,
        BazelModelBuilder builder,
        BazelEntitySource entitySource,
        GlobalNamingContext naming
    ) {
        Function<OutputLocation, @Nullable Path> resolveLocation = locationResolver(myContext, snapshot);
        CommonWorkspaceSyncConfig commonSyncConfig = Objects.requireNonNull(myCommonSyncConfig);
        JavaWorkspaceSyncConfig javaSyncConfig = Objects.requireNonNull(myJavaSyncConfig);

        ImportContext importContext = new ImportContext(
            Objects.requireNonNull(myPlan),
            naming,
            myJvmResolved,
            snapshot.repoMapping(),
            commonSyncConfig.projectName(),
            commonSyncConfig.projectRootDir(),
            myDefaultJdkName,
            ProjectViewGlobSet.of(commonSyncConfig.projectRootDir(), javaSyncConfig.testSourcesPatterns()),
            snapshot.fileToTarget(),
            entitySource,
            javaSyncConfig.excludeCompiledSourceCodeInsideJars(),
            // consulo: upstream reads the entity from the current workspace model snapshot
            BazelProjectModelService.getInstance(myContext.project()).getProjectData(CompiledSourceCodeInsideJarExcludeEntity.class),
            resolveLocation,
            execrootLocationResolver(myContext, snapshot),
            null
        );

        // consulo: telemetry is not ported (bspTracer span "load.modules.ms")
        return new JvmTargetEntitiesBuilder(importContext).writeAll(builder).thenRun(() -> {
            Map<String, String> javacOptions = calculateAllJavacOptions(importContext);
            myJavacOptions = javacOptions;
            applyJavacOptions(builder, javacOptions);
        });
    }

    // consulo: JavacConfiguration.ADDITIONAL_OPTIONS_OVERRIDE (module name -> javac options) does not exist,
    // the options become the compiler arguments of the Java module extension of the module
    private static void applyJavacOptions(BazelModelBuilder builder, Map<String, String> javacOptions) {
        for (Map.Entry<String, String> entry : javacOptions.entrySet()) {
            Module module = builder.findModule(entry.getKey());
            if (module == null) {
                continue;
            }
            JavaMutableModuleExtension<?> javaExtension = builder.getRootModel(module).getExtensionWithoutCheck(JavaMutableModuleExtension.class);
            if (javaExtension != null) {
                javaExtension.setCompilerArguments(ParametersListUtil.parse(entry.getValue()));
            }
        }
    }

    private static Function<OutputLocation, @Nullable Path> locationResolver(WorkspaceImporterContext context, WorkspaceSnapshot snapshot) {
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        return it -> context.outputResolver().resolve(it, localRepositories);
    }

    private static Function<OutputLocation, @Nullable Path> execrootLocationResolver(WorkspaceImporterContext context, WorkspaceSnapshot snapshot) {
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        OutputLocationResolver resolver = DefaultOutputLocationResolver.createExecrootResolving(context.bazelInfo());
        return it -> resolver.resolve(it, localRepositories);
    }

    private static HashMap<String, String> calculateAllJavacOptions(ImportContext ctx) {
        HashMap<String, String> result = new HashMap<>();
        for (BuildTarget target : ctx.getTargets()) {
            JvmBuildTarget jvm = JvmBuildTarget.extractJvmBuildTarget(target);
            if (jvm == null) {
                continue;
            }
            StringsCollection options = jvm.javacOpts();
            if (options.isEmpty()) {
                continue;
            }
            if (options.getSize() == 1 && options.get(0).equals("-proc:none")) {
                continue;
            }
            String moduleName = ctx.getModuleNamesByKey().get(target.getKey());
            if (moduleName == null) {
                continue;
            }
            result.put(moduleName, String.join(" ", options.asList()));
        }
        return result;
    }
}
