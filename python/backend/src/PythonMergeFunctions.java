package com.intellij.bazel.python.backend;

import org.jetbrains.bazel.python.lang.PythonBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetMerger;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetMerger.MergeFunction;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.StringsCollection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

// consulo: upstream Kotlin top-level declarations
public final class PythonMergeFunctions {
    private PythonMergeFunctions() {
    }

    public static final Map<Class<? extends BuildTargetData>, MergeFunction<?>> pythonTargetMergeFunctions = Map.of(
        PythonBuildTarget.class, (MergeFunction<PythonBuildTarget>) (left, right) -> {
            // consulo: data class copy() with the changed values; Kotlin `(left + right).distinct()` and map `left + right`
            LinkedHashSet<String> imports = new LinkedHashSet<>(left.imports().asList());
            imports.addAll(right.imports().asList());
            Map<OutputLocation, OutputLocation> generatedSourceOverrides = new LinkedHashMap<>(left.generatedSourceOverrides());
            generatedSourceOverrides.putAll(right.generatedSourceOverrides());
            return new PythonBuildTarget(
                left.version(),
                left.interpreter(),
                StringsCollection.of(new ArrayList<>(imports)),
                WorkspaceTargetMerger.mergeLocationCollections(left.generatedSources(), right.generatedSources()),
                WorkspaceTargetMerger.mergeLocationCollections(left.externalSources(), right.externalSources()),
                left.mainFile(),
                left.mainModule(),
                left.runnerScript(),
                left.targetArgs(),
                generatedSourceOverrides
            );
        }
    );
}
