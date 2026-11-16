package org.jetbrains.bazel.workspace.importer;

import com.intellij.java.language.LanguageLevel;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.bazel.java.localize.BazelJavaBackendLocalize;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.content.library.Library;
import consulo.java.language.module.extension.JavaMutableModuleExtension;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.commons.symlinks.BazelConvenienceSymlinksScanner;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.scala.sdk.ScalaSdkExtension;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameSpace;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.ScalaBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.target.BuildTargetUtils;
import org.jetbrains.bazel.workspace.ProjectViewGlobSet;
import org.jetbrains.bazel.workspace.importer.DependencyBuilder.ModuleDependencyItem;
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource;
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions.BazelModuleExtensionEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetLabelList;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetSourceRootTypeId;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.WorkspaceLibraries.LibraryItem;
import org.jetbrains.bsp.protocol.utils.StringUtils;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Writes the full set of JVM workspace-model entities for all {@code ImportContext.targets} directly
 * into the supplied {@link BazelModelBuilder}.
 * <p>
 * Runs two passes: first resolves every target into a {@code TargetPlan} (in parallel, no writes);
 * then writes all entities sequentially.
 * <p>
 * consulo: the entities are the classic modules (root models), libraries and the builder data of the custom entities
 */
public class JvmTargetEntitiesBuilder {
    /**
     * shared per-import-pass context for {@link JvmTargetEntitiesBuilder} and the per-aspect builders it calls.
     * build it once per import; reuse across all per-target invocations.
     * <p>
     * consulo: upstream {@code virtualFileUrlManager} (workspace model) is dropped, urls are VFS url strings
     */
    // RC: replaces `ProjectDetails` + the per-target `ModuleDetails` intermediates
    public static class ImportContext {
        private final JvmImportPlan myPlan;
        private final GlobalNamingContext myNaming;
        private final Map<WorkspaceTargetKey, JvmResolvedTarget> myJvmResolved;
        private final RepoMapping myRepoMapping;
        private final String myProjectName;
        private final Path myProjectBasePath;
        private final @Nullable String myDefaultJdkName;
        private final ProjectViewGlobSet myTestSourcesGlob;
        private final FileToTargetMap myFileToTargets;
        private final BazelEntitySource myEntitySource;
        private final boolean myExcludeCompiledSourceCodeInsideJars;
        private final @Nullable CompiledSourceCodeInsideJarExcludeEntity myCurrentCompiledSourceExcludeEntity;
        private final Function<OutputLocation, @Nullable Path> myResolveLocation;
        private final Function<OutputLocation, @Nullable Path> myResolveExecrootLocation;
        private final @Nullable ProgressIndicator myProgressReporter;

        private final Supplier<Set<WorkspaceTargetKey>> myTargetKeys;

        private final Map<WorkspaceTargetKey, String> myModuleNamesByKey;

        private final Set<String> myKnownModuleNames;

        private final Map<WorkspaceTargetKey, String> myLibraryNamesByKey;

        private final Map<Label, List<String>> myLibrariesByLabel;

        private final DependencyBuilder myDependencyBuilder;
        private final JavaSourceRootMerger mySourceRootMerger;
        private final BazelConvenienceSymlinksScanner myConvenienceSymlinksScanner;

