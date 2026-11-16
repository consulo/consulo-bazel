package org.jetbrains.bazel.sync.workspace.snapshot;

import consulo.logging.Logger;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

// consulo: upstream `typealias MergeFunctionMap = Map<KClass<out BuildTargetData>, MergeFunction<*>>`,
// it is spelled out as Map<Class<? extends BuildTargetData>, MergeFunction<?>>
public class WorkspaceTargetMerger {
    @FunctionalInterface
    public interface MergeFunction<T extends BuildTargetData> {
        T invoke(T left, T right);
    }

    private static final Logger LOG = Logger.getInstance(WorkspaceTargetMerger.class);

    private final Map<Class<? extends BuildTargetData>, MergeFunction<?>> myMergeFunctions;

    public WorkspaceTargetMerger(Map<Class<? extends BuildTargetData>, MergeFunction<?>> mergeFunctions) {
        myMergeFunctions = mergeFunctions;
    }

    public Map<Class<? extends BuildTargetData>, MergeFunction<?>> getMergeFunctions() {
        return myMergeFunctions;
    }

    public List<BuildTarget> mergeByTargetKey(Collection<BuildTarget> targets) {
        Map<WorkspaceTargetKey, List<BuildTarget>> groups = new LinkedHashMap<>();
        for (BuildTarget it : targets) {
            WorkspaceTargetKey groupKey = new WorkspaceTargetKey(it.getKey().label(), it.getKey().configuration(), WorkspaceAspectIds.EMPTY);
            groups.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(it);
        }
        List<BuildTarget> result = new ArrayList<>(groups.size());
        for (Map.Entry<WorkspaceTargetKey, List<BuildTarget>> entry : groups.entrySet()) {
            WorkspaceTargetKey key = entry.getKey();
            List<BuildTarget> group = entry.getValue();
            if (group.size() == 1) {
                BuildTarget target = group.get(0);
                result.add(BuildTarget.copy(
                    target,
                    withKey(target.getInfo(), key, target.getKind()),
                    target.getDependencies(),
                    target.getSources(),
                    target.getResources(),
                    target.getData()
                ));
            }
            else {
                // keep it deterministic
                result.add(group.stream()
                    .sorted(Comparator.comparing(it -> it.getKey().toString()))
                    .reduce((l, r) -> merge(key, l, r))
                    .orElseThrow());
            }
        }
        return result;
    }

    private BuildTarget merge(WorkspaceTargetKey key, BuildTarget left, BuildTarget right) {
        if (!isCompatibleWith(left, right)) {
            LOG.warn("Trying to merge incompatible " + BuildTarget.class + ", " + key + ", workspace model could be incorrect.");
        }

        Set<LanguageClass> languageClasses = new LinkedHashSet<>(left.getKind().languageClasses());
        languageClasses.addAll(right.getKind().languageClasses());
        TargetKind kind = new TargetKind(left.getKind().kind(), languageClasses, left.getKind().ruleType());

        List<DependencyLabel> allDependencies = new ArrayList<>(left.getDependencies());
        allDependencies.addAll(right.getDependencies());
        Map<DependencyLabel, DependencyLabel> distinctDependencies = new LinkedHashMap<>();
        for (DependencyLabel it : allDependencies) {
            WorkspaceTargetKey targetKey = it.targetKey();
            distinctDependencies.putIfAbsent(
                new DependencyLabel(new WorkspaceTargetKey(targetKey.label(), targetKey.configuration(), WorkspaceAspectIds.EMPTY), it.kind()),
                it
            );
        }
        List<DependencyLabel> dependencies = new ArrayList<>(distinctDependencies.values());

        OutputLocationCollection sources = mergeLocationCollections(left.getSources(), right.getSources());
        OutputLocationCollection resources = mergeLocationCollections(left.getResources(), right.getResources());

        List<BuildTargetData> data = mergeBuildData(Stream.concat(left.getData().stream(), right.getData().stream()));

        return BuildTarget.create(
            withKey(left.getInfo(), key, kind),
            dependencies,
            sources,
            resources,
            data
        );
    }

    // consulo: Kotlin data class copy(key = ..., kind = ...) of BuildTargetInfo
    private static BuildTargetInfo withKey(BuildTargetInfo receiver, WorkspaceTargetKey key, TargetKind kind) {
        return new BuildTargetInfo(
            key,
            kind,
            receiver.generatorName(),
            receiver.isTestOnly(),
            receiver.tags(),
            receiver.workspaceName()
        );
    }

    private static boolean isCompatibleWith(BuildTarget receiver, BuildTarget other) {
        return receiver.getKind().kind().equals(other.getKind().kind())
            && receiver.getKind().ruleType() == other.getKind().ruleType()
            && Objects.equals(receiver.getGeneratorName(), other.getGeneratorName())
            && BuildTarget.isManual(receiver) == BuildTarget.isManual(other)
            && receiver.isTestOnly() == other.isTestOnly();
    }

    @SuppressWarnings("unchecked")
    private List<BuildTargetData> mergeBuildData(Stream<BuildTargetData> input) {
        // we have to merge them, `BuildTargetData` doesn't correspond to specific provider
        // BuildTargetData` can overlap relative to source provider
        Map<Class<?>, List<BuildTargetData>> groups = new LinkedHashMap<>();
        input.forEach(it -> groups.computeIfAbsent(it.getClass(), ignored -> new ArrayList<>()).add(it));
        List<BuildTargetData> result = new ArrayList<>(groups.size());
        for (Map.Entry<Class<?>, List<BuildTargetData>> entry : groups.entrySet()) {
            Class<?> type = entry.getKey();
            List<BuildTargetData> data = entry.getValue();
            if (data.size() == 1) {
                result.add(data.get(0));
            }
            else {
                MergeFunction<BuildTargetData> fn = (MergeFunction<BuildTargetData>) myMergeFunctions.get(type);
                if (fn == null) {
                    // only warn when candidates are indeed different
                    boolean allEqual = data.stream().allMatch(it -> it.equals(data.get(0)));
                    if (!allEqual) {
                        LOG.warn("Trying to merge unsupported " + BuildTargetData.class + ", " + type + ", workspace model could be incorrect.");
                    }
                    result.add(data.get(0));
                }
                else {
                    result.add(data.stream().reduce(fn::invoke).orElseThrow());
                }
            }
        }
        return result;
    }

    public static OutputLocationCollection mergeLocationCollections(OutputLocationCollection left, OutputLocationCollection right) {
        if (left.equals(right)) {
            return left;
        }
        return new OutputLocationCollection() {
            @Override
            public boolean isEmpty() {
                return left.isEmpty() && right.isEmpty();
            }

            @Override
            public Stream<OutputLocation> getOutputLocations() {
                return Stream.concat(left.getOutputLocations(), right.getOutputLocations()).distinct();
            }
        };
    }
}
