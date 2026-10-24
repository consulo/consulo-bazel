/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.server.sync.sharding;

import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.ShardingApproach;
import org.jetbrains.bazel.commons.TargetCollection;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.server.sync.sharding.WildcardTargetExpander.ExpandedTargetsResult;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler.BazelTaskLogger;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Utility methods for sharding Bazel build invocations.  */
public final class BazelBuildTargetSharder {
    private BazelBuildTargetSharder() {
    }

    /**
     * Max number of individual targets per Bazel build shard.
     */
    private static final int MAX_TARGET_SHARD_SIZE = 10000;

    /**
     * number of packages per bazel query shard
     */
    public static final int PACKAGE_SHARD_SIZE = 500;

    /** Expand wildcard target patterns and partition the resulting target list.  */
    public static CompletableFuture<ShardedTargetsResult> expandAndShardTargets(
        BazelPathsResolver pathResolver,
        TargetCollection targets,
        ProjectView projectView,
        BazelRunner bazelRunner,
        BazelTaskLogger taskLogger,
        TaskId taskId,
        @Nullable List<Label> allTargets /* all known targets, if any, from first phase */
    ) {
        if (allTargets != null) {
            return CompletableFuture.completedFuture(new ShardedTargetsResult(
                shardTargetsToBatches(allTargets, List.of(), getTargetShardSize(projectView)),
                BazelStatus.SUCCESS
            ));
        }
        List<Label> includes = targets.values();
        List<Label> excludes = targets.excludedValues();
        ShardingApproach shardingApproach = getShardingApproach(projectView);
        return switch (shardingApproach) {
            case SHARD_ONLY -> CompletableFuture.completedFuture(
                new ShardedTargetsResult(
                    shardTargetsToBatches(includes, excludes, getTargetShardSize(projectView)),
                    BazelStatus.SUCCESS
                )
            );

            case QUERY_AND_SHARD ->
                WildcardTargetExpander.queryIndividualTargets(includes, excludes, bazelRunner, taskId, projectView).thenApply(singleTargets ->
                    new ShardedTargetsResult(
                        shardTargetsToBatches(singleTargets.getSingleTargets(), List.of(), getTargetShardSize(projectView)),
                        singleTargets.getBuildResult()
                    )
                );

            case EXPAND_AND_SHARD ->
                expandWildcardTargets(
                    pathResolver,
                    includes,
                    excludes,
                    bazelRunner,
                    taskLogger,
                    taskId,
                    projectView
                ).thenApply(expandedTargets -> {
                    if (expandedTargets.getBuildResult() == BazelStatus.FATAL_ERROR) {
                        return new ShardedTargetsResult(List.of(), expandedTargets.getBuildResult());
                    }
                    else {
                        return new ShardedTargetsResult(
                            shardTargetsToBatches(expandedTargets.getSingleTargets(), excludes, getTargetShardSize(projectView)),
                            expandedTargets.getBuildResult()
                        );
                    }
                });
        };
    }

    private static ShardingApproach getShardingApproach(ProjectView projectView) {
        String shardingApproach = ProjectViewExtensions.getShardingApproach(projectView);
        if (shardingApproach == null) {
            return ShardingApproach.QUERY_AND_SHARD;
        }
        ShardingApproach approach = ShardingApproach.fromString(
            shardingApproach
        );
        return approach != null ? approach : ShardingApproach.QUERY_AND_SHARD;
    }

    /** Number of individual targets per blaze build shard.  */
    private static int getTargetShardSize(ProjectView projectView) {
        return Math.min(ProjectViewExtensions.getTargetShardSize(projectView), MAX_TARGET_SHARD_SIZE);
    }

    /**
     *  Expand wildcard target patterns into individual Bazel targets.
     */
    private static CompletableFuture<ExpandedTargetsResult> expandWildcardTargets(
        BazelPathsResolver pathsResolver,
        List<Label> includes,
        List<Label> excludes,
        BazelRunner bazelRunner,
        BazelTaskLogger taskLogger,
        TaskId taskId,
        ProjectView projectView
    ) {
        List<Label> wildcardIncludes = includes.stream().filter(Label::isWildcard).toList();
        if (wildcardIncludes.isEmpty()) {
            return CompletableFuture.completedFuture(new ExpandedTargetsResult(new LinkedHashSet<>(includes), BazelStatus.SUCCESS));
        }
        Map<Label, List<Label>> expandedTargets =
            WildcardTargetExpander.expandToNonRecursiveWildcardTargets(
                pathsResolver,
                wildcardIncludes
            );

        // replace original recursive targets with the expanded list, retaining relative ordering
        List<Label> fullList = new ArrayList<>();
        for (Label target : includes) {
            List<Label> expanded = expandedTargets.get(target);
            if (expanded == null) {
                fullList.add(target);
            }
            else {
                fullList.addAll(expanded);
            }
        }

        return WildcardTargetExpander
            .expandToSingleTargets(
                fullList,
                excludes,
                bazelRunner,
                taskLogger,
                taskId,
                projectView
            ).thenApply(expandToSingleTargetsResult -> {
                ExpandedTargetsResult result = WildcardTargetExpander.orEmpty(expandToSingleTargetsResult);

                // finally add back any explicitly-specified single targets which may have been removed by the
                // query (for example, because they have the 'manual' tag). shardTargetsToBatches drops the
                // excluded ones again.
                Set<Label> singleTargets = new LinkedHashSet<>();
                for (Label it : includes) {
                    if (!it.isWildcard()) {
                        singleTargets.add(it);
                    }
                }
                return ExpandedTargetsResult.merge(
                    result,
                    new ExpandedTargetsResult(singleTargets, result.getBuildResult())
                );
            });
    }