        ImportContext(
            JvmImportPlan plan,
            GlobalNamingContext naming,
            Map<WorkspaceTargetKey, JvmResolvedTarget> jvmResolved,
            RepoMapping repoMapping,
            String projectName,
            Path projectBasePath,
            @Nullable String defaultJdkName,
            ProjectViewGlobSet testSourcesGlob,
            FileToTargetMap fileToTargets,
            BazelEntitySource entitySource,
            boolean excludeCompiledSourceCodeInsideJars,
            @Nullable CompiledSourceCodeInsideJarExcludeEntity currentCompiledSourceExcludeEntity,
            Function<OutputLocation, @Nullable Path> resolveLocation,
            Function<OutputLocation, @Nullable Path> resolveExecrootLocation,
            @Nullable ProgressIndicator progressReporter
        ) {
            myPlan = plan;
            myNaming = naming;
            myJvmResolved = jvmResolved;
            myRepoMapping = repoMapping;
            myProjectName = projectName;
            myProjectBasePath = projectBasePath;
            myDefaultJdkName = defaultJdkName;
            myTestSourcesGlob = testSourcesGlob;
            myFileToTargets = fileToTargets;
            myEntitySource = entitySource;
            myExcludeCompiledSourceCodeInsideJars = excludeCompiledSourceCodeInsideJars;
            myCurrentCompiledSourceExcludeEntity = currentCompiledSourceExcludeEntity;
            myResolveLocation = resolveLocation;
            myResolveExecrootLocation = resolveExecrootLocation;
            myProgressReporter = progressReporter;

            myTargetKeys = LazyValue.notNull(() -> {
                Set<WorkspaceTargetKey> keys = new HashSet<>();
                for (BuildTarget it : myPlan.getTargets()) {
                    keys.add(it.getKey());
                }
                return keys;
            });

            Map<WorkspaceTargetKey, String> moduleNamesByKey = new LinkedHashMap<>();
            for (WorkspaceTargetKey it : myPlan.getModuleKeys()) {
                moduleNamesByKey.put(it, myNaming.findOrFallback(NameSpace.MODULE, JvmImportPlan.JVM_NAME_PRODUCER, it));
            }
            myModuleNamesByKey = moduleNamesByKey;

            myKnownModuleNames = new HashSet<>(myModuleNamesByKey.values());

            Map<WorkspaceTargetKey, String> libraryNamesByKey = new LinkedHashMap<>();
            for (WorkspaceTargetKey it : myPlan.getLibraryKeys()) {
                libraryNamesByKey.put(it, myNaming.findOrFallback(NameSpace.LIBRARY, JvmImportPlan.JVM_NAME_PRODUCER, it));
            }
            myLibraryNamesByKey = libraryNamesByKey;

            Map<Label, LinkedHashSet<String>> librariesByLabel = new LinkedHashMap<>();
            for (LibraryItem it : getLibraries()) {
                librariesByLabel.computeIfAbsent(it.key().label(), ignored -> new LinkedHashSet<>())
                    .add(Objects.requireNonNull(myLibraryNamesByKey.get(it.key())));
            }
            Map<Label, List<String>> distinctLibrariesByLabel = new LinkedHashMap<>();
            librariesByLabel.forEach((label, names) -> distinctLibrariesByLabel.put(label, new ArrayList<>(names)));
            myLibrariesByLabel = distinctLibrariesByLabel;

            myDependencyBuilder = new DependencyBuilder(getTargets(), myJvmResolved, myRepoMapping, getLibraryShadowedProducers());
            mySourceRootMerger = new JavaSourceRootMerger(myFileToTargets);
            myConvenienceSymlinksScanner = new BazelConvenienceSymlinksScanner(myProjectBasePath);
        }

        JvmImportPlan getPlan() {
            return myPlan;
        }

        GlobalNamingContext getNaming() {
            return myNaming;
        }

        Map<WorkspaceTargetKey, JvmResolvedTarget> getJvmResolved() {
            return myJvmResolved;
        }

        RepoMapping getRepoMapping() {
            return myRepoMapping;
        }

        String getProjectName() {
            return myProjectName;
        }

        Path getProjectBasePath() {
            return myProjectBasePath;
        }

        @Nullable String getDefaultJdkName() {
            return myDefaultJdkName;
        }

        ProjectViewGlobSet getTestSourcesGlob() {
            return myTestSourcesGlob;
        }

        FileToTargetMap getFileToTargets() {
            return myFileToTargets;
        }

        BazelEntitySource getEntitySource() {
            return myEntitySource;
        }

        boolean getExcludeCompiledSourceCodeInsideJars() {
            return myExcludeCompiledSourceCodeInsideJars;
        }

        @Nullable CompiledSourceCodeInsideJarExcludeEntity getCurrentCompiledSourceExcludeEntity() {
            return myCurrentCompiledSourceExcludeEntity;
        }

        Function<OutputLocation, @Nullable Path> getResolveLocation() {
            return myResolveLocation;
        }

        Function<OutputLocation, @Nullable Path> getResolveExecrootLocation() {
            return myResolveExecrootLocation;
        }

        @Nullable ProgressIndicator getProgressReporter() {
            return myProgressReporter;
        }

        List<BuildTarget> getTargets() {
            return myPlan.getTargets();
        }

        Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> getLibraryShadowedProducers() {
            return myPlan.getLibraryShadowedProducers();
        }

        List<LibraryItem> getLibraries() {
            return myPlan.getLibraries();
        }

        Map<WorkspaceTargetKey, String> getModuleNamesByKey() {
            return myModuleNamesByKey;
        }

