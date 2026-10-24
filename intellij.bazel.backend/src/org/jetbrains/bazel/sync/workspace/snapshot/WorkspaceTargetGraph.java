package org.jetbrains.bazel.sync.workspace.snapshot;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.DependencyLabel.DependencyLabelKind;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.label.Label.SingleTarget;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Immutable {@link BuildTarget} graph, takes into account configurations {@link WorkspaceConfiguration}
 */
public interface WorkspaceTargetGraph {

    /**
     * Entire set of {@link BuildTarget} present inside this {@link WorkspaceTargetGraph}
     */
    Stream<WorkspaceTargetKey> getAllTargets();

    /**
     * Root targets
     */
    Stream<WorkspaceTargetKey> getRootTargets();

    /**
     * Find {@link BuildTarget} by distinct {@link WorkspaceTargetKey}
     *
     * @param targetKey Specific {@link WorkspaceTargetKey}
     * @param strict    Match {@code targetKey} exactly, when false try matching without propagated aspects {@link WorkspaceTargetKey#aspectIds}
     * @return Matched target
     */
    @Nullable WorkspaceTargetKey findTargetByKey(WorkspaceTargetKey targetKey, boolean strict);

    default @Nullable WorkspaceTargetKey findTargetByKey(WorkspaceTargetKey targetKey) {
        return findTargetByKey(targetKey, false);
    }

    /**
     * Find all {@link BuildTarget} that lossy match {@code targetKey}, match only by label and configuration
     *
     * @param targetKey Specific {@link WorkspaceTargetKey}
     * @return All matching targets
     */
    Stream<WorkspaceTargetKey> findTargetsLossy(WorkspaceTargetKey targetKey);

    /**
     * Find all direct successors of target with key {@code targetKey}.
     *
     * @param targetKey Specific {@link WorkspaceTargetKey}
     * @param strict    Match {@code targetKey} exactly, when false try matching without propagated aspects {@link WorkspaceTargetKey#aspectIds}
     */
    Stream<WorkspaceTargetKey> findAllSuccessors(WorkspaceTargetKey targetKey, boolean strict);

    default Stream<WorkspaceTargetKey> findAllSuccessors(WorkspaceTargetKey targetKey) {
        return findAllSuccessors(targetKey, true);
    }

    /**
     * Walk graph and find all targets until depth {@code maxDepth}
     *
     * @param maxDepth                      Max walk depth
     * @param runtimeDependencies           Do include all runtime dependencies
     * @param useRelaxedDependencyExpansion Ignore propagated aspect when traversing through dependencies
     * @param condition                     Include targets that satisfy this condition
     * @return Target list in topological order, according to {@code maxDepth} setting
     */
    Stream<WorkspaceTargetKey> findAllTargetsAtDepth(
        int maxDepth,
        boolean runtimeDependencies,
        boolean useRelaxedDependencyExpansion,
        Predicate<WorkspaceTargetKey> condition
    );

    // consulo: overloads for the upstream default arguments
    // (runtimeDependencies = maxDepth < 0, useRelaxedDependencyExpansion = false, condition = { true })
    default Stream<WorkspaceTargetKey> findAllTargetsAtDepth(int maxDepth) {
        return findAllTargetsAtDepth(maxDepth, maxDepth < 0);
    }

    default Stream<WorkspaceTargetKey> findAllTargetsAtDepth(int maxDepth, boolean runtimeDependencies) {
        return findAllTargetsAtDepth(maxDepth, runtimeDependencies, false);
    }

    default Stream<WorkspaceTargetKey> findAllTargetsAtDepth(int maxDepth, boolean runtimeDependencies, boolean useRelaxedDependencyExpansion) {
        return findAllTargetsAtDepth(maxDepth, runtimeDependencies, useRelaxedDependencyExpansion, key -> true);
    }

    /**
     * Find all transitive successors to {@code targetKey}
     *
     * @param targetKey                     Target key
     * @param useRelaxedDependencyExpansion Ignore propagated aspect when traversing through dependencies
     * @return Sequence of all transitive successors of {@code targetKey}
     */
    Stream<WorkspaceTargetKey> findAllTransitiveSuccessors(
        WorkspaceTargetKey targetKey,
        boolean useRelaxedDependencyExpansion
    );