    /**
     * Shards a list of individual Bazel targets (with no wildcard expressions other than for excluded
     * target patterns).
     * <p>
     * An exact excluded label is removed here. An excluded wildcard pattern goes to every shard, and
     * Bazel applies it, because a {@link Label} cannot match a pattern.
     */
    public static List<TargetCollection> shardTargetsToBatches(
        Collection<Label> targets,
        Collection<Label> excludes,
        int softShardSize
    ) {
        Set<Label> exactExcludes = new HashSet<>();
        for (Label it : excludes) {
            if (!it.isWildcard()) {
                exactExcludes.add(it);
            }
        }
        List<Label> targetsWithoutExactExcludes = targets.stream().filter(it -> !exactExcludes.contains(it)).toList();
        List<List<Label>> targetBatches = calculateTargetBatches(targetsWithoutExactExcludes, softShardSize);
        List<Label> excludedValues = List.copyOf(excludes);
        return targetBatches.stream().map(batch ->
            new TargetCollection(
                batch,
                excludedValues
            )
        ).toList();
    }

    /**
     * Given a list of individual, un-excluded blaze targets (no wildcard target patterns), returns a
     * list of target batches.
     * <p>
     * Two rules apply:
     * - A batch holds the targets of one repository only. Bazel loads a repository per batch, so a
     *   mixed batch makes the build load more than it needs.
     * - A batch keeps the targets of one package together. Targets in a package share their load and
     *   analysis work.
     * <p>
     * Therefore {@code softShardSize} is a goal, not a limit. A batch takes one more package only while it
     * stays at or below the goal, but a single package larger than the goal still goes into one batch.
     * {@link #MAX_TARGET_SHARD_SIZE} is the true limit. A package above it is split, because a batch that
     * large risks an OOM.
     */
    public static List<List<Label>> calculateTargetBatches(Collection<Label> targets, int softShardSize) {
        if (!(softShardSize > 0)) {
            throw new IllegalArgumentException("The shard size must be greater than zero, but it is " + softShardSize);
        }
        // consulo: upstream sorted().groupBy { label -> (label as? ResolvedLabel)?.repo }
        Map<Label.@Nullable RepoType, List<Label>> targetsByRepository = new LinkedHashMap<>();
        targets.stream().sorted().forEach(label -> {
            Label.RepoType repo = label instanceof ResolvedLabel resolvedLabel ? resolvedLabel.repo() : null;
            targetsByRepository.computeIfAbsent(repo, it -> new ArrayList<>()).add(label);
        });
        List<List<Label>> result = new ArrayList<>();
        for (List<Label> repositoryTargets : targetsByRepository.values()) {
            result.addAll(batchPackagesOfOneRepository(repositoryTargets, softShardSize));
        }
        return result;
    }

    /**
     * Puts the packages of one repository into batches. Keeps the targets of a package together, and
     * fills a batch up to {@code softShardSize}.
     */
    private static List<List<Label>> batchPackagesOfOneRepository(List<Label> targets, int softShardSize) {
        List<List<Label>> batches = new ArrayList<>();
        List<Label> batch = new ArrayList<>();
        // consulo: upstream groupBy { label -> label.packagePath }
        Map<Label.PackageType, List<Label>> targetsByPackage = new LinkedHashMap<>();
        for (Label label : targets) {
            targetsByPackage.computeIfAbsent(label.packagePath(), it -> new ArrayList<>()).add(label);
        }
        for (List<Label> packageTargets : targetsByPackage.values()) {
            boolean splitPackage = packageTargets.size() > MAX_TARGET_SHARD_SIZE;
            boolean batchIsFull = batch.size() + packageTargets.size() > softShardSize;
            if (!batch.isEmpty() && (splitPackage || batchIsFull)) {
                batches.add(batch);
                batch = new ArrayList<>();
            }
            if (splitPackage) {
                batches.addAll(chunked(packageTargets, MAX_TARGET_SHARD_SIZE));
            }
            else {
                batch.addAll(packageTargets);
            }
        }
        if (!batch.isEmpty()) {
            batches.add(batch);
        }
        return batches;
    }

    /**
     * Partition targets list, retaining the original relative ordering. The caller passes the excluded
     * targets separately.
     */
    public static List<List<Label>> shardTargetsRetainingOrdering(List<Label> targets, int shardSize) {
        return chunked(targets, shardSize);
    }

    public record ShardedTargetsResult(
        List<TargetCollection> targets,
        BazelStatus buildResult
    ) {
    }

    // consulo: Kotlin Iterable.chunked(size)
    private static <T> List<List<T>> chunked(List<T> list, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size " + size + " must be greater than zero.");
        }
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
        }
        return result;
    }
}