        Set<String> getKnownModuleNames() {
            return myKnownModuleNames;
        }

        Map<WorkspaceTargetKey, String> getLibraryNamesByKey() {
            return myLibraryNamesByKey;
        }

        Map<Label, List<String>> getLibrariesByLabel() {
            return myLibrariesByLabel;
        }

        DependencyBuilder getDependencyBuilder() {
            return myDependencyBuilder;
        }

        JavaSourceRootMerger getSourceRootMerger() {
            return mySourceRootMerger;
        }

        BazelConvenienceSymlinksScanner getConvenienceSymlinksScanner() {
            return myConvenienceSymlinksScanner;
        }

        // targets contain stripped keys, so we need to ensure that `key` is stripped too
        boolean isInTargets(WorkspaceTargetKey key) {
            return myTargetKeys.get().contains(DependencyBuilder.stripAspects(key));
        }
    }

    // intentionally skip propagated aspect IDs, they're stripped before anyway
    private static final Comparator<WorkspaceTargetKey> TARGET_KEY_COMPARATOR =
        Comparator.<WorkspaceTargetKey, Label>comparing(WorkspaceTargetKey::label)
            .thenComparing(it -> it.configuration().getShortChecksum(), Comparator.nullsFirst(Comparator.<String>naturalOrder()));

    private final ImportContext myCtx;

    // consulo: upstream `javaModuleType = ModuleTypeId("JAVA_MODULE")` is the Java module extension (JavaMutableModuleExtension)
    private final int myResolverParallelism = Runtime.getRuntime().availableProcessors() * 2;
    private final int myResolverBatchSize = 512;

    JvmTargetEntitiesBuilder(ImportContext ctx) {
        myCtx = ctx;
    }

    public CompletableFuture<Void> writeAll(BazelModelBuilder storage) {
        AtomicInteger resolvedTargets = new AtomicInteger();
        ProgressIndicator progressReporter = myCtx.getProgressReporter();

        if (progressReporter != null) {
            progressReporter.setText2(LocalizeValue.empty());
            progressReporter.setText(BazelJavaBackendLocalize.workspaceJavaImporterWritingLibraries());
            progressReporter.setFraction(0.0);
        }

        // phase 0: write independent libraries
        LibraryBuilder.writeAll(
            myCtx.getLibraries(),
            myCtx.getEntitySource(),
            key -> getValue(myCtx.getLibraryNamesByKey(), key),
            storage,
            myCtx::isInTargets
        );

        if (progressReporter != null) {
            progressReporter.setText(BazelJavaBackendLocalize.workspaceJavaImporterResolvingTargets(myCtx.getTargets().size()));
            progressReporter.setFraction(0.0);
        }

        // phase 1: resolve every target. no storage writes.
        // consulo: Dispatchers.Default.limitedParallelism(resolverParallelism) -> a bounded application pool executor
        ExecutorService dispatcher = AppExecutorUtil.createBoundedApplicationPoolExecutor("Bazel JVM Target Resolver", myResolverParallelism);
        List<CompletableFuture<List<TargetAndPlan>>> batches = new ArrayList<>();
        List<BuildTarget> targets = myCtx.getTargets();
        for (int start = 0; start < targets.size(); start += myResolverBatchSize) {
            List<BuildTarget> batch = targets.subList(start, Math.min(start + myResolverBatchSize, targets.size()));
            batches.add(CompletableFuture.supplyAsync(() -> {
                List<TargetAndPlan> result = new ArrayList<>();
                for (BuildTarget it : batch) {
                    TargetPlan plan = resolve(it);
                    if (progressReporter != null) {
                        progressReporter.setFraction((double) resolvedTargets.incrementAndGet() / myCtx.getTargets().size());
                    }
                    if (plan != null) {
                        result.add(new TargetAndPlan(it, plan));
                    }
                }
                return result;
            }, dispatcher));
        }

        return CompletableFuture.allOf(batches.toArray(CompletableFuture[]::new)).thenAccept(ignored -> {
            List<TargetAndPlan> plans = new ArrayList<>();
            for (CompletableFuture<List<TargetAndPlan>> it : batches) {
                plans.addAll(it.join());
            }

            // phase 2: write entities sequentially.
            // `writtenNames` preserves the original `distinctBy { it.getModuleName() }` semantics: if two targets
            // (or dummies) end up with the same module name, the first one wins and the rest are skipped,
            // always write targets with `JvmBuildTarget` present
            if (progressReporter != null) {
                progressReporter.setText(BazelJavaBackendLocalize.workspaceJavaImporterBuildingModel());
            }
            Set<String> writtenNames = new HashSet<>();
            Comparator<TargetAndPlan> comparator = Comparator.<TargetAndPlan, WorkspaceTargetKey>comparing(it -> it.target().getKey(), TARGET_KEY_COMPARATOR)
                .thenComparing(it -> JvmBuildTarget.extractJvmBuildTarget(it.target()) != null, Comparator.reverseOrder())
                .thenComparing(it -> it.plan().moduleName());
            List<TargetAndPlan> sorted = new ArrayList<>(plans);
            sorted.sort(comparator);
            // consulo: upstream MutableEntityStorage needs no lock; the classic modifiable models read the module and library
            //   tables, which needs read access on this pool thread
            ReadAction.run(() -> {
                for (int index = 0; index < sorted.size(); index++) {
                    TargetAndPlan it = sorted.get(index);
                    writeOne(it.target(), it.plan(), writtenNames, storage);
                    if (progressReporter != null) {
                        progressReporter.setFraction((double) (index + 1) / plans.size());
                    }
                }
            });

            // phase 3: compute excluded sources inside Jars
            if (myCtx.getExcludeCompiledSourceCodeInsideJars()) {
                List<Path> productionSources = new ArrayList<>();
                for (TargetAndPlan it : plans) {
                    if (!(it.plan() instanceof TargetPlan.Full full)) {
                        continue;
                    }
                    for (SourceRootBuilder.ResolvedSourceRoot root : full.sourceRootsBeforeMerge()) {
                        // Don't exclude test classes (BAZEL-3633)
                        if (root.rootType().equals(SourceRootBuilder.JAVA_SOURCE_ROOT_TYPE) && !root.generated()) {
                            productionSources.add(root.sourcePath());
                        }
                    }
                }
                CompiledSourceCodeInsideJarExcludeBuilder.write(
                    productionSources,
                    myCtx.getLibraries(),
                    storage,
                    myCtx.getCurrentCompiledSourceExcludeEntity()
                );
            }

            if (progressReporter != null) {
                progressReporter.setText(LocalizeValue.empty());
            }
        });
    }