    default Stream<WorkspaceTargetKey> findAllTransitiveSuccessors(WorkspaceTargetKey targetKey) {
        return findAllTransitiveSuccessors(targetKey, false);
    }

    /**
     * Find all transitive successors to {@code targetKey} excluding traversal over root targets
     *
     * @param targetKey                     Target key
     * @param useRelaxedDependencyExpansion Ignore propagated aspect when traversing through dependencies
     * @return Sequence of all transitive successors of {@code targetKey} exclusing root targets
     */
    Stream<WorkspaceTargetKey> findAllTransitiveSuccessorsWithoutRootTargets(
        WorkspaceTargetKey targetKey,
        boolean useRelaxedDependencyExpansion
    );

    default Stream<WorkspaceTargetKey> findAllTransitiveSuccessorsWithoutRootTargets(WorkspaceTargetKey targetKey) {
        return findAllTransitiveSuccessorsWithoutRootTargets(targetKey, false);
    }

    WorkspaceTargetGraph EMPTY = new WorkspaceTargetGraph() {
        @Override
        public Stream<WorkspaceTargetKey> getAllTargets() {
            return Stream.of();
        }

        @Override
        public Stream<WorkspaceTargetKey> getRootTargets() {
            return Stream.of();
        }

        @Override
        public @Nullable WorkspaceTargetKey findTargetByKey(
            WorkspaceTargetKey targetKey,
            boolean strict
        ) {
            return null;
        }

        @Override
        public Stream<WorkspaceTargetKey> findTargetsLossy(WorkspaceTargetKey targetKey) {
            return Stream.of();
        }

        @Override
        public Stream<WorkspaceTargetKey> findAllSuccessors(
            WorkspaceTargetKey targetKey,
            boolean strict
        ) {
            return Stream.of();
        }

        @Override
        public Stream<WorkspaceTargetKey> findAllTargetsAtDepth(
            int maxDepth,
            boolean runtimeDependencies,
            boolean useRelaxedDependencyExpansion,
            Predicate<WorkspaceTargetKey> condition
        ) {
            return Stream.of();
        }

        @Override
        public Stream<WorkspaceTargetKey> findAllTransitiveSuccessors(
            WorkspaceTargetKey targetKey,
            boolean useRelaxedDependencyExpansion
        ) {
            return Stream.of();
        }

        @Override
        public Stream<WorkspaceTargetKey> findAllTransitiveSuccessorsWithoutRootTargets(
            WorkspaceTargetKey targetKey,
            boolean useRelaxedDependencyExpansion
        ) {
            return Stream.of();
        }

    };

    // consulo: internal upstream top-level constant
    int INVALID_TARGET_ID = -1;

    // consulo: internal upstream, public because the snapshot type contributor registers it
    record LabelConfigKey(Label label, WorkspaceConfigurationId configuration) {
    }

    /**
     * Low memory footprint, immutable target graph representation
     */
    // consulo: internal upstream, public because the snapshot type contributor registers it
    final class WorkspaceTargetGraphImpl implements WorkspaceTargetGraph {
        private final IntSet myRootTargetIds;
        private final Object2IntMap<WorkspaceTargetKey> myTargetKey2TargetId;
        private final Object2ObjectMap<LabelConfigKey, int[]> myLabelConfig2TargetIds;
        private final WorkspaceTargetKey[] myId2WorkspaceTarget;
        private final int[][] myId2CompileSuccessors;
        private final int[][] myId2AllSuccessors;
        private final int[][] myId2RelaxedCompileSuccessors;
        private final int[][] myId2RelaxedAllSuccessors;

        // recreated lazily: Kryo field serialization skips transient fields and instantiates
        // the class without running field initializers
        private transient @Nullable AtomicReferenceArray<int @Nullable []> myId2TransitiveTargetCache;

