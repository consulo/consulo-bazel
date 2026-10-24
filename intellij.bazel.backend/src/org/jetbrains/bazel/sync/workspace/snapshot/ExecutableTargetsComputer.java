package org.jetbrains.bazel.sync.workspace.snapshot;

import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bsp.protocol.BuildTarget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO: migrate to WorkspaceTargetKey
public final class ExecutableTargetsComputer {
    private static final int MAX_EXECUTABLE_TARGET_IDS = 5;

    private ExecutableTargetsComputer() {
    }

    public static Map<ResolvedLabel, List<Label>> calculateExecutableTargets(
        Collection<BuildTarget> targets,
        Map<Label, BuildTarget> labelToTargetInfo
    ) {
        Map<Label, Set<Label>> targetDirectDependentsGraph = calculateDirectDependentsGraph(targets);
        Map<Label, Set<Label>> targetToTransitiveRevertedDependenciesCache = new LinkedHashMap<>();
        Map<ResolvedLabel, List<Label>> result = new LinkedHashMap<>();
        for (BuildTarget target : targets) {
            Label label = BuildTarget.getId(target);
            Set<Label> executables = calculateTransitivelyExecutableTargets(
                targetToTransitiveRevertedDependenciesCache,
                targetDirectDependentsGraph,
                label,
                labelToTargetInfo
            );
            if (label instanceof ResolvedLabel resolvedLabel && !executables.isEmpty()) {
                result.put(resolvedLabel, new ArrayList<>(executables));
            }
        }
        Map<ResolvedLabel, List<Label>> sorted = new LinkedHashMap<>();
        for (Map.Entry<ResolvedLabel, List<Label>> entry : result.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue().stream().sorted(Comparator.comparing(Label::toString)).toList());
        }
        return sorted;
    }

    private static Map<Label, Set<Label>> calculateDirectDependentsGraph(Collection<BuildTarget> targets) {
        Map<Label, Set<Label>> targetIdToDirectDependentIds = new HashMap<>();
        for (BuildTarget targetInfo : targets) {
            for (DependencyLabel dependency : targetInfo.getDependencies()) {
                targetIdToDirectDependentIds
                    .computeIfAbsent(dependency.targetKey().label(), it -> new HashSet<>())
                    .add(BuildTarget.getId(targetInfo));
            }
        }
        return targetIdToDirectDependentIds;
    }

    private static Set<Label> calculateTransitivelyExecutableTargets(
        Map<Label, Set<Label>> resultCache,
        Map<Label, Set<Label>> targetDirectDependentsGraph,
        Label target,
        Map<Label, BuildTarget> labelToTargetInfo
    ) {
        return getOrPut(resultCache, target, () -> {
            BuildTarget targetInfo = labelToTargetInfo.get(target);
            if (targetInfo != null && targetInfo.getKind().isExecutable()) {
                return Set.of(target);
            }

            Set<Label> directDependentIds = targetDirectDependentsGraph.get(target);
            if (directDependentIds == null) {
                return Set.of();
            }

            List<Label> executableTargetsFromSamePackage = directDependentIds.stream().filter(it -> {
                BuildTarget info = labelToTargetInfo.get(it);
                return it.packagePath().equals(target.packagePath()) && info != null && info.getKind().isExecutable();
            }).toList();
            if (!executableTargetsFromSamePackage.isEmpty()) {
                return new HashSet<>(executableTargetsFromSamePackage);
            }

            return directDependentIds
                .stream()
                .flatMap(dependency ->
                    calculateTransitivelyExecutableTargets(resultCache, targetDirectDependentsGraph, dependency, labelToTargetInfo).stream()
                )
                .distinct()
                .limit(MAX_EXECUTABLE_TARGET_IDS)
                .collect(Collectors.toCollection(HashSet::new));
        });
    }

    // consulo: Kotlin MutableMap.getOrPut, computeIfAbsent cannot be used because the computation recurses into the map
    private static <K, V> V getOrPut(Map<K, V> map, K key, Supplier<V> defaultValue) {
        V value = map.get(key);
        if (value != null) {
            return value;
        }
        V answer = defaultValue.get();
        map.put(key, answer);
        return answer;
    }
}
