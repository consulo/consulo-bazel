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

import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.TargetCollection;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler.BazelTaskLogger;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Expands wildcard target patterns into individual Bazel targets.  */
public final class WildcardTargetExpander {
    private WildcardTargetExpander() {
    }

    /**
     * Expand recursive wildcard Bazel target patterns into single-package wildcard patterns, via a
     * file system traversal.
     */
    public static Map<Label, List<Label>> expandToNonRecursiveWildcardTargets(
        BazelPathsResolver pathResolver,
        List<Label> wildcardIncludes
    ) {
        return PackageLister.expandPackageTargets(
            pathResolver,
            wildcardIncludes
        );
    }

    /** Runs a sharded Bazel query to expand wildcard targets to individual Bazel targets  */
    public static CompletableFuture<@Nullable ExpandedTargetsResult> expandToSingleTargets(
        List<Label> packageTargets,
        List<Label> excludes,
        BazelRunner bazelRunner,
        BazelTaskLogger taskLogger,
        TaskId taskId,
        ProjectView projectView
    ) {
        List<List<Label>> shards =
            BazelBuildTargetSharder.shardTargetsRetainingOrdering(
                packageTargets,
                BazelBuildTargetSharder.PACKAGE_SHARD_SIZE
            );
        if (shards.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Set<Label> singleTargets = new LinkedHashSet<>();
        BazelStatus buildResult = BazelStatus.SUCCESS;
        return expandShardsToSingleTargets(shards, 0, singleTargets, buildResult, excludes, bazelRunner, taskLogger, taskId, projectView)
            .thenApply(it -> it);
    }

    // consulo: the upstream `for (shard in shards)` loop suspends on each query, so the iterations are chained
    private static CompletableFuture<ExpandedTargetsResult> expandShardsToSingleTargets(
        List<List<Label>> shards,
        int shardIndex,
        Set<Label> singleTargets,
        BazelStatus previousBuildResult,
        List<Label> excludes,
        BazelRunner bazelRunner,
        BazelTaskLogger taskLogger,
        TaskId taskId,
        ProjectView projectView
    ) {
        if (shardIndex >= shards.size()) {
            return CompletableFuture.completedFuture(new ExpandedTargetsResult(singleTargets, previousBuildResult));
        }
        List<Label> shard = shards.get(shardIndex);
        return queryIndividualTargets(shard, excludes, bazelRunner, taskId, projectView).thenCompose(result -> {
            singleTargets.addAll(result.getSingleTargets());
            BazelStatus buildResult = previousBuildResult.merge(result.getBuildResult());
            if (buildResult == BazelStatus.FATAL_ERROR) {
                taskLogger.warn("Bazel query for expanding package targets failed with fatal error. Skipping further expanding queries.");
                return CompletableFuture.completedFuture(new ExpandedTargetsResult(singleTargets, buildResult));
            }
            return expandShardsToSingleTargets(
                shards,
                shardIndex + 1,
                singleTargets,
                buildResult,
                excludes,
                bazelRunner,
                taskLogger,
                taskId,
                projectView
            );
        });
    }

    /** Runs a Bazel query to expand the input target patterns to individual Bazel targets.  */
    public static CompletableFuture<ExpandedTargetsResult> queryIndividualTargets(
        List<Label> includedPatterns,
        List<Label> excludedTargets,
        BazelRunner bazelRunner,
        TaskId taskId,
        ProjectView projectView
    ) {
        TargetCollection targetsSpec =
            new TargetCollection(
                includedPatterns,
                excludedTargets
            );
        BazelCommand command =
            bazelRunner.buildBazelCommand(projectView, builder ->
                // exclude 'manual' targets,
                // which shouldn't be built when expanding wildcard target patterns if `allow_manual_targets_sync` is not specified.
                builder.query(ProjectViewExtensions.getAllowManualTargetsSync(projectView), query -> {
                    query.getTargets().addAll(targetsSpec.values());
                    query.getExcludedTargets().addAll(targetsSpec.excludedValues());
                    query.getOptions().addAll(List.of("--output=label", "--keep_going"));
                })
            );
        return bazelRunner
            .runBazelCommand(command, taskId, false)
            .waitAndGetResult()
            .thenApply(queryResult -> {
                Set<Label> singleTargets = new LinkedHashSet<>();
                for (String it : queryResult.getStdoutLines()) {
                    singleTargets.add(Label.parse(it));
                }
                return new ExpandedTargetsResult(
                    singleTargets,
                    queryResult.getBazelStatus()
                );
            });
    }

    public static class ExpandedTargetsResult {
        private final Set<Label> mySingleTargets;
        private final BazelStatus myBuildResult;

        public ExpandedTargetsResult(Set<Label> singleTargets, BazelStatus buildResult) {
            mySingleTargets = singleTargets;
            myBuildResult = buildResult;
        }

        public Set<Label> getSingleTargets() {
            return mySingleTargets;
        }

        public BazelStatus getBuildResult() {
            return myBuildResult;
        }

        public static ExpandedTargetsResult merge(ExpandedTargetsResult first, ExpandedTargetsResult second) {
            BazelStatus buildResult = first.getBuildResult().merge(second.getBuildResult());
            Set<Label> targets = new LinkedHashSet<>(first.getSingleTargets());
            targets.addAll(second.getSingleTargets());
            return new ExpandedTargetsResult(targets, buildResult);
        }
    }

    public static ExpandedTargetsResult orEmpty(@Nullable ExpandedTargetsResult receiver) {
        return receiver != null ? receiver : new ExpandedTargetsResult(Set.of(), BazelStatus.SUCCESS);
    }
}