        WorkspaceTargetGraphImpl(
            IntSet rootTargetIds,
            Object2IntMap<WorkspaceTargetKey> targetKey2TargetId,
            Object2ObjectMap<LabelConfigKey, int[]> labelConfig2TargetIds,
            WorkspaceTargetKey[] id2WorkspaceTarget,
            int[][] id2CompileSuccessors,
            int[][] id2AllSuccessors,
            int[][] id2RelaxedCompileSuccessors,
            int[][] id2RelaxedAllSuccessors
        ) {
            myRootTargetIds = rootTargetIds;
            myTargetKey2TargetId = targetKey2TargetId;
            myLabelConfig2TargetIds = labelConfig2TargetIds;
            myId2WorkspaceTarget = id2WorkspaceTarget;
            myId2CompileSuccessors = id2CompileSuccessors;
            myId2AllSuccessors = id2AllSuccessors;
            myId2RelaxedCompileSuccessors = id2RelaxedCompileSuccessors;
            myId2RelaxedAllSuccessors = id2RelaxedAllSuccessors;
            myId2TransitiveTargetCache = new AtomicReferenceArray<>(id2WorkspaceTarget.length);
        }

        private AtomicReferenceArray<int @Nullable []> transitiveTargetCache() {
            AtomicReferenceArray<int @Nullable []> cache = myId2TransitiveTargetCache;
            if (cache == null) {
                cache = new AtomicReferenceArray<>(myId2WorkspaceTarget.length);
                myId2TransitiveTargetCache = cache;
            }
            return cache;
        }

        // no backing field: a stored Sequence would end up in the serialized form
        @Override
        public Stream<WorkspaceTargetKey> getAllTargets() {
            return Arrays.stream(myId2WorkspaceTarget);
        }

        @Override
        public Stream<WorkspaceTargetKey> getRootTargets() {
            return myRootTargetIds.intStream().mapToObj(it -> myId2WorkspaceTarget[it]);
        }


        @Override
        public @Nullable WorkspaceTargetKey findTargetByKey(WorkspaceTargetKey targetKey, boolean strict) {
            int targetId = myTargetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID);
            if (targetId != INVALID_TARGET_ID) {
                return myId2WorkspaceTarget[targetId];
            }
            if (strict) {
                return null;
            }
            // shadow graph fallback: pick first matching
            int[] candidates = myLabelConfig2TargetIds.get(new LabelConfigKey(targetKey.label(), targetKey.configuration()));
            int canonical = candidates != null && candidates.length > 0 ? candidates[0] : INVALID_TARGET_ID;
            if (canonical == INVALID_TARGET_ID) {
                return null;
            }
            else {
                return myId2WorkspaceTarget[canonical];
            }
        }

        @Override
        public Stream<WorkspaceTargetKey> findTargetsLossy(WorkspaceTargetKey targetKey) {
            int[] ids = myLabelConfig2TargetIds.get(new LabelConfigKey(targetKey.label(), targetKey.configuration()));
            return Arrays.stream(ids != null ? ids : new int[0])
                .mapToObj(it -> myId2WorkspaceTarget[it]);
        }

