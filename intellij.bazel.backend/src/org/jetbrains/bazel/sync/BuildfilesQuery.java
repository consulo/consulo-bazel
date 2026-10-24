package org.jetbrains.bazel.sync;

import org.jetbrains.bazel.bazelrunner.params.BazelFlag;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.AllPackagesBeneath;
import org.jetbrains.bazel.label.Label.AllRuleTargets;
import org.jetbrains.bazel.label.Label.AllRuleTargetsAndFiles;
import org.jetbrains.bazel.label.Label.Main;
import org.jetbrains.bazel.label.Label.RepoType;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryOutput;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryParams;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class BuildfilesQuery {
    private BuildfilesQuery() {
    }

    // consulo: overload for the upstream default argument (taskId = null)
    public static CompletableFuture<List<Label>> findDependantBuildFiles(
        BazelServerFacade server,
        Set<Path> workspaceRelativePaths,
        Set<RepoType> universeRepos,
        RepoMapping repoMapping
    ) {
        return findDependantBuildFiles(server, workspaceRelativePaths, universeRepos, repoMapping, null);
    }

    public static CompletableFuture<List<Label>> findDependantBuildFiles(
        BazelServerFacade server,
        Set<Path> workspaceRelativePaths,
        Set<RepoType> universeRepos,
        RepoMapping repoMapping,
        @Nullable TaskId taskId
    ) {
        if (workspaceRelativePaths.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        return server.query(
                new BazelQueryParams<>(
                    expressionOf(workspaceRelativePaths),
                    BazelQueryOutput.Labels.INSTANCE,
                    taskId,
                    true,
                    List.of(
                        BazelFlag.universeScope(universeScopeOf(universeRepos)),
                        BazelFlag.orderOutput(false),
                        BazelFlag.consistentLabels(true)
                    )
                )
            )
            .thenApply(result -> {
                // an output label looks like `@@dep_repo+//lib:BUILD.bazel`, and we want `@@dep_repo+//lib:all`
                List<Label> labels = new ArrayList<>();
                for (Label label : result.result()) {
                    if (label instanceof ResolvedLabel resolvedLabel) {
                        labels.add(new ResolvedLabel(resolvedLabel.repo(), resolvedLabel.packagePath(), AllRuleTargets.INSTANCE));
                    }
                }
                return removeDuplicatedLocalRepos(labels, repoMapping);
            });
    }

    public static String expressionOf(Set<Path> workspaceRelativePaths) {
        // consulo: Kotlin Path.invariantSeparatorsPathString
        return "rbuildfiles(" + workspaceRelativePaths.stream()
            .map(it -> "\"" + it.toString().replace(it.getFileSystem().getSeparator(), "/") + "\"")
            .collect(Collectors.joining(",")) + ")";
    }

    private static String universeScopeOf(Set<RepoType> universeRepos) {
        Set<RepoType> repos = new LinkedHashSet<>(universeRepos);
        repos.add(Main.INSTANCE);
        return repos.stream()
            .map(it -> new ResolvedLabel(it, new AllPackagesBeneath(List.of()), AllRuleTargetsAndFiles.INSTANCE).toString())
            .collect(Collectors.joining(","));
    }


    // remove package of the main repository that a local repository holds too,
    // local repository inside the workspace root gives every package of it two labels
    private static List<Label> removeDuplicatedLocalRepos(List<Label> receiver, RepoMapping repoMapping) {
        Collection<Path> localPaths = repoMapping instanceof BzlmodRepoMapping bzlmodRepoMapping
            ? bzlmodRepoMapping.getCanonicalRepoNameToLocalPath().values()
            : List.of();
        if (localPaths.isEmpty()) {
            return receiver;
        }
        List<Label> result = new ArrayList<>();
        for (Label label : receiver) {
            if (!(label instanceof ResolvedLabel resolved)) {
                result.add(label);
                continue;
            }
            boolean duplicated = resolved.repo() instanceof Main
                && localPaths.stream().anyMatch(it -> Label.toPath(resolved.packagePath()).startsWith(it));
            if (!duplicated) {
                result.add(label);
            }
        }
        return result;
    }
}