    // consulo: Kotlin Pair<BuildTarget, TargetPlan>
    private record TargetAndPlan(BuildTarget target, TargetPlan plan) {
    }

    /**
     * Returns null when the target should be skipped:
     * - non-JVM targets (matches the `isJvmTarget` filter in the old TargetIdToModuleEntitiesMap),
     * - kotlin with-sources targets when the facet EP is missing.
     */
    private @Nullable TargetPlan resolve(BuildTarget target) {
        if (!JavaLanguageClass.isJvmTarget(target.getKind())) {
            return null;
        }
        String moduleName = getValue(myCtx.getModuleNamesByKey(), withoutAspects(target.getKey()));
        DependencyBuilder.Resolved resolvedDeps = myCtx.getDependencyBuilder().resolve(target);
        String jdkName = jdkNameFor(target);
        JvmResolvedTarget jvmResolvedTarget = myCtx.getJvmResolved().get(withoutAspects(target.getKey()));
        String javaLangVersion = jvmResolvedTarget != null ? jvmResolvedTarget.javaVersion() : null;
        JvmBuildTarget jvmBuildTarget = WorkspaceHelpers.findBuildData(target, JvmBuildTarget.class);
        List<String> javacOptions = jvmBuildTarget != null ? jvmBuildTarget.javacOpts().asList() : List.of();
        // keep binary JARs stored as execroot paths for hotswap
        List<Path> jvmBinaryJars = jvmBuildTarget != null
            ? resolvePaths(jvmBuildTarget.binaryOutputs(), myCtx.getResolveExecrootLocation())
            : List.of();
        ScalaBuildTarget scalaTarget = WorkspaceHelpers.findBuildData(target, ScalaBuildTarget.class);
        KotlinBuildTarget kotlinTarget = WorkspaceHelpers.findBuildData(target, KotlinBuildTarget.class);
        List<String> associates = new ArrayList<>();
        if (kotlinTarget != null) {
            for (WorkspaceTargetKey it : new LinkedHashSet<>(kotlinTarget.associates())) {
                String associate = myCtx.getModuleNamesByKey().get(withoutAspects(it));
                if (associate != null) {
                    associates.add(associate);
                }
            }
        }

        boolean hasSources = !target.getSources().isEmpty();
        boolean hasResources = !target.getResources().isEmpty();
        boolean isJavaKotlin = JavaLanguageClass.includesJava(target.getKind()) || JavaLanguageClass.includesKotlin(target.getKind());

        if (!hasSources && !hasResources && isJavaKotlin) {
            return new TargetPlan.WithoutSources(moduleName, resolvedDeps, jdkName, javaLangVersion);
        }
        else if (JavaLanguageClass.includesKotlin(target.getKind()) && KotlinFacetEntityUpdater.getEp().getExtensionList().isEmpty()) {
            // original behavior: kotlin module with sources requires the EP, skip otherwise.
            //
            // but if target has both java + kotlin and `KotlinFacetEntityUpdater` is empty,
            // then module won't be created?
            //
            // TODO: I don't really know is that intended behavior, have to be investigated
            //  however `KotlinFacetEntityUpdater` to be empty is nearly impossible,
            //  someone would have to disable kotlin plugin explicitly (and then our content module won't be loaded) :p
            // TODO consulo: KotlinFacetEntityUpdater is implemented by the Kotlin integration only, without it Kotlin
            //  targets with sources are not imported
            return null;
        }
        else {
            List<SourceRootBuilder.ResolvedSourceRoot> resolvedSourceRoots =
                SourceRootBuilder.resolve(target, myCtx.getTestSourcesGlob(), myCtx.getResolveLocation());
            Path baseDirectory = myCtx.getResolveLocation().apply(BuildTargetUtils.getBaseDirectoryLocation(target));
            if (baseDirectory == null) {
                return null;
            }
            List<Path> resourceFiles = new ArrayList<>();
            target.getResources().getOutputLocations().forEach(it -> {
                Path resolved = myCtx.getResolveLocation().apply(it);
                if (resolved != null) {
                    resourceFiles.add(resolved);
                }
            });
            List<SourceRootBuilder.ResolvedSourceRoot> mainSourceRoots = myCtx.getSourceRootMerger().merge(
                baseDirectory,
                resolvedSourceRoots,
                resourceFiles
            );
            List<ResourceRootBuilder.ResolvedResourceRoot> resourceRoots = ResourceRootBuilder.resolve(
                target,
                baseDirectory,
                myCtx.getConvenienceSymlinksScanner(),
                myCtx.getResolveLocation()
            );
            return new TargetPlan.Full(
                moduleName,
                resolvedDeps,
                jdkName,
                javaLangVersion,
                javacOptions,
                jvmBinaryJars,
                scalaTarget,
                kotlinTarget,
                associates,
                mainSourceRoots,
                resourceRoots,
                resolvedSourceRoots
            );
        }
    }

