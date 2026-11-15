package org.jetbrains.bazel.sync.workspace.languages.kotlin;

import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelper;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl(id = "KotlinLanguagePlugin")
public class KotlinLanguagePlugin implements LanguagePlugin {
    public KotlinLanguagePlugin() {
    }

    @Override
    public Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes() {
        return Set.of(KotlinBuildTarget.class);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguages() {
        return Set.of(JavaLanguageClass.KOTLIN);
    }

    @Override
    public List<LanguageClass> collectUsedLanguages(TargetIdeInfo target) {
        if (target.hasKotlinTargetInfo()) {
            return List.of(JavaLanguageClass.KOTLIN);
        }
        return List.of();
    }


    @Override
    public CompletableFuture<List<BuildTargetData>> mapBuildTargetData(
        BazelServerFacade server,
        TargetIdeInfo target
    ) {
        if (!target.hasKotlinTargetInfo()) {
            return CompletableFuture.completedFuture(List.of());
        }
        IntellijIdeInfo.KotlinTargetInfo kotlinTarget = target.getKotlinTargetInfo();
        // the paths below are generated jars, existence checks or compiler arguments, so the local override does not matter

        List<ArtifactLocation> ktStdlibJars = new ArrayList<>();
        List<ArtifactLocation> ktStdlibSources = new ArrayList<>();
        for (IntellijIdeInfo.JvmOutputs output : kotlinTarget.getStdlibJarsList()) {
            List<ArtifactLocation> jars = output.getBinaryJarsList();
            List<ArtifactLocation> sources;
            if (!output.getSourceJarsList().isEmpty()) {
                sources = output.getSourceJarsList();
            }
            else {
                // fallback to hack, awaits proper fix in rules_kotlin
                // https://github.com/bazel-contrib/rules_kotlin/pull/1761
                sources = jars.stream()
                    .map(KotlinLanguagePlugin::toSourcesJar)
                    .filter(it -> Files.exists(server.getBazelPathsResolver().resolve(it, RepoMapping.NoLocalRepositories)))
                    .toList();
            }

            ktStdlibJars.addAll(jars);
            ktStdlibSources.addAll(sources);
        }

        List<ArtifactLocation> kspSourceJars;
        if (target.hasJvmTargetInfo()) {
            // consulo: upstream shadows `target` with the java common info
            IntellijIdeInfo.JavaCommonInfo javaCommon = target.getJavaCommon();
            kspSourceJars = javaCommon.getGeneratedJarsList().stream()
                .flatMap(it -> it.getSourceJarsList().stream())
                .filter(it -> isKspSourceJar(it) &&
                    !server.getBazelPathsResolver().resolve(it, RepoMapping.NoLocalRepositories)
                        .startsWith(server.getBazelInfo().workspaceRoot()))
                .toList();
        }
        else {
            kspSourceJars = List.of();
        }
        @Nullable String languageVersion = kotlinTarget.getLanguageVersion().isBlank() ? null : kotlinTarget.getLanguageVersion();
        @Nullable String apiVersion = kotlinTarget.getApiVersion().isBlank() ? null : kotlinTarget.getApiVersion();
        @Nullable String moduleName = kotlinTarget.getModuleName().isBlank() ? null : kotlinTarget.getModuleName();
        StringsCollection kotlincOptions =
            StringsCollection.of(toKotlincOptArguments(kotlinTarget, server, RepoMapping.NoLocalRepositories));
        // consulo: the suspend calls of upstream run sequentially, in the same order
        return OutputLocationCollectionBuilder.build(ktStdlibJars, server.getOutputParser()).thenCompose(stdlibJars ->
            OutputLocationCollectionBuilder.build(ktStdlibSources, server.getOutputParser()).thenCompose(stdlibInferredSourceJars ->
                OutputLocationCollectionBuilder.build(kspSourceJars, server.getOutputParser()).thenApply(kspSourceJarsCollection ->
                    List.<BuildTargetData>of(
                        new KotlinBuildTarget(
                            languageVersion,
                            apiVersion,
                            kotlincOptions,
                            kotlinTarget.getAssociatedTargetsList().stream().map(WorkspaceHelper::toWorkspaceTargetKey).toList(),
                            moduleName,
                            stdlibJars,
                            stdlibInferredSourceJars,
                            kotlinTarget.getExportedCompilerPluginTargetsList().stream().map(WorkspaceHelper::toWorkspaceTargetKey).toList(),
                            kspSourceJarsCollection
                        )
                    )
                )
            )
        );
    }

    private static List<String> toKotlincOptArguments(
        IntellijIdeInfo.KotlinTargetInfo receiver,
        BazelServerFacade server,
        LocalRepositoryMapping localRepositories
    ) {
        List<String> result = new ArrayList<>(receiver.getKotlincOptsList());
        result.addAll(additionalKotlinOpts(receiver, server, localRepositories));
        return result;
    }

    private static List<String> additionalKotlinOpts(
        IntellijIdeInfo.KotlinTargetInfo receiver,
        BazelServerFacade server,
        LocalRepositoryMapping localRepositories
    ) {
        List<String> result = new ArrayList<>(toKotlincPluginClasspathArguments(receiver, server, localRepositories));
        result.addAll(toKotlincPluginOptionArguments(receiver));
        return result;
    }

    private static List<String> toKotlincPluginOptionArguments(IntellijIdeInfo.KotlinTargetInfo receiver) {
        return receiver.getKotlincPluginInfosList().stream()
            .flatMap(it -> it.getKotlincPluginOptionsList().stream())
            .flatMap(it -> List.of("-P", "plugin:" + it.getPluginId() + ":" + it.getOptionValue()).stream())
            .toList();
    }

    private static List<String> toKotlincPluginClasspathArguments(
        IntellijIdeInfo.KotlinTargetInfo receiver,
        BazelServerFacade server,
        LocalRepositoryMapping localRepositories
    ) {
        return receiver.getKotlincPluginInfosList().stream()
            .flatMap(it -> it.getPluginJarsList().stream())
            .map(it -> "-Xplugin=" + server.getBazelPathsResolver().resolve(it, localRepositories))
            .toList();
    }

    // KSP inside rules_kotlin is special, rules_kotlin/intellij-aspect doesn't distinguish between
    // normal compiler outputs and KSP ones, that's why we have to use this heuristic.
    // Ideally `KtJvmInfo` provider should expose something like ksp_srcjars for plugin.
    private static boolean isKspSourceJar(ArtifactLocation receiver) {
        return receiver.getRelativePath().endsWith("ksp-gensrc.jar");
    }

    // the `-sources.jar` sibling of a jar
    private static ArtifactLocation toSourcesJar(ArtifactLocation receiver) {
        String relativePath = receiver.getRelativePath();
        int lastSlash = relativePath.lastIndexOf('/');
        String directory = lastSlash < 0 ? "" : relativePath.substring(0, lastSlash);
        String fileName = relativePath.substring(lastSlash + 1).replace(".jar", "-sources.jar");
        return receiver.toBuilder().setRelativePath(directory.isEmpty() ? fileName : directory + "/" + fileName).build();
    }
}
