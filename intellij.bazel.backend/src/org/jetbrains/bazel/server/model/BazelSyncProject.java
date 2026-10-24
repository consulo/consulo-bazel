package org.jetbrains.bazel.server.model;

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target;
import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// consulo: holder class for the top-level declarations of upstream BazelSyncProject.kt
public final class BazelSyncProject {
    private BazelSyncProject() {
    }

    public record PhasedSyncProject(
        Path workspaceRoot,
        BazelRelease bazelRelease,
        Map<Label, Target> modules,
        boolean hasError
    ) {
        public PhasedSyncProject(Path workspaceRoot, BazelRelease bazelRelease, Map<Label, Target> modules) {
            this(workspaceRoot, bazelRelease, modules, false);
        }
    }

    /** Project is the internal model of the project. Bazel/Aspect Model -> Project -> BSP Model  */
    public record AspectSyncProject(
        Path workspaceRoot,
        BazelRelease bazelRelease,
        boolean hasError,

        // TODO: use abstract BEP artifact access here instead of java.nio.Path
        Set<Path> targetProtoPaths,

        Set<WorkspaceTargetKey> rootTargets,
        Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations
    ) {
        // consulo: upstream default value of hasError
        public AspectSyncProject(
            Path workspaceRoot,
            BazelRelease bazelRelease,
            Set<Path> targetProtoPaths,
            Set<WorkspaceTargetKey> rootTargets,
            Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations
        ) {
            this(workspaceRoot, bazelRelease, false, targetProtoPaths, rootTargets, configurations);
        }

        public AspectSyncProject plus(AspectSyncProject project) {
            if (!workspaceRoot.equals(project.workspaceRoot)) {
                throw new IllegalStateException(
                    "Cannot add projects with different workspace roots: " + workspaceRoot + " and " + project.workspaceRoot
                );
            }
            if (!bazelRelease.equals(project.bazelRelease)) {
                throw new IllegalStateException(
                    "Cannot add projects with different bazel versions: " + bazelRelease + " and " + project.bazelRelease
                );
            }

            Set<Path> newTargets = new LinkedHashSet<>(targetProtoPaths);
            newTargets.addAll(project.targetProtoPaths);
            return new AspectSyncProject(workspaceRoot, bazelRelease, hasError, newTargets, rootTargets, configurations);
        }
    }

    // consulo: upstream Kotlin extension property IntellijIdeInfo.TargetIdeInfo.sourcesList
    public static Stream<ArtifactLocation> getSourcesList(IntellijIdeInfo.TargetIdeInfo receiver) {
        return receiver.getSrcsList().stream().filter(it -> it.getIsSource());
    }

    // consulo: upstream Kotlin extension property IntellijIdeInfo.TargetIdeInfo.generatedSourcesList
    public static Stream<ArtifactLocation> getGeneratedSourcesList(IntellijIdeInfo.TargetIdeInfo receiver) {
        return receiver.getSrcsList().stream().filter(it -> !it.getIsSource());
    }
}