    private void writeOne(
        BuildTarget target,
        TargetPlan plan,
        Set<String> writtenNames,
        BazelModelBuilder storage
    ) {
        if (!writtenNames.add(plan.moduleName())) {
            return;
        }
        if (plan instanceof TargetPlan.WithoutSources withoutSources) {
            writeWithoutSources(target, withoutSources, storage);
        }
        else if (plan instanceof TargetPlan.Full full) {
            writeFull(target, full, storage);
        }
    }

    private void writeWithoutSources(BuildTarget target, TargetPlan.WithoutSources plan, BazelModelBuilder storage) {
        // matches JavaModuleWithoutSourcesUpdater: only [SdkDep] as base, no ModuleSourceDependency
        // (sourceless modules don't own a source root, so no module-source-dependency).
        // consulo: the module source entry always exists in a Consulo root model
        List<ModuleDependencyItem> baseDeps;
        if (plan.jdkName() != null) {
            baseDeps = List.of(new ModuleDependencyItem.SdkDependency(plan.jdkName()));
        }
        else {
            baseDeps = List.of();
        }
        List<ModuleDependencyItem> deps = new ArrayList<>(baseDeps);
        deps.addAll(dependenciesAsItems(plan.moduleName(), plan.resolvedDeps(), null));
        addModuleEntity(target, plan.moduleName(), plan.resolvedDeps(), SourceRootBuilder.JAVA_SOURCE_ROOT_TYPE, deps, storage);
    }

