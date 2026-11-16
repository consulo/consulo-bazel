/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.dotnet.workspace;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.bazel.dotnet.localize.BazelDotNetLocalize;
import consulo.bazel.dotnet.module.extension.BazelCSharpMutableModuleExtension;
import consulo.bazel.dotnet.module.extension.BazelDotNetCoreMutableModuleExtension;
import consulo.bazel.dotnet.workspace.BazelDotNetModuleConfigurator;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.content.library.Library;
import consulo.csharp.module.CSharpNullableOption;
import consulo.csharp.module.extension.CSharpLanguageVersion;
import consulo.dotnet.DotNetTarget;
import consulo.dotnet.core.bundle.DotNetCoreBundleType;
import consulo.dotnet.dll.DotNetModuleFileType;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.dotnet.sync.DotNetBuildTarget;
import org.jetbrains.bazel.dotnet.sync.DotNetFrameworkDefines;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.GlobalNamingContextBuilder;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameProducer;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameSpace;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.extensions.BazelModuleExtensionEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetLabelList;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetSourceRootTypeId;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.utils.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Imports the rules_dotnet C# targets: one module per target with the Bazel .NET and C# module extensions
 * ({@code bazel-dotnet-core}, {@code bazel-csharp}), the hermetic .NET SDK of the target as a {@link DotNetCoreBundleType}
 * bundle, the reference assemblies of the targeting pack and of NuGet packages as libraries, and the C# targets the target
 * depends on as module dependencies.
 * <p>
 * consulo: Consulo-only, upstream has no .NET support. The modules are set up like the MSBuild projects of consulo-msbuild
 * ({@code BaseDotNetProjectCapability}, {@code BaseCSharpProjectCapability}).
 */
public class DotNetBazelWorkspaceImporter implements BazelWorkspaceImporter {
    private static final Logger LOG = Logger.getInstance(DotNetBazelWorkspaceImporter.class);

    private static final NameProducer NAME_PRODUCER = new NameProducer("dotnet");

    // the ids of the Consulo ContentFolderTypeProviders, which WorkspaceModelTargetSourceRootTypeId stores (like the Python importer)
    private static final String PRODUCTION_ROOT_TYPE = "PRODUCTION";
    private static final String TEST_ROOT_TYPE = "TEST";

    private static final String LIBRARY_PREFIX = ".NET: ";

    private final WorkspaceImporterContext myContext;

    private Map<WorkspaceTargetKey, BuildTarget> myTargets = Map.of();
    private Map<Path, Sdk> mySdksByDotNet = Map.of();
    private @Nullable Sdk myFallbackSdk;

    public DotNetBazelWorkspaceImporter(WorkspaceImporterContext context) {
        myContext = context;
    }

    @Override
    public LocalizeValue getImporterName() {
        return BazelDotNetLocalize.consoleTaskModelDotnetImporter();
    }