        @Override
        public Stream<WorkspaceTargetKey> findAllSuccessors(
            WorkspaceTargetKey targetKey,
            boolean strict
        ) {
            int id = myTargetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID);
            if (id == INVALID_TARGET_ID) {
                return Stream.of();
            }
            int[][] collection = strict ? myId2AllSuccessors : myId2RelaxedAllSuccessors;
            return Arrays.stream(collection[id])
                .mapToObj(it -> myId2WorkspaceTarget[it]);
        }

        @Override
        public Stream<WorkspaceTargetKey> findAllTargetsAtDepth(
            int maxDepth,
            boolean runtimeDependencies,
            boolean useRelaxedDependencyExpansion,
            Predicate<WorkspaceTargetKey> condition
        ) {
            int[] depth = new int[myId2WorkspaceTarget.length];
            Arrays.fill(depth, Integer.MAX_VALUE);

            // TODO: use primitive collection here
            ArrayDeque<Integer> toVisit = new ArrayDeque<>();
            List<WorkspaceTargetKey> result = new ArrayList<>();

            int[][] compileSuccessors;
            if (useRelaxedDependencyExpansion) {
                compileSuccessors = myId2RelaxedCompileSuccessors;
            }
            else {
                compileSuccessors = myId2CompileSuccessors;
            }
            int[][] allSuccessors;
            if (useRelaxedDependencyExpansion) {
                allSuccessors = myId2RelaxedAllSuccessors;
            }
            else {
                allSuccessors = myId2AllSuccessors;
            }

            // perform BFS traversal
            for (IntIterator iterator = myRootTargetIds.intIterator(); iterator.hasNext(); ) {
                int targetId = iterator.nextInt();
                WorkspaceTargetKey target = myId2WorkspaceTarget[targetId];
                if (!condition.test(target)) {
                    continue;
                }
                toVisit.add(targetId);
                depth[targetId] = 0;
            }

            while (!toVisit.isEmpty()) {
                int currentId = toVisit.removeFirst();
                result.add(myId2WorkspaceTarget[currentId]);
                int currentDepth = depth[currentId];
                if (currentDepth == maxDepth) {
                    continue;
                }

                int[] successors = runtimeDependencies ? allSuccessors[currentId] : compileSuccessors[currentId];
                for (int succId : successors) {
                    WorkspaceTargetKey target = myId2WorkspaceTarget[succId];
                    if (!condition.test(target)) {
                        continue;
                    }
                    if (depth[succId] > currentDepth + 1) {
                        depth[succId] = currentDepth + 1;
                        toVisit.addLast(succId);
                    }
                }
            }

            return result.stream();
        }

        // MAYBE RC: avoid Sequence<...> as a return value?,
        //  there are ways to compute transitive closure faster
        //  but this approach is good enough
        @Override
        public Stream<WorkspaceTargetKey> findAllTransitiveSuccessors(
            WorkspaceTargetKey targetKey,
            boolean useRelaxedDependencyExpansion
        ) {
            int targetId = myTargetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID);
            if (targetId == INVALID_TARGET_ID) {
                return Stream.empty();
            }
            return Arrays.stream(computeTransitiveSuccessorIds(targetId, useRelaxedDependencyExpansion))
                .mapToObj(transitiveSuccId -> myId2WorkspaceTarget[transitiveSuccId]);
        }

        @Override
        public Stream<WorkspaceTargetKey> findAllTransitiveSuccessorsWithoutRootTargets(
            WorkspaceTargetKey targetKey,
            boolean useRelaxedDependencyExpansion
        ) {
            int targetId = myTargetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID);
            if (targetId == INVALID_TARGET_ID) {
                return Stream.empty();
            }
            int[][] allSuccessors = useRelaxedDependencyExpansion ? myId2RelaxedAllSuccessors : myId2AllSuccessors;
            return Arrays.stream(allSuccessors[targetId])
                .filter(depId -> !myRootTargetIds.contains(depId))
                .flatMap(depId ->
                    IntStream.concat(Arrays.stream(computeTransitiveSuccessorIds(depId, useRelaxedDependencyExpansion)), IntStream.of(depId))
                )
                .mapToObj(transitiveSuccId -> myId2WorkspaceTarget[transitiveSuccId]);
        }

        private int[] computeTransitiveSuccessorIds(int targetId, boolean useRelaxedDependencyExpansion) {
            if (useRelaxedDependencyExpansion) {
                // TODO: cache relaxed transitive closures if profiling shows it matters
                return computeTransitiveSuccessorSlow(targetId, myId2RelaxedCompileSuccessors);
            }
            else {
                return findAllTransitiveSuccessorIds(transitiveTargetCache(), targetId);
            }
        }

        private int[] findAllTransitiveSuccessorIds(AtomicReferenceArray<int @Nullable []> cache, int targetId) {
            int[] cached = cache.get(targetId);
            if (cached != null) {
                return cached;
            }
            IntOpenHashSet visited = new IntOpenHashSet();
            for (int succId : myId2CompileSuccessors[targetId]) {
                if (!visited.add(succId)) {
                    continue;
                }
                for (int succDepId : findAllTransitiveSuccessorIds(cache, succId)) {
                    visited.add(succDepId);
                }
            }
            int[] baked = visited.toIntArray();
            cache.compareAndSet(targetId, null, baked);
            return baked;
        }

        private int[] computeTransitiveSuccessorSlow(int targetId, int[][] adjacency) {
            IntOpenHashSet visited = new IntOpenHashSet();
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            stack.addLast(targetId);
            while (!stack.isEmpty()) {
                int currentId = stack.removeLast();
                for (int succId : adjacency[currentId]) {
                    if (visited.add(succId)) {
                        stack.addLast(succId);
                    }
                }
            }
            return visited.toIntArray();
        }

    }

    final class WorkspaceTargetGraphBuilder {
        private WorkspaceTargetGraphBuilder() {
        }

        // RC: Generally speaking, `WorkspaceTargetGraph` shouldn't relay on rootTargets from BEP,
        //  instead workspace sync scope should be obtained using query, updating `rootTargets` incrementally
        //  and keeping track of it is hard problem to solve, that I don't want to solve - its not even worth looking into it
        //  for now when we're living in the world with monolithic sync pipeline passing entire set of
        //  `rootTargets` is fine, but for future change we shall consider removing it in order to provide easy
        //  to understand and simple incremental snapshot updating
        public static WorkspaceTargetGraph build(Set<WorkspaceTargetKey> rootTargets, Collection<BuildTarget> targets) {
            // with this trick, we have unique IDs for each BuildTarget
            BuildTarget[] id2WorkspaceTarget = targets.toArray(new BuildTarget[0]);
            IntOpenHashSet rootTargetIds = new IntOpenHashSet();
            Object2IntOpenHashMap<WorkspaceTargetKey> targetKey2TargetId = new Object2IntOpenHashMap<>();

            Map<LabelConfigKey, IntArrayList> labelConfig2TargetIdList = new HashMap<>();
            for (int targetId = 0; targetId < id2WorkspaceTarget.length; targetId++) {
                BuildTarget target = id2WorkspaceTarget[targetId];
                targetKey2TargetId.put(target.getKey(), targetId);
                LabelConfigKey smallKey = new LabelConfigKey(target.getKey().label(), target.getKey().configuration());
                labelConfig2TargetIdList.computeIfAbsent(smallKey, it -> new IntArrayList()).add(targetId);
                if (rootTargets.contains(target.getKey())) {
                    rootTargetIds.add(targetId);
                }
            }

            Object2ObjectOpenHashMap<LabelConfigKey, int[]> labelConfig2TargetIds = new Object2ObjectOpenHashMap<>(labelConfig2TargetIdList.size());
            for (Map.Entry<LabelConfigKey, IntArrayList> entry : labelConfig2TargetIdList.entrySet()) {
                labelConfig2TargetIds.put(entry.getKey(), entry.getValue().toIntArray());
            }

            Set<Label> rootLabels = new HashSet<>();
            for (WorkspaceTargetKey it : rootTargets) {
                rootLabels.add(it.label());
            }
            for (int targetId = 0; targetId < id2WorkspaceTarget.length; targetId++) {
                BuildTarget target = id2WorkspaceTarget[targetId];
                String generatorName = target.getGeneratorName();
                if (generatorName == null) {
                    continue;
                }
                if (!generatorName.isEmpty()) {
                    ResolvedLabel resolvedLabel = Label.assumeResolved(target.getKey().label());
                    ResolvedLabel generatorTargetLabel =
                        new ResolvedLabel(resolvedLabel.repo(), resolvedLabel.packagePath(), new SingleTarget(generatorName));

                    // try the macro/alias key with the current target's `aspectIds` propagated first,
                    // then the configuration-only key, then the label-only key.
                    WorkspaceTargetKey generatorTargetKey = new WorkspaceTargetKey(generatorTargetLabel, target.getKey().configuration());
                    WorkspaceTargetKey generatorTargetKeyWithAspect =
                        new WorkspaceTargetKey(
                            generatorTargetLabel,
                            target.getKey().configuration(),
                            target.getKey().aspectIds()
                        );
                    WorkspaceTargetKey generatorTargetKeyWithConfiguration = new WorkspaceTargetKey(generatorTargetLabel);
                    boolean isGeneratorTargetInScope = Stream.of(generatorTargetKeyWithAspect, generatorTargetKey, generatorTargetKeyWithConfiguration)
                        .mapToInt(it -> targetKey2TargetId.getOrDefault(it, INVALID_TARGET_ID))
                        .anyMatch(it -> it != INVALID_TARGET_ID);

                    // Support macros that generate a target and refer to it by an alias, see https://youtrack.jetbrains.com/issue/BAZEL-3048
                    // Aliases generated by macros can appear in rootTargets. But the aspect isn't run on aliases, so we can't get the target it refers to without a query.
                    // However, every target generated by a macro has the original macro name in .generatorName.
                    // We use that here to link the alias to its actual target in the graph, without an additional Bazel query invocation.
                    if (rootLabels.contains(generatorTargetKey.label()) && !isGeneratorTargetInScope) {
                        rootTargetIds.add(targetId);
                    }
                }
            }

            Predicate<DependencyLabel> compilePredicate =
                it -> it.kind() == DependencyLabelKind.COMPILE || it.kind() == DependencyLabelKind.EXPORTED_COMPILE_TIME;
            Predicate<DependencyLabel> allPredicate = it -> true;

            WorkspaceTargetKey[] id2WorkspaceTargetKey = new WorkspaceTargetKey[id2WorkspaceTarget.length];
            for (int it = 0; it < id2WorkspaceTarget.length; it++) {
                id2WorkspaceTargetKey[it] = id2WorkspaceTarget[it].getKey();
            }

            return new WorkspaceTargetGraphImpl(
                rootTargetIds,
                targetKey2TargetId,
                labelConfig2TargetIds,
                id2WorkspaceTargetKey,
                buildSuccessorIds(id2WorkspaceTarget, targetKey2TargetId, compilePredicate),
                buildSuccessorIds(id2WorkspaceTarget, targetKey2TargetId, allPredicate),
                buildRelaxedSuccessorIds(id2WorkspaceTarget, labelConfig2TargetIds, compilePredicate),
                buildRelaxedSuccessorIds(id2WorkspaceTarget, labelConfig2TargetIds, allPredicate)
            );
        }

        // consulo: upstream local functions of build(), the captured locals are passed explicitly
        private static int resolveDepId(Object2IntMap<WorkspaceTargetKey> targetKey2TargetId, BuildTarget parent, DependencyLabel dep) {
            int exact = targetKey2TargetId.getOrDefault(dep.targetKey(), INVALID_TARGET_ID);
            if (exact != INVALID_TARGET_ID) {
                return exact;
            }
            // shadow graph stuff: try key with parent aspects
            if (parent.getKey().aspectIds().equals(WorkspaceAspectIds.EMPTY)) {
                return INVALID_TARGET_ID;
            }
            WorkspaceTargetKey propagated =
                new WorkspaceTargetKey(dep.targetKey().label(), dep.targetKey().configuration(), parent.getKey().aspectIds());
            return targetKey2TargetId.getOrDefault(propagated, INVALID_TARGET_ID);
        }

        private static int[][] buildSuccessorIds(
            BuildTarget[] id2WorkspaceTarget,
            Object2IntMap<WorkspaceTargetKey> targetKey2TargetId,
            Predicate<DependencyLabel> predicate
        ) {
            int[][] result = new int[id2WorkspaceTarget.length][];
            for (int targetId = 0; targetId < id2WorkspaceTarget.length; targetId++) {
                BuildTarget target = id2WorkspaceTarget[targetId];
                result[targetId] = target.getDependencies()
                    .stream()
                    .filter(predicate)
                    .mapToInt(it -> resolveDepId(targetKey2TargetId, target, it))
                    .filter(it -> it != INVALID_TARGET_ID)
                    .toArray();
            }
            return result;
        }

        private static int[] resolveDepIdsRelaxed(Object2ObjectMap<LabelConfigKey, int[]> labelConfig2TargetIds, DependencyLabel dep) {
            int[] ids = labelConfig2TargetIds.get(new LabelConfigKey(dep.targetKey().label(), dep.targetKey().configuration()));
            return ids != null ? ids : new int[0];
        }

        private static int[][] buildRelaxedSuccessorIds(
            BuildTarget[] id2WorkspaceTarget,
            Object2ObjectMap<LabelConfigKey, int[]> labelConfig2TargetIds,
            Predicate<DependencyLabel> predicate
        ) {
            int[][] result = new int[id2WorkspaceTarget.length][];
            for (int targetId = 0; targetId < id2WorkspaceTarget.length; targetId++) {
                BuildTarget target = id2WorkspaceTarget[targetId];
                IntOpenHashSet acc = new IntOpenHashSet();
                for (DependencyLabel dep : target.getDependencies()) {
                    if (!predicate.test(dep)) {
                        continue;
                    }
                    for (int id : resolveDepIdsRelaxed(labelConfig2TargetIds, dep)) {
                        acc.add(id);
                    }
                }
                result[targetId] = acc.toIntArray();
            }
            return result;
        }
    }
}