    private void writeFull(
        BuildTarget target,
        TargetPlan.Full plan,
        BazelModelBuilder storage
    ) {
        ModuleDependencyItem scalaSdkDep = plan.scalaTarget() != null && ScalaSdkExtension.scalaSdkExtensionExists()
            ? DependencyBuilder.toLibraryDependency(scalaVersionToScalaSdkName(plan.scalaTarget().scalaVersion()), false)
            : null;
        List<ModuleDependencyItem> associatesDeps = plan.associates().stream().map(it -> moduleDependency(it, true)).toList();
        List<ModuleDependencyItem> deps = new ArrayList<>(baseDependencies(plan.jdkName()));
        deps.addAll(dependenciesAsItems(plan.moduleName(), plan.resolvedDeps(), scalaSdkDep));
        deps.addAll(associatesDeps);

        String defaultRootTypeId = target.getKind().ruleType() == RuleType.TEST
            ? SourceRootBuilder.JAVA_TEST_SOURCE_ROOT_TYPE
            : SourceRootBuilder.JAVA_SOURCE_ROOT_TYPE;
        Module moduleEntity = addModuleEntity(target, plan.moduleName(), plan.resolvedDeps(), defaultRootTypeId, deps, storage);
        addJavaModuleSettings(moduleEntity, plan.javaLangVersion(), storage);

        if (plan.scalaTarget() != null) {
            ScalaAddendumBuilder.write(plan.scalaTarget(), myCtx.getResolveLocation(), moduleEntity, storage);
        }

        ResourceRootBuilder.write(plan.resourceRoots(), moduleEntity, storage);
        SourceRootBuilder.write(plan.mainSourceRoots(), moduleEntity, myCtx.getProjectBasePath(), storage);

        if (!plan.jvmBinaryJars().isEmpty()) {
            JvmBinaryJarsBuilder.write(plan.jvmBinaryJars(), moduleEntity, storage);
        }

        if (plan.kotlinTarget() != null) {
            // TODO: migrate `KotlinFacetBuilder` and `KotlinFacetEntityUpdater` to language-agnostic
            //  extension point which can modify `ModuleEntity` based on `BuildTarget` passed
            //  by that we can completely get rid of language-specific logic from shared JVM importer
            //  this would remove both `KotlinFacetBuilder` and `ScalaAddendumBuilder` and make possible moving ep
            //  implementation to separate modules
            KotlinFacetBuilder.write(
                plan.kotlinTarget(),
                target.getKind().ruleType() == RuleType.TEST,
                new LinkedHashSet<>(plan.associates()),
                moduleEntity,
                storage
            );
        }
    }

    private Module addModuleEntity(
        BuildTarget target,
        String moduleName,
        DependencyBuilder.Resolved resolvedDeps,
        String defaultRootTypeId,
        List<ModuleDependencyItem> dependencies,
        BazelModelBuilder storage
    ) {
        // consulo: upstream entity source of the module, ownership is tracked by BazelModelBuilder
        BazelEntitySource entitySource = toEntitySource(target.getKind(), moduleName);
        Module module = storage.getOrCreateModule(moduleName, null);
        ModifiableRootModel model = storage.getRootModel(module);
        // consulo: this.type = javaModuleType
        JavaMutableModuleExtension<?> javaExtension = javaExtension(model);
        javaExtension.setEnabled(true);
        for (ModuleDependencyItem dependency : dependencies) {
            addDependency(model, javaExtension, dependency, storage);
        }
        storage.putModuleData(
            module,
            BazelModuleExtensionEntity.class,
            new BazelModuleExtensionEntity(
                WorkspaceModelTargetKey.of(target.getKey()),
                new WorkspaceModelTargetSourceRootTypeId(defaultRootTypeId),
                new WorkspaceModelTargetLabelList(
                    resolvedDeps.strictDependenciesCheck(),
                    resolvedDeps.strictDependencies().stream().map(Label::toString).toList()
                )
            )
        );
        // TODO consulo: upstream sets `exModuleOptions = ExternalSystemModuleOptionsEntity(externalSystem = BazelPluginConstants.ID)`;
        //  Consulo marks external system modules with an ExternalSystemModuleExtension, Bazel has none
        return module;
    }

