package org.jetbrains.bazel.sync.workspace.languages.java;

import com.google.devtools.build.lib.view.proto.Deps;
import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.JavaToolchainInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.JvmOutputs;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.JvmTargetInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.annotation.component.ExtensionImpl;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.io.DigestUtil;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.LanguageClassService;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.server.model.BazelSyncProject;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JavaProviderData;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JavaToolchainData;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.JdepsJar;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSyncConfig;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@ExtensionImpl(id = "JavaLanguagePlugin")
public class JavaLanguagePlugin implements LanguagePlugin {
    public JavaLanguagePlugin() {
    }

    @Override
    public Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes() {
        return Set.of(JvmBuildTarget.class, JavaProviderData.class, JavaToolchainData.class);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguages() {
        return Set.of(JavaLanguageClass.JAVA);
    }

    @Override
    public List<LanguageClass> collectUsedLanguages(TargetIdeInfo target) {
        return target.getJavaCommon().getJvmTarget() ? List.of(JavaLanguageClass.JAVA) : List.of();
    }

    @Override
    public CompletableFuture<List<WorkspaceSyncConfig>> createSyncConfigs(Project project, ProjectView projectView) {
        return CompletableFuture.completedFuture(List.of(
            new JavaWorkspaceSyncConfig(
                ProjectViewExtensions.getTestSources(projectView),
                BazelFeatureFlags.getExcludeCompiledSourceCodeInsideJars()
            )
        ));
    }

    @Override
    public CompletableFuture<List<BuildTargetData>> mapBuildTargetData(
        BazelServerFacade server,
        TargetIdeInfo target
    ) {
        // consulo: Kotlin listOfNotNull of sequential suspend calls
        List<BuildTargetData> result = new ArrayList<>();
        return createJvmBuildTargetData(server, target).thenCompose(jvmBuildTarget -> {
            if (jvmBuildTarget != null) {
                result.add(jvmBuildTarget);
            }
            return createJavaProviderData(server, target);
        }).thenCompose(javaProviderData -> {
            if (javaProviderData != null) {
                result.add(javaProviderData);
            }
            return createJavaToolchainData(server, target);
        }).thenApply(javaToolchainData -> {
            if (javaToolchainData != null) {
                result.add(javaToolchainData);
            }
            return result;
        });
    }

    private CompletableFuture<@Nullable JavaProviderData> createJavaProviderData(BazelServerFacade server, TargetIdeInfo target) {
        boolean hasProvider = target.getJavaCommon().getJvmTarget() ||
            target.getJavaProvider().getFullCompileJarsCount() > 0 ||
            target.getJavaProvider().getHasApiGeneratingPlugins();
        if (!hasProvider) {
            return CompletableFuture.completedFuture(null);
        }
        return OutputLocationCollectionBuilder.build(target.getJavaProvider().getFullCompileJarsList(), server.getOutputParser())
            .thenApply(fullCompileJars -> new JavaProviderData(
                fullCompileJars,
                target.getJavaProvider().getHasApiGeneratingPlugins()
            ));
    }

    private CompletableFuture<@Nullable JavaToolchainData> createJavaToolchainData(BazelServerFacade server, TargetIdeInfo target) {
        if (!target.hasJavaToolchainInfo()) {
            return CompletableFuture.completedFuture(null);
        }
        JavaToolchainInfo toolchain = target.getJavaToolchainInfo();
        List<String> homePaths = List.of(toolchain.getJavaHomePath(), toolchain.getBootClasspathJavaHomePath());
        // an empty path means that the aspect did not set it
        List<OutputLocation> parsedHomePaths = server.getOutputParser().parseExecrootPath(homePaths);
        List<@Nullable OutputLocation> homeLocations = new ArrayList<>();
        for (int i = 0; i < Math.min(parsedHomePaths.size(), homePaths.size()); i++) {
            homeLocations.add(homePaths.get(i).isEmpty() ? null : parsedHomePaths.get(i));
        }
        @Nullable OutputLocation javaHome = homeLocations.get(0);
        @Nullable OutputLocation bootClasspathJavaHome = homeLocations.get(1);
        return CompletableFuture.completedFuture(new JavaToolchainData(
            toolchain.getSourceVersion().isBlank() ? null : toolchain.getSourceVersion(),
            toolchain.getTargetVersion().isBlank() ? null : toolchain.getTargetVersion(),
            javaHome,
            bootClasspathJavaHome,
            toolchain.getIsExecConfig()
        ));
    }

    private CompletableFuture<@Nullable JvmBuildTarget> createJvmBuildTargetData(BazelServerFacade server, TargetIdeInfo target) {
        if (!target.getJavaCommon().getJvmTarget()) {
            return CompletableFuture.completedFuture(null);
        }
        JvmTargetInfo jvmTarget = target.getJvmTargetInfo();
        OutputLocationParser parser = server.getOutputParser();
        Map<String, String> environmentVariables = new LinkedHashMap<>(target.getEnvMap());
        for (String it : target.getEnvInheritList()) {
            // consulo: EnvironmentUtil.getValue(name) -> Platform.current().os().getEnvironmentVariable(name)
            String value = Platform.current().os().getEnvironmentVariable(it);
            environmentVariables.put(it, value != null ? value : "");
        }

        // consulo: the suspend calls of upstream run sequentially, in the same order
        CompletableFuture<List<JvmBuildTarget.JvmOutputs>> generatedJvmOutputsFuture = CompletableFuture.completedFuture(new ArrayList<>());
        for (JvmOutputs gen : target.getJavaCommon().getGeneratedJarsList()) {
            generatedJvmOutputsFuture = generatedJvmOutputsFuture.thenCompose(generatedJvmOutputs ->
                OutputLocationCollectionBuilder.build(gen.getBinaryJarsList(), parser).thenCompose(binaryJars ->
                    OutputLocationCollectionBuilder.build(gen.getInterfaceJarsList(), parser).thenCompose(interfaceJars ->
                        OutputLocationCollectionBuilder.build(gen.getSourceJarsList(), parser).thenApply(sourceJars -> {
                            generatedJvmOutputs.add(new JvmBuildTarget.JvmOutputs(binaryJars, interfaceJars, sourceJars));
                            return generatedJvmOutputs;
                        })
                    )
                )
            );
        }

        List<ArtifactLocation> binaryJars = new ArrayList<>();
        List<ArtifactLocation> interfaceJars = new ArrayList<>();
        List<ArtifactLocation> sourceJars = new ArrayList<>();
        for (JvmOutputs it : target.getJavaCommon().getJarsList()) {
            binaryJars.addAll(it.getBinaryJarsList());
            interfaceJars.addAll(it.getInterfaceJarsList());
            sourceJars.addAll(it.getSourceJarsList());
        }

        return generatedJvmOutputsFuture.thenCompose(generatedJvmOutputs ->
            OutputLocationCollectionBuilder.build(binaryJars, parser).thenCompose(binaryOutputs ->
                OutputLocationCollectionBuilder.build(interfaceJars, parser).thenCompose(outputInterfaceJars ->
                    OutputLocationCollectionBuilder.build(sourceJars, parser).thenCompose(outputSourceJars ->
                        createJdepsJars(server, target).thenCompose(jdepsJars ->
                            OutputLocationCollectionBuilder.build(getIntellijPluginJars(target), parser).thenApply(intellijPluginJars ->
                                new JvmBuildTarget(
                                    StringsCollection.of(target.getJavaCommon().getJavacOptsList()),
                                    binaryOutputs,
                                    new LinkedHashMap<>(environmentVariables),
                                    getMainClass(jvmTarget),
                                    StringsCollection.of(jvmTarget.getJvmFlagsList()),
                                    StringsCollection.of(jvmTarget.getArgsList()),
                                    resourceStripPrefixLocation(target),
                                    outputInterfaceJars,
                                    outputSourceJars,
                                    generatedJvmOutputs,
                                    jdepsJars,
                                    intellijPluginJars,
                                    target.hasExecutableInfo(),
                                    targetChecksStrictDeps(target)
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    private StrictDependencyCheckedType targetChecksStrictDeps(TargetIdeInfo target) {
        // Special case, to be dropped shortly
        // Ultimate monorepo rules do not support strict deps
        if (target.getKind().equals("jvm_library") || target.getKind().equals("_jvm_library_jps")) {
            return StrictDependencyCheckedType.OFF;
        }

        // At the moment only java supports strict deps
        // https://blog.bazel.build/2017/06/28/sjd-unused_deps.html
        // TODO: support Kotlin strict deps
        // TODO: investigate scala
        boolean hasJavaSources = BazelSyncProject.getSourcesList(target).anyMatch(it ->
            JavaLanguageClass.JAVA.equals(LanguageClassService.getInstance().fromPath(it.getRelativePath()))
        );
        if (!hasJavaSources) {
            return StrictDependencyCheckedType.OFF;
        }

        if (target.hasKotlinTargetInfo() || target.hasScalaTargetInfo()) {
            return StrictDependencyCheckedType.WARNING;
        }

        return StrictDependencyCheckedType.ERROR;
    }

    private @Nullable String getMainClass(JvmTargetInfo jvmTargetInfo) {
        return jvmTargetInfo.getMainClass().isBlank() ? null : jvmTargetInfo.getMainClass();
    }

    private CompletableFuture<List<JdepsJar>> createJdepsJars(
        BazelServerFacade server,
        TargetIdeInfo targetInfo
    ) {
        // the absolute path checks the file and gives the synthetic label, the execroot path gives the location
        List<String> execrootPaths = new ArrayList<>();
        List<Path> paths = new ArrayList<>();
        for (String it : dependencyJarsFromJdepsFiles(server, targetInfo)) {
            execrootPaths.add(it);
            paths.add(server.getBazelPathsResolver().resolveOutput(Paths.get(it)));
        }
        List<OutputLocation> locations = server.getOutputParser().parseExecrootPath(execrootPaths);
        List<JdepsJar> result = new ArrayList<>();
        for (int i = 0; i < Math.min(paths.size(), locations.size()); i++) {
            result.add(new JdepsJar(syntheticLabel(server, paths.get(i)), locations.get(i)));
        }
        return CompletableFuture.completedFuture(result);
    }

    // returns the execroot paths of the jars
    private Set<String> dependencyJarsFromJdepsFiles(BazelServerFacade server, TargetIdeInfo targetInfo) {
        Set<String> result = new LinkedHashSet<>();
        for (ArtifactLocation jdeps : targetInfo.getJavaCommon().getJdepsList()) {
            // a jdeps file is generated, so the local override does not change its path
            Path path = server.getBazelPathsResolver().resolve(jdeps, RepoMapping.NoLocalRepositories);
            if (Files.exists(path)) {
                List<Deps.Dependency> dependencyList;
                try (InputStream it = Files.newInputStream(path)) {
                    dependencyList = Deps.Dependencies.parseFrom(it).getDependencyList();
                }
                catch (IOException e) {
                    // consulo: Kotlin has no checked exceptions
                    throw new UncheckedIOException(e);
                }
                for (Deps.Dependency it : dependencyList) {
                    if (isRelevant(it)) {
                        result.add(it.getPath());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Similar to what was done in the Google's Bazel plugin in JdepsFileReader#relevantDep,
     * we should only include deps that are actually used by the compiler
     */
    private static boolean isRelevant(Deps.Dependency receiver) {
        return receiver.getKind() == Deps.Dependency.Kind.EXPLICIT || receiver.getKind() == Deps.Dependency.Kind.IMPLICIT;
    }

    private final Pattern myReplacementRegex = Pattern.compile("[^0-9a-zA-Z]");

    private Label syntheticLabel(BazelServerFacade server, Path lib) {
        Path relativeLibPath = relativeToOrSelf(lib, server.getBazelPathsResolver().bazelBin());
        // just in case of a conflict in filename
        String shaOfPath = sha1Hex(relativeLibPath.toString()).substring(0, 7);
        return Label.synthetic(
            myReplacementRegex.matcher(
                Objects.requireNonNull(lib
                    .getFileName())
                    .toString())
                .replaceAll("-") + "-" + shaOfPath
        );
    }

    // consulo: IntelliJ DigestUtil.sha1Hex(String)
    private static String sha1Hex(String input) {
        return HexFormat.of().formatHex(DigestUtil.sha1().digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    // consulo: Kotlin Path.relativeToOrSelf
    private static Path relativeToOrSelf(Path receiver, Path base) {
        try {
            return base.normalize().relativize(receiver.normalize());
        }
        catch (IllegalArgumentException e) {
            return receiver;
        }
    }

    private List<ArtifactLocation> getIntellijPluginJars(TargetIdeInfo targetInfo) {
        // _repackaged_files is created upon calling repackaged_files in rules_intellij
        if (!targetInfo.getKind().equals("_repackaged_files")) {
            return List.of();
        }
        return BazelSyncProject.getGeneratedSourcesList(targetInfo)
            .filter(it -> it.getRelativePath().endsWith(".jar"))
            .toList();
    }

    // a prefix in an external repository stays external, so the local override resolves it to the local checkout at import
    private static @Nullable OutputLocation resourceStripPrefixLocation(TargetIdeInfo receiver) {
        if (!receiver.hasJvmTargetInfo()) {
            return null;
        }
        String prefix = receiver.getJvmTargetInfo().getResourceStripPrefix();
        if (prefix.isEmpty()) {
            return null;
        }
        Label label = Label.label(receiver);
        if (label instanceof ResolvedLabel resolvedLabel && !resolvedLabel.isMainWorkspace()) {
            return new OutputLocation.External(resolvedLabel.getRepoName(), prefix);
        }
        return new OutputLocation.Workspace(prefix);
    }
}