    @Override
    public CompletableFuture<WorkspaceImporterResult> doImport(WorkspaceImporterPhase phase, WorkspaceSnapshot snapshot, TaskId taskId) {
        // an exception of a failed phase -> a failed future, like the other importers
        try {
            if (phase instanceof WorkspaceImporterPhase.Initialize initialize) {
                return onInitialize(snapshot, initialize.naming(), taskId);
            }
            else if (phase instanceof WorkspaceImporterPhase.WorkspaceApply apply) {
                onWorkspaceApply(snapshot, apply.builder(), apply.naming());
            }
            return CompletableFuture.completedFuture(WorkspaceImporterResult.Success);
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<WorkspaceImporterResult> onInitialize(WorkspaceSnapshot snapshot, GlobalNamingContextBuilder naming, TaskId taskId) {
        int importDepth = WorkspaceHelpers.getCommonSyncConfig(snapshot).importDepth();
        Map<WorkspaceTargetKey, BuildTarget> targets = new LinkedHashMap<>();
        WorkspaceHelpers.filterBuildTarget(
                snapshot.targetGraph().findAllTargetsAtDepth(importDepth, importDepth < 0, true)
                    .map(it -> snapshot.targets().findTargetByKey(it))
                    .filter(Objects::nonNull),
                DotNetBuildTarget.class
            )
            .filter(it -> it.getSecond().isCSharp())
            .forEach(it -> targets.putIfAbsent(it.getFirst().getKey(), it.getFirst()));
        myTargets = targets;
        if (myTargets.isEmpty()) {
            return CompletableFuture.completedFuture(WorkspaceImporterResult.Abort);
        }

        for (WorkspaceTargetKey key : myTargets.keySet()) {
            naming.declare(NameSpace.MODULE, NAME_PRODUCER, key);
        }

        // the SDKs are registered before the root models are filled: the .NET module extension references its SDK
        Project project = myContext.project();
        return util.withSubtask(
            util.getSyncConsole(project),
            myContext.progressReporter(),
            taskId.subTask("calculate-dotnet-sdk"),
            BazelDotNetLocalize.consoleTaskModelCalculateDotnetSdk(),
            subtaskId -> createSdks(project)
        ).thenApply(ignored -> WorkspaceImporterResult.Success);
    }

    // ---- SDK ----

    private CompletableFuture<Boolean> createSdks(Project project) {
        OutputLocationResolver resolver = DefaultOutputLocationResolver.createExecrootResolving(myContext.bazelInfo());
        // dotnet executable -> SDK home (sdk/<version> of its installation)
        Map<Path, Path> homes = new LinkedHashMap<>();
        for (BuildTarget target : myTargets.values()) {
            DotNetBuildTarget dotNetTarget = Objects.requireNonNull(DotNetBuildTarget.extractDotNetBuildTarget(target));
            OutputLocation dotnet = dotNetTarget.dotnet();
            Path dotnetPath = dotnet == null ? null : resolver.resolve(dotnet);
            if (dotnetPath == null || homes.containsKey(dotnetPath)) {
                continue;
            }
            Path home = findSdkHome(dotnetPath);
            if (home != null) {
                homes.put(dotnetPath, home);
            }
            else {
                LOG.warn("No .NET SDK next to " + dotnetPath);
            }
        }

        return project.getUIAccess().giveAsync(() -> WriteAction.compute(() -> {
            Map<Path, Sdk> sdks = new LinkedHashMap<>();
            for (Map.Entry<Path, Path> entry : homes.entrySet()) {
                Sdk sdk = findOrCreateSdk(entry.getValue());
                if (sdk != null) {
                    sdks.put(entry.getKey(), sdk);
                }
            }
            mySdksByDotNet = sdks;
            // a target without a hermetic SDK (the aspect found no compile action) gets any SDK
            Sdk fallback = sdks.isEmpty() ? null : sdks.values().iterator().next();
            if (fallback == null) {
                List<Sdk> existing = SdkTable.getInstance().getSdksOfType(DotNetCoreBundleType.getInstance());
                fallback = existing.isEmpty() ? null : existing.get(0);
            }
            myFallbackSdk = fallback;
            return Boolean.TRUE;
        }));
    }

    /**
     * The SDK of a {@code dotnet} executable: the {@code sdk/<version>} directory of its installation (rules_dotnet downloads one
     * SDK per toolchain), the highest valid one if there are several.
     */
    private static @Nullable Path findSdkHome(Path dotnet) {
        Path installation = dotnet.getParent();
        if (installation == null) {
            return null;
        }
        DotNetCoreBundleType type = DotNetCoreBundleType.getInstance();
        Platform platform = Platform.current();
        try (Stream<Path> children = Files.list(installation.resolve("sdk"))) {
            return children
                .filter(Files::isDirectory)
                .filter(it -> type.isValidSdkHome(platform, it))
                .max(Comparator.comparing(it -> String.valueOf(it.getFileName())))
                .orElse(null);
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     * An SDK of the SDK table with this home, or a new one named after the version and the home (the hermetic SDKs of several
     * Bazel workspaces can have the same version).
     */
    private static @Nullable Sdk findOrCreateSdk(Path home) {
        DotNetCoreBundleType type = DotNetCoreBundleType.getInstance();
        SdkTable sdkTable = SdkTable.getInstance();
        Path normalizedHome = home.toAbsolutePath().normalize();
        for (Sdk sdk : sdkTable.getSdksOfType(type)) {
            String homePath = sdk.getHomePath();
            if (homePath != null && Path.of(homePath).toAbsolutePath().normalize().equals(normalizedHome)) {
                return sdk;
            }
        }
        String version = type.getVersionString(Platform.current(), normalizedHome);
        String name = "Bazel .NET " + (version != null ? version : String.valueOf(normalizedHome.getFileName())) + " ("
            + StringUtils.md5Hash(normalizedHome.toString(), 5) + ")";
        Sdk existing = sdkTable.findSdk(name);
        if (existing != null) {
            return existing;
        }
        try {
            Sdk sdk = sdkTable.createSdk(normalizedHome, name, type);
            type.setupSdkPaths(sdk);
            sdkTable.addSdk(sdk);
            return sdk;
        }
        catch (Throwable e) {
            LOG.warn("Cannot create the .NET SDK " + normalizedHome, e);
            return null;
        }
    }

    private @Nullable Sdk sdkOf(DotNetBuildTarget target, OutputLocationResolver resolver) {
        OutputLocation dotnet = target.dotnet();
        Path dotnetPath = dotnet == null ? null : resolver.resolve(dotnet);
        Sdk sdk = dotnetPath == null ? null : mySdksByDotNet.get(dotnetPath);
        return sdk != null ? sdk : myFallbackSdk;
    }

    // ---- modules ----

    private void onWorkspaceApply(WorkspaceSnapshot snapshot, BazelModelBuilder builder, GlobalNamingContext naming) {
        OutputLocationResolver resolver = DefaultOutputLocationResolver.createExecrootResolving(myContext.bazelInfo());
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        BazelPathsResolver pathsResolver = new BazelPathsResolver(myContext.bazelInfo());

        // several targets in one Bazel package: the package directory can be the content root of one module only, so each
        // module gets its source files as content roots (like the Python importer); a package with one target is its content root
        Map<WorkspaceTargetKey, Path> packageDirectories = new LinkedHashMap<>();
        Map<Path, Integer> targetsByDirectory = new LinkedHashMap<>();
        for (WorkspaceTargetKey key : myTargets.keySet()) {
            Path directory = pathsResolver.toDirectoryPath(Label.assumeResolved(key.label()), snapshot.repoMapping()).normalize();
            packageDirectories.put(key, directory);
            targetsByDirectory.merge(directory, 1, Integer::sum);
        }

        Map<String, Library> libraries = new LinkedHashMap<>();
        // consulo: the classic modifiable models read the module table (addModuleOrderEntry), which needs read access on this
        //   pool thread, like the Python importer
        ReadAction.run(() -> {
            for (Map.Entry<WorkspaceTargetKey, BuildTarget> entry : myTargets.entrySet()) {
                WorkspaceTargetKey key = entry.getKey();
                BuildTarget target = entry.getValue();
                DotNetBuildTarget dotNetTarget = Objects.requireNonNull(DotNetBuildTarget.extractDotNetBuildTarget(target));
                Path directory = Objects.requireNonNull(packageDirectories.get(key));
                boolean ownsDirectory = targetsByDirectory.getOrDefault(directory, 0) == 1;
                addModule(
                    snapshot,
                    builder,
                    naming,
                    target,
                    dotNetTarget,
                    ownsDirectory ? directory : null,
                    resolver,
                    localRepositories,
                    libraries
                );
            }
        });
    }

    private void addModule(
        WorkspaceSnapshot snapshot,
        BazelModelBuilder builder,
        GlobalNamingContext naming,
        BuildTarget target,
        DotNetBuildTarget dotNetTarget,
        @Nullable Path packageDirectory,
        OutputLocationResolver resolver,
        LocalRepositoryMapping localRepositories,
        Map<String, Library> libraries
    ) {
        String moduleName = naming.findOrFallback(NameSpace.MODULE, NAME_PRODUCER, target.getKey());
        Module module = builder.getOrCreateModule(moduleName, null);
        ModifiableRootModel model = builder.getRootModel(module);
        boolean isTest = target.getKind().ruleType() == RuleType.TEST;

        // the parent extension first: a child extension can only be enabled with its parent
        BazelDotNetCoreMutableModuleExtension dotNetExtension = model.getExtensionWithoutCheck(BazelDotNetCoreMutableModuleExtension.class);
        if (dotNetExtension != null) {
            setupDotNetExtension(dotNetExtension, dotNetTarget, resolver);
        }
        BazelCSharpMutableModuleExtension cSharpExtension = model.getExtensionWithoutCheck(BazelCSharpMutableModuleExtension.class);
        if (cSharpExtension != null) {
            setupCSharpExtension(cSharpExtension, dotNetTarget);
        }

        ContentFolderTypeProvider folderType = isTest
            ? TestContentFolderTypeProvider.getInstance()
            : ProductionContentFolderTypeProvider.getInstance();
        if (packageDirectory != null) {
            String url = VirtualFileUtil.pathToUrl(packageDirectory.toString());
            model.addContentEntry(url).addFolder(url, folderType);
        }
        else {
            for (Path source : workspaceSources(dotNetTarget, snapshot.repoMapping(), resolver, localRepositories)) {
                String url = VirtualFileUtil.pathToUrl(source.toString());
                ContentEntry contentEntry = model.addContentEntry(url);
                contentEntry.addFolder(url, folderType);
            }
        }

        // consulo: the module source dependency is implicit (the first order entry of a Consulo root model)
        for (DependencyLabel dependency : target.getDependencies()) {
            if (dependency.isRuntime() || !myTargets.containsKey(dependency.targetKey())) {
                continue;
            }
            String dependencyName = naming.findOrFallback(NameSpace.MODULE, NAME_PRODUCER, dependency.targetKey());
            // modules are referenced by name, a module written later resolves after the commit
            Module dependencyModule = builder.findModule(dependencyName);
            ModuleOrderEntry orderEntry = dependencyModule != null
                ? model.addModuleOrderEntry(dependencyModule)
                : model.addInvalidModuleEntry(dependencyName);
            orderEntry.setExported(dependency.getExported());
            orderEntry.setScope(DependencyScope.COMPILE);
        }

        // System types resolve only through the reference assemblies of the targeting pack
        for (Library library : referenceLibraries(builder, dotNetTarget.frameworkReferences(), resolver, localRepositories, libraries)) {
            LibraryOrderEntry orderEntry = model.addLibraryEntry(library);
            orderEntry.setScope(DependencyScope.COMPILE);
        }
        for (Library library : referenceLibraries(builder, dotNetTarget.packageReferences(), resolver, localRepositories, libraries)) {
            LibraryOrderEntry orderEntry = model.addLibraryEntry(library);
            orderEntry.setScope(DependencyScope.COMPILE);
        }

        // consulo: other libraries (e.g. consulo.bazel.dotnet.nunit) enable their extensions, like test frameworks
        Application.get().getExtensionPoint(BazelDotNetModuleConfigurator.class)
            .forEachExtensionSafe(it -> it.configure(model, target, dotNetTarget));

        builder.putModuleData(
            module,
            BazelModuleExtensionEntity.class,
            new BazelModuleExtensionEntity(
                WorkspaceModelTargetKey.of(target.getKey()),
                new WorkspaceModelTargetSourceRootTypeId(isTest ? TEST_ROOT_TYPE : PRODUCTION_ROOT_TYPE),
                new WorkspaceModelTargetLabelList(StrictDependencyCheckedType.OFF, List.of())
            )
        );
    }

    /**
     * consulo-msbuild's {@code BaseDotNetProjectCapability}: SDK, target, debug info, source roots, the built assembly and the
     * preprocessor symbols.
     */
    private void setupDotNetExtension(
        BazelDotNetCoreMutableModuleExtension extension,
        DotNetBuildTarget target,
        OutputLocationResolver resolver
    ) {
        extension.setEnabled(true);

        Sdk sdk = sdkOf(target, resolver);
        if (sdk != null) {
            // the .NET extensions have no SDK order entry (not a ModuleExtensionWithSdk), the SDK only runs the assemblies
            extension.getInheritableSdk().set(null, sdk);
        }

        extension.setTarget(target.executable() ? DotNetTarget.EXECUTABLE : DotNetTarget.LIBRARY);
        extension.setAllowDebugInfo(true);
        extension.setAllowSourceRoots(true);

        // the assembly in bazel-out (of the configuration of the sync), which the .NET run configuration runs
        OutputLocation assembly = target.assembly();
        Path assemblyPath = assembly == null ? null : resolver.resolve(assembly);
        Path outputDirectory = assemblyPath == null ? null : assemblyPath.getParent();
        Path assemblyFileName = assemblyPath == null ? null : assemblyPath.getFileName();
        if (outputDirectory != null && assemblyFileName != null) {
            extension.setOutputDir(outputDirectory.toString());
            extension.setFileName(assemblyFileName.toString());
        }
        else {
            extension.setFileName(target.assemblyName() + ".dll");
        }

        List<String> variables = extension.getVariables();
        variables.clear();
        variables.addAll(DotNetFrameworkDefines.compilerDefines(target));
    }

    // consulo-msbuild's BaseCSharpProjectCapability: language version, nullable context, unsafe code
    private static void setupCSharpExtension(BazelCSharpMutableModuleExtension extension, DotNetBuildTarget target) {
        extension.setEnabled(true);
        extension.setLanguageVersion(toLanguageVersion(target.languageVersion()));
        extension.setNullableOption(toNullableOption(target.nullable()));
        extension.setAllowUnsafeCode(target.allowUnsafeBlocks());
        extension.setInternalsVisibleTo(target.internalsVisibleTo());
    }

    /**
     * The {@code langversion} of the target ({@code 12.0}, {@code 7.3}, {@code 9}, {@code ISO-2}, {@code latest}, ...); the
     * default, {@code latest} and {@code preview} are the highest version Consulo knows.
     */
    static CSharpLanguageVersion toLanguageVersion(String langVersion) {
        String value = langVersion.trim().toUpperCase(Locale.ROOT);
        if (value.startsWith("ISO-")) {
            value = value.substring("ISO-".length());
        }
        if (value.isEmpty() || !Character.isDigit(value.charAt(0))) {
            return CSharpLanguageVersion.HIGHEST;
        }
        String name = "_" + (value.contains(".") ? value.replace('.', '_') : value + "_0");
        for (CSharpLanguageVersion version : CSharpLanguageVersion.values()) {
            if (version.name().equals(name)) {
                return version;
            }
        }
        return CSharpLanguageVersion.HIGHEST;
    }

    // rules_dotnet passes /nullable:disable for anything but enable, warnings and annotations (the attribute default is disable)
    static CSharpNullableOption toNullableOption(String nullable) {
        return switch (nullable) {
            case "enable" -> CSharpNullableOption.ENABLE;
            case "warnings" -> CSharpNullableOption.WARNINGS;
            case "annotations" -> CSharpNullableOption.ANNOTATIONS;
            default -> CSharpNullableOption.DISABLE;
        };
    }

    // the sources of the target in the workspace or a local repository; external ones (the NUnit shim of rules_dotnet) are skipped
    private static List<Path> workspaceSources(
        DotNetBuildTarget target,
        RepoMapping repoMapping,
        OutputLocationResolver resolver,
        LocalRepositoryMapping localRepositories
    ) {
        Set<Path> result = new LinkedHashSet<>();
        for (OutputLocation source : target.sources()) {
            if (!OutputLocation.isSource(source) || !OutputLocation.isUserCode(source, repoMapping)) {
                continue;
            }
            Path path = resolver.resolve(source, localRepositories);
            if (path != null) {
                result.add(path.normalize());
            }
        }
        return new ArrayList<>(result);
    }

    // ---- libraries ----

    /**
     * One project library per directory of reference assemblies (the {@code ref/<tfm>} directory of a targeting pack, the
     * {@code lib/<tfm>} directory of a NuGet package), with {@code netdll} roots and the XML documentation next to an assembly.
     */
    private static List<Library> referenceLibraries(
        BazelModelBuilder builder,
        List<OutputLocation> references,
        OutputLocationResolver resolver,
        LocalRepositoryMapping localRepositories,
        Map<String, Library> libraries
    ) {
        Map<String, List<Path>> byLibrary = new LinkedHashMap<>();
        for (OutputLocation reference : references) {
            Path path = resolver.resolve(reference, localRepositories);
            if (path == null) {
                continue;
            }
            byLibrary.computeIfAbsent(libraryName(reference, path), it -> new ArrayList<>()).add(path.normalize());
        }

        List<Library> result = new ArrayList<>();
        for (Map.Entry<String, List<Path>> entry : byLibrary.entrySet()) {
            String name = entry.getKey();
            Library library = libraries.get(name);
            boolean created = library == null;
            if (library == null) {
                library = builder.getOrCreateLibrary(name);
                libraries.put(name, library);
            }
            Library.ModifiableModel libraryModel = builder.getLibraryModifiableModel(library);
            Set<String> existing = created ? Set.of() : Set.of(libraryModel.getUrls(BinariesOrderRootType.ID));
            for (Path dll : entry.getValue()) {
                String url = ArchiveVfsUtil.getArchiveRootUrl(DotNetModuleFileType.PROTOCOL, dll);
                if (url == null || existing.contains(url)) {
                    continue;
                }
                libraryModel.addRoot(url, BinariesOrderRootType.ID);
                Path documentation = documentationOf(dll);
                if (documentation != null) {
                    libraryModel.addRoot(VirtualFileUtil.pathToUrl(documentation.toString()), DocumentationOrderRootType.ID);
                }
            }
            result.add(library);
        }
        return result;
    }

    // e.g. ".NET: nuget.microsoft.netcore.app.ref.v8.0.30/ref/net8.0" for a reference of rules_dotnet's targeting pack repository
    private static String libraryName(OutputLocation reference, Path path) {
        String repository;
        if (reference instanceof OutputLocation.External external) {
            String repoName = external.repoName();
            int plus = repoName.lastIndexOf('+');
            repository = plus >= 0 && plus < repoName.length() - 1 ? repoName.substring(plus + 1) : repoName;
        }
        else {
            repository = "";
        }
        Path relative = OutputLocation.toRepoRelativePath(reference);
        Path directory = relative != null ? relative.getParent() : path.getParent();
        String directoryName = directory == null ? "" : directory.toString().replace('\\', '/');
        String name = repository.isEmpty() ? directoryName : directoryName.isEmpty() ? repository : repository + "/" + directoryName;
        return LIBRARY_PREFIX + name;
    }

    private static @Nullable Path documentationOf(Path dll) {
        Path fileName = dll.getFileName();
        Path directory = dll.getParent();
        if (fileName == null || directory == null) {
            return null;
        }
        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        Path documentation = directory.resolve((dot > 0 ? name.substring(0, dot) : name) + ".xml");
        return Files.isRegularFile(documentation) ? documentation : null;
    }
}