    private void addJavaModuleSettings(Module parent, @Nullable String javaLanguageVersion, BazelModelBuilder storage) {
        // consulo: JavaModuleSettingsEntity -> the Java module extension; upstream inheritedCompilerOutput = false and
        // excludeOutput = true, Bazel modules have no compiler output in Consulo
        JavaMutableModuleExtension<?> javaExtension = javaExtension(storage.getRootModel(parent));
        LanguageLevel languageLevel = javaLanguageVersion != null ? LanguageLevel.parse(javaLanguageVersion) : null;
        String languageLevelId = languageLevel != null ? languageLevel.getName() : null;
        if (languageLevelId != null) {
            javaExtension.getInheritableLanguageLevel().set(null, languageLevelId);
        }
    }

    private static BazelEntitySource toEntitySource(TargetKind kind, String moduleName) {
        if (JavaLanguageClass.isJvmTarget(kind)) {
            return new BazelEntitySource.BazelModuleEntitySource(moduleName);
        }
        else {
            return BazelEntitySource.BazelDummyEntitySource.INSTANCE;
        }
    }

    private static List<ModuleDependencyItem> baseDependencies(@Nullable String jdkName) {
        List<ModuleDependencyItem> result = new ArrayList<>();
        // consulo: ModuleSourceDependency is implicit (the first order entry of a Consulo root model)
        if (jdkName != null) {
            result.add(new ModuleDependencyItem.SdkDependency(jdkName));
        }
        return result;
    }

    private List<ModuleDependencyItem> dependenciesAsItems(
        String moduleName,
        DependencyBuilder.Resolved resolved,
        @Nullable ModuleDependencyItem scalaSdkDep
    ) {
        List<ModuleDependencyItem> result = new ArrayList<>();
        if (scalaSdkDep != null) {
            result.add(scalaSdkDep);
        }
        for (DependencyLabel dep : resolved.dependencies()) {
            DependencyScope scope = dep.isRuntime() ? DependencyScope.RUNTIME : DependencyScope.COMPILE;
            // resolve to a module by exact (label, configuration), an undeclared dependency is not an imported
            // module, so it falls through to the libraries below
            String depModuleName = myCtx.getModuleNamesByKey().get(withoutAspects(dep.targetKey()));
            String asModule = depModuleName != null && myCtx.getKnownModuleNames().contains(depModuleName) && !depModuleName.equals(moduleName)
                ? depModuleName
                : null;
            if (asModule != null) {
                result.add(moduleDependency(asModule, dep.getExported(), scope));
                continue;
            }
            for (String libName : myCtx.getLibrariesByLabel().getOrDefault(dep.targetKey().label(), List.of())) {
                result.add(DependencyBuilder.toLibraryDependency(libName, dep.getExported(), scope));
            }
        }
        return result;
    }

    private static ModuleDependencyItem moduleDependency(String moduleName, boolean exported) {
        return moduleDependency(moduleName, exported, DependencyScope.COMPILE);
    }

    private static ModuleDependencyItem moduleDependency(
        String moduleName,
        boolean exported,
        DependencyScope scope
    ) {
        return new ModuleDependencyItem.ModuleDependency(
            moduleName,
            exported,
            scope,
            true
        );
    }

    // consulo: upstream DependencyScope.toEntityScope() maps the scope to the workspace model scope; the classic
    // DependencyScope is used directly

    private @Nullable String jdkNameFor(BuildTarget target) {
        JvmResolvedTarget resolved = myCtx.getJvmResolved().get(withoutAspects(target.getKey()));
        Path javaHome = resolved != null ? resolved.javaHome() : null;
        if (javaHome != null) {
            return projectNameToJdkName(myCtx.getProjectName(), javaHome);
        }
        return myCtx.getDefaultJdkName();
    }

