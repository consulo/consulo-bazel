package org.jetbrains.bazel.sync.workspace.languages.jvm;

import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.StringsCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

// consulo: the other top-level declarations of upstream JvmBuildTarget.kt are nested here
public record JvmBuildTarget(
    StringsCollection javacOpts,
    OutputLocationCollection binaryOutputs,

    Map<String, String> environmentVariables,
    @Nullable String mainClass,
    @Nullable String testClass,
    StringsCollection jvmArgs,
    StringsCollection programArgs,
    @Nullable OutputLocation resolvedResourceStripPrefix,
    OutputLocationCollection outputInterfaceJars,
    OutputLocationCollection outputSourceJars,
    List<JvmOutputs> generatedJars,
    List<JdepsJar> jdepsJars,
    OutputLocationCollection intellijPluginJars,
    boolean hasExecutableInfo,
    StrictDependencyCheckedType checkStrictDependencies
) implements BuildTargetData {
    // consulo: upstream default values of all parameters
    public JvmBuildTarget() {
        this(
            StringsCollection.EMPTY,
            OutputLocationCollection.EMPTY,
            Map.of(),
            null,
            null,
            StringsCollection.EMPTY,
            StringsCollection.EMPTY,
            null,
            OutputLocationCollection.EMPTY,
            OutputLocationCollection.EMPTY,
            List.of(),
            List.of(),
            OutputLocationCollection.EMPTY,
            false,
            StrictDependencyCheckedType.OFF
        );
    }

    public record KotlinBuildTarget(
        @Nullable String languageVersion,
        @Nullable String apiVersion,
        StringsCollection kotlincOptions,
        List<WorkspaceTargetKey> associates,
        @Nullable String moduleName,
        OutputLocationCollection stdlibJars,
        OutputLocationCollection stdlibInferredSourceJars,
        List<WorkspaceTargetKey> exportedCompilerPluginTargetsList,
        OutputLocationCollection kspSourceJars
    ) implements BuildTargetData {
        // consulo: upstream default values of moduleName, stdlibJars, stdlibInferredSourceJars,
        // exportedCompilerPluginTargetsList and kspSourceJars
        public KotlinBuildTarget(
            @Nullable String languageVersion,
            @Nullable String apiVersion,
            StringsCollection kotlincOptions,
            List<WorkspaceTargetKey> associates
        ) {
            this(
                languageVersion,
                apiVersion,
                kotlincOptions,
                associates,
                null,
                OutputLocationCollection.EMPTY,
                OutputLocationCollection.EMPTY,
                List.of(),
                OutputLocationCollection.EMPTY
            );
        }
    }

    public record ScalaBuildTarget(
        String scalaVersion,
        OutputLocationCollection sdkJars,
        StringsCollection scalacOptions,
        List<Label> scalatestClasspathTargets
    ) implements BuildTargetData {
    }

    public record JvmOutputs(
        OutputLocationCollection binaryJars,
        OutputLocationCollection interfaceJars,
        OutputLocationCollection sourceJars
    ) {
        // consulo: upstream default values of all parameters
        public JvmOutputs() {
            this(OutputLocationCollection.EMPTY, OutputLocationCollection.EMPTY, OutputLocationCollection.EMPTY);
        }
    }

    public record JdepsJar(
        Label syntheticLabel,
        OutputLocation jar
    ) {
    }

    public record JavaProviderData(
        OutputLocationCollection fullCompileJars,
        boolean hasApiGeneratingPlugins
    ) implements BuildTargetData {
        // consulo: upstream default values of all parameters
        public JavaProviderData() {
            this(OutputLocationCollection.EMPTY, false);
        }
    }

    public record JavaToolchainData(
        @Nullable String sourceVersion,
        @Nullable String targetVersion,
        @Nullable OutputLocation javaHome,
        @Nullable OutputLocation bootClasspathJavaHome,
        boolean isExecConfig
    ) implements BuildTargetData {
        // consulo: upstream default values of all parameters
        public JavaToolchainData() {
            this(null, null, null, null, false);
        }
    }

    public sealed interface JvmDependency permits JvmDependency.LibraryDependency, JvmDependency.ModuleDependency {
        DependencyLabel dependency();

        record LibraryDependency(DependencyLabel dependency) implements JvmDependency {
        }

        record ModuleDependency(DependencyLabel dependency) implements JvmDependency {
        }
    }

    public static @Nullable JvmBuildTarget extractJvmBuildTarget(BuildTarget target) {
        return BuildTarget.extractData(target, JvmBuildTarget.class);
    }

    public static @Nullable ScalaBuildTarget extractScalaBuildTarget(BuildTarget target) {
        return BuildTarget.extractData(target, ScalaBuildTarget.class);
    }

    public static @Nullable KotlinBuildTarget extractKotlinBuildTarget(BuildTarget target) {
        return BuildTarget.extractData(target, KotlinBuildTarget.class);
    }
}
