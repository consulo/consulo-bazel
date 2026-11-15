package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.localize.BazelCLionCommonLocalize;
import consulo.build.ui.event.MessageEvent;
import org.jetbrains.bazel.clion.sync.CcBuildTarget;
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.DependencyLabel.DependencyLabelKind;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// consulo: upstream Kotlin top-level functions of buildToolchainMap.kt
public final class buildToolchainMap {
    private buildToolchainMap() {
    }

    // consulo: upstream `context(ctx: CcImportContext) fun buildToolchainMap()`, the context parameter is the first parameter
    public static Map<WorkspaceTargetKey, WorkspaceTargetKey> buildToolchainMap(CcImportContext ctx) {
        Set<WorkspaceTargetKey> toolchains = ctx.getSnapshot().targets().allTargets()
            .filter(it -> WorkspaceHelpers.hasBuildData(it, CcToolchainBuildTarget.class))
            .map(BuildTarget::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        // pick an arbitrary fallback toolchain
        @Nullable WorkspaceTargetKey fallbackToolchain = toolchains.isEmpty() ? null : toolchains.iterator().next();

        Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> problems = new LinkedHashMap<>();
        Map<WorkspaceTargetKey, WorkspaceTargetKey> result = new LinkedHashMap<>();

        for (BuildTarget target : ctx.getSnapshot().targets().allTargets().toList()) {
            if (!WorkspaceHelpers.hasBuildData(target, CcBuildTarget.class)) {
                continue;
            }
            List<WorkspaceTargetKey> candidates = findTargetToolchain(target, toolchains);

            // if the CcInfo was provided by an aspect, we do not know what toolchain the aspect used
            boolean plainTarget = target.getKey().aspectIds().getIds().isEmpty();

            // no need to report an error if the target has no sources
            boolean hasSources = !target.getSources().isEmpty();

            if (hasSources && (candidates.size() > 1 || (candidates.isEmpty() && plainTarget))) {
                problems.put(target.getKey(), candidates);
            }

            @Nullable WorkspaceTargetKey toolchain = candidates.isEmpty() ? fallbackToolchain : candidates.get(0);
            if (toolchain != null) {
                result.put(target.getKey(), toolchain);
            }
        }

        reportProblems(ctx, problems);

        return result;
    }

    /**
     * Prefer {@link CcTargetUtils#findToolchain} over directly calling this function.
     */
    public static List<WorkspaceTargetKey> findTargetToolchain(BuildTarget target, Set<WorkspaceTargetKey> toolchains) {
        List<WorkspaceTargetKey> candidates = target.getDependencies().stream()
            .filter(it -> it.kind() == DependencyLabelKind.TOOLCHAIN)
            .map(DependencyLabel::targetKey)
            .filter(toolchains::contains)
            .toList();

        // prefer the dependencies of type toolchain if any were found
        if (!candidates.isEmpty()) {
            return candidates;
        }

        // for older Bazel versions (<= 7) toolchain dependencies are just regular dependencies
        return target.getDependencies().stream().map(DependencyLabel::targetKey).filter(toolchains::contains).toList();
    }

    private static void reportProblems(CcImportContext ctx, Map<WorkspaceTargetKey, List<WorkspaceTargetKey>> problems) {
        if (problems.isEmpty()) {
            return;
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<WorkspaceTargetKey, List<WorkspaceTargetKey>> problem : problems.entrySet()) {
            String candidates;
            if (problem.getValue().isEmpty()) {
                candidates = BazelCLionCommonLocalize.ccToolchainNoDependenciesFound().get();
            }
            else {
                candidates = problem.getValue().stream().map(utils::presentable).collect(Collectors.joining(", "));
            }

            lines.add(utils.presentable(problem.getKey()) + ": " + candidates);
        }
        String description = String.join("\n", lines);

        ctx.reportEvent(
            MessageEvent.Kind.WARNING,
            BazelCLionCommonLocalize.ccToolchainUnexpectedDependencyCount(problems.size()),
            description
        );
    }
}