    // consulo: applies a ModuleDependencyItem to the classic root model; libraries and modules are referenced by name
    // like the upstream LibraryId / ModuleId, so a dependency on a module written later (or a library created later,
    // e.g. the Scala SDK) resolves after the commit
    private static void addDependency(
        ModifiableRootModel model,
        JavaMutableModuleExtension<?> javaExtension,
        ModuleDependencyItem dependency,
        BazelModelBuilder storage
    ) {
        if (dependency instanceof ModuleDependencyItem.SdkDependency sdkDependency) {
            javaExtension.getInheritableSdk().set(null, sdkDependency.sdkName());
            if (model.findModuleExtensionSdkEntry(javaExtension) == null) {
                model.addModuleExtensionSdkEntry(javaExtension);
            }
        }
        else if (dependency instanceof ModuleDependencyItem.LibraryDependency libraryDependency) {
            Library library = storage.getLibraryModel().getLibraryByName(libraryDependency.library());
            LibraryOrderEntry entry = library != null
                ? model.addLibraryEntry(library)
                : model.addInvalidLibrary(libraryDependency.library(), ProjectLibraryTable.PROJECT_LEVEL);
            entry.setExported(libraryDependency.exported());
            entry.setScope(libraryDependency.scope());
        }
        else if (dependency instanceof ModuleDependencyItem.ModuleDependency moduleDependency) {
            Module dependencyModule = storage.findModule(moduleDependency.module());
            ModuleOrderEntry entry = dependencyModule != null
                ? model.addModuleOrderEntry(dependencyModule)
                : model.addInvalidModuleEntry(moduleDependency.module());
            entry.setExported(moduleDependency.exported());
            entry.setScope(moduleDependency.scope());
            entry.setProductionOnTestDependency(moduleDependency.productionOnTest());
        }
    }

    private static JavaMutableModuleExtension<?> javaExtension(ModifiableRootModel model) {
        JavaMutableModuleExtension<?> extension = model.getExtensionWithoutCheck(JavaMutableModuleExtension.class);
        if (extension == null) {
            throw new IllegalStateException("Java module extension is not available for module " + model.getModule().getName());
        }
        return extension;
    }

    private static WorkspaceTargetKey withoutAspects(WorkspaceTargetKey key) {
        // consulo: Kotlin `key.copy(aspectIds = WorkspaceAspectIds.EMPTY)`
        return new WorkspaceTargetKey(key.label(), key.configuration(), WorkspaceAspectIds.EMPTY);
    }

    // consulo: Kotlin Map.getValue
    private static <K, V> V getValue(Map<K, V> map, K key) {
        V value = map.get(key);
        if (value == null) {
            throw new NoSuchElementException("Key " + key + " is missing in the map.");
        }
        return value;
    }

    private sealed interface TargetPlan permits TargetPlan.WithoutSources, TargetPlan.Full {
        String moduleName();

        DependencyBuilder.Resolved resolvedDeps();

        @Nullable String jdkName();

        @Nullable String javaLangVersion();

        default List<SourceRootBuilder.ResolvedSourceRoot> mainSourceRoots() {
            return List.of();
        }

        record WithoutSources(
            String moduleName,
            DependencyBuilder.Resolved resolvedDeps,
            @Nullable String jdkName,
            @Nullable String javaLangVersion
        ) implements TargetPlan {
        }

        record Full(
            String moduleName,
            DependencyBuilder.Resolved resolvedDeps,
            @Nullable String jdkName,
            @Nullable String javaLangVersion,
            List<String> javacOptions,
            List<Path> jvmBinaryJars,
            @Nullable ScalaBuildTarget scalaTarget,
            @Nullable KotlinBuildTarget kotlinTarget,
            List<String> associates,
            List<SourceRootBuilder.ResolvedSourceRoot> mainSourceRoots,
            List<ResourceRootBuilder.ResolvedResourceRoot> resourceRoots,
            List<SourceRootBuilder.ResolvedSourceRoot> sourceRootsBeforeMerge
        ) implements TargetPlan {
        }
    }

    // RC: naming helpers duplicated from ModuleDetailsToJavaModuleTransformer.kt so this file is independent of MMM.
    public static String scalaVersionToScalaSdkName(String receiver) {
        return "scala-sdk-" + receiver;
    }

    private static final Set<String> JVM_SOURCE_EXTENSIONS = Set.of("java", "kt", "scala");

    public static boolean hasJvmSourceExtension(OutputLocation receiver) {
        return JVM_SOURCE_EXTENSIONS.contains(BuilderUtils.extension(OutputLocation.getRelativeNioPath(receiver)));
    }

    public static List<Path> resolvePaths(OutputLocationCollection receiver, Function<OutputLocation, @Nullable Path> resolve) {
        List<Path> result = new ArrayList<>();
        receiver.getOutputLocations().forEach(it -> {
            Path path = resolve.apply(it);
            if (path != null) {
                result.add(path);
            }
        });
        return result;
    }

    public static String projectNameToBaseJdkName(String receiver) {
        return receiver + "-jdk";
    }

    public static String projectNameToJdkName(String receiver, Path javaHomeUri) {
        return projectNameToBaseJdkName(receiver) + "-" + StringUtils.md5Hash(javaHomeUri.toString(), 5);
    }
}
