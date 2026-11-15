package org.jetbrains.bazel.workspace.importer;

import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.ScalaBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetMerger;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetMerger.MergeFunction;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

// consulo: upstream Kotlin top-level declarations
public final class JvmMergeFunctions {
    private JvmMergeFunctions() {
    }

    public static final Map<Class<? extends BuildTargetData>, MergeFunction<?>> jvmTargetMergeFunctions = Map.of(
        JvmBuildTarget.class, (MergeFunction<JvmBuildTarget>) (left, right) -> {
            // consulo: data class copy() with the changed values
            return new JvmBuildTarget(
                left.javacOpts(),
                WorkspaceTargetMerger.mergeLocationCollections(left.binaryOutputs(), right.binaryOutputs()),
                left.environmentVariables(),
                left.mainClass(),
                left.jvmArgs(),
                left.programArgs(),
                left.resolvedResourceStripPrefix(),
                WorkspaceTargetMerger.mergeLocationCollections(left.outputInterfaceJars(), right.outputInterfaceJars()),
                WorkspaceTargetMerger.mergeLocationCollections(left.outputSourceJars(), right.outputSourceJars()),
                distinctPlus(left.generatedJars(), right.generatedJars()),
                distinctPlus(left.jdepsJars(), right.jdepsJars()),
                WorkspaceTargetMerger.mergeLocationCollections(left.intellijPluginJars(), right.intellijPluginJars()),
                left.hasExecutableInfo() || right.hasExecutableInfo(),
                left.checkStrictDependencies()
            );
        },

        KotlinBuildTarget.class, (MergeFunction<KotlinBuildTarget>) (left, right) -> {
            return new KotlinBuildTarget(
                left.languageVersion(),
                left.apiVersion(),
                left.kotlincOptions(),
                distinctPlus(left.associates(), right.associates()),
                left.moduleName(),
                WorkspaceTargetMerger.mergeLocationCollections(left.stdlibJars(), right.stdlibJars()),
                WorkspaceTargetMerger.mergeLocationCollections(left.stdlibInferredSourceJars(), right.stdlibInferredSourceJars()),
                distinctPlus(left.exportedCompilerPluginTargetsList(), right.exportedCompilerPluginTargetsList()),
                left.kspSourceJars()
            );
        },

        ScalaBuildTarget.class, (MergeFunction<ScalaBuildTarget>) (left, right) -> {
            return new ScalaBuildTarget(
                left.scalaVersion(),
                WorkspaceTargetMerger.mergeLocationCollections(left.sdkJars(), right.sdkJars()),
                left.scalacOptions(),
                distinctPlus(left.scalatestClasspathTargets(), right.scalatestClasspathTargets())
            );
        }
    );

    // consulo: Kotlin `(left + right).distinct()`
    private static <T> List<T> distinctPlus(List<T> left, List<T> right) {
        LinkedHashSet<T> result = new LinkedHashSet<>(left);
        result.addAll(right);
        return new ArrayList<>(result);
    }
}
