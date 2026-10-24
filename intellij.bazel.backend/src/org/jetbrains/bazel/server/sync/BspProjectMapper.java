package org.jetbrains.bazel.server.sync;

import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.ExcludableValue;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewTargets;
import org.jetbrains.bazel.server.BazelQuery;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryOutput;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryParams;
import org.jetbrains.bsp.protocol.JvmToolchainInfo;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.WorkspaceDirectories.WorkspaceDirectoriesResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BspProjectMapper {
    private final Path myWorkspaceRoot;
    private final BazelRunner myBazelRunner;
    private final ProjectView myProjectView;
    private final BazelPathsResolver myBazelPathsResolver;

    public BspProjectMapper(
        Path workspaceRoot,
        BazelRunner bazelRunner,
        ProjectView projectView,
        BazelPathsResolver bazelPathsResolver
    ) {
        myWorkspaceRoot = workspaceRoot;
        myBazelRunner = bazelRunner;
        myProjectView = projectView;
        myBazelPathsResolver = bazelPathsResolver;
    }

    public CompletableFuture<WorkspaceDirectoriesResult> workspaceDirectories(RepoMapping repoMapping, TaskId taskId) {
        Set<Path> excludedTechnicalDirectories = getTechnicalDirectoriesToExclude();
        Set<Path> included = new LinkedHashSet<>();
        Set<Path> excluded = new LinkedHashSet<>();

        List<ExcludableValue<Path>> directories = ProjectViewTargets.getDirectories(myProjectView);
        for (ExcludableValue<Path> directory : directories) {
            Path relativeToWorkspaceRoot = myWorkspaceRoot.resolve(directory.value()).normalize();
            switch (directory) {
                case ExcludableValue.Included<Path> it -> included.add(relativeToWorkspaceRoot);
                case ExcludableValue.Excluded<Path> it -> excluded.add(relativeToWorkspaceRoot);
            }
        }

        Set<Path> includedFromTargets = new LinkedHashSet<>();
        Set<Path> excludedFromTargets = new LinkedHashSet<>();
        // If we include (exclude) a directory by target, we want to add (remove) only directories of it's package
        // and not subpackages, so we need to additionally include (exclude) them.
        Set<Path> excludedAdditionally = new LinkedHashSet<>();
        Set<Path> includedAdditionally = new LinkedHashSet<>();

        return getSubPackages(myProjectView, repoMapping, taskId).thenApply(allSubPackages -> {
            for (ExcludableValue<Label> target : ProjectViewTargets.getTargets(myProjectView)) {
                Path path = toDirectoryPath(Label.assumeResolved(target.value()), repoMapping);
                List<Path> subPackages = allSubPackages.stream().filter(it -> it.startsWith(path) && !it.equals(path)).toList();
                Set<Path> targetSet;
                Set<Path> additionalSet;
                switch (target) {
                    case ExcludableValue.Included<Label> it -> {
                        targetSet = includedFromTargets;
                        additionalSet = excludedAdditionally;
                    }
                    case ExcludableValue.Excluded<Label> it -> {
                        targetSet = excludedFromTargets;
                        additionalSet = includedAdditionally;
                    }
                }

                targetSet.add(path);
                if (target.value().isRecursive()) {
                    targetSet.addAll(subPackages);
                }
                else {
                    additionalSet.addAll(subPackages);
                }
            }

            // We need a hierarchy of collected sets to avoid cases when one directory is both included and excluded.
            // The sets are prioritized according to the following order (from lowest to highest):
            // includedAdditionally < excludedAdditionally < included < excluded < includedFromTargets < excludedFromTargets < excludedTechnicalDirectories.
            //
            // Note that 'included' and 'excluded' sets (derived from the 'directories' section of the .bazelproject file)
            // are matched resursively, meaning that if a directory is included, all its subdirectories are also
            // included (unless overwritten by a higher priority set).
            // All other sets (derived from the 'targets' section) are matched exactly.
            //
            // This hierarchy ensures that:
            // 1. Targets section items are stronger than directories section items (e.g., including a target
            //    overwrites an exclusion of its parent directory).
            // 2. Excludes within the same section (directories or targets) overwrite includes.
            // 3. Subpackages of non-recursive targets (included/excluded additionally) have the lowest priority,
            //    ensuring they are only used as a fallback if no other rule applies.
            //
            // Note that if 'derive_targets_from_directories' is true, directories are also present in the targets
            // sets, which effectively moves them to a higher priority position.
            new DirsPriority()
                .addLast(includedAdditionally)
                .addLast(excludedAdditionally)
                .addLast(included, true)
                .addLast(excluded, true)
                .addLast(includedFromTargets)
                .addLast(excludedFromTargets)
                .addLast(excludedTechnicalDirectories, true)
                .removeShadowedPaths();

            Set<Path> includedDirectories = new LinkedHashSet<>(included);
            includedDirectories.addAll(includedFromTargets);
            includedDirectories.addAll(includedAdditionally);
            Set<Path> excludedDirectories = new LinkedHashSet<>(excluded);
            excludedDirectories.addAll(excludedFromTargets);
            excludedDirectories.addAll(excludedAdditionally);
            excludedDirectories.addAll(excludedTechnicalDirectories);
            return new WorkspaceDirectoriesResult(
                new ArrayList<>(includedDirectories),
                new ArrayList<>(excludedDirectories)
            );
        });
    }

    private CompletableFuture<List<Path>> getSubPackages(ProjectView projectView, RepoMapping repoMapping, TaskId taskId) {
        List<ResolvedLabel> patterns =
            ProjectViewTargets.getTargets(projectView).stream().map(it -> Label.assumeResolved(it.value())).distinct().toList();
        return runBazelBuildfilesQuery(projectView, patterns, taskId).thenApply(out -> {
            if (out.isBlank()) {
                return List.<Path>of();
            }

            List<Path> result = new ArrayList<>();
            for (String line : out.lines().toList()) {
                if (line.isBlank()) {
                    continue;
                }

                Label label = Label.parseOrNull(line.trim());
                if (label == null) {
                    continue;
                }
                Path labelPath = toDirectoryPath(Label.assumeResolved(label), repoMapping);
                Path dirPath = Files.isRegularFile(labelPath) ? Objects.requireNonNull(labelPath.getParent()) : labelPath;
                result.add(dirPath);
            }
            return result;
        });
    }

    private Path toDirectoryPath(ResolvedLabel receiver, RepoMapping repoMapping) {
        return myBazelPathsResolver.toDirectoryPath(receiver, repoMapping).normalize();
    }

    private CompletableFuture<String> runBazelBuildfilesQuery(ProjectView projectView, List<ResolvedLabel> patterns, TaskId taskId) {
        if (patterns.isEmpty()) {
            return CompletableFuture.completedFuture("");
        }
        StringBuilder exprBuilder = new StringBuilder();
        exprBuilder.append("buildfiles(set(");
        for (int i = 0; i < patterns.size(); i++) {
            if (i > 0) {
                exprBuilder.append(" ");
            }
            exprBuilder.append("\"").append(patterns.get(i)).append("\"");
        }
        exprBuilder.append("))");
        String expr = exprBuilder.toString();

        return BazelQuery.runBazelQuery(myBazelRunner, projectView, new BazelQueryParams<>(expr, new BazelQueryOutput.Raw(), taskId))
            .thenApply(result -> {
                if (result.status() != BazelStatus.SUCCESS) {
                    throw new RuntimeException("bazel query failed: " + String.join("\n", result.stderrLines()));
                }
                return result.result();
            });
    }

    // bazel symlinks exclusion logic is taken care by BazelSymlinkExcludeService
    private Set<Path> getTechnicalDirectoriesToExclude() {
        Set<Path> result = new LinkedHashSet<>();
        result.add(myWorkspaceRoot.resolve(Constants.DOT_BAZELBSP_DIR_NAME));
        return result;
    }

    public CompletableFuture<JvmToolchainInfo> jvmBuilderParamsForTarget(Label target) {
        return JvmToolchainQuery.jvmToolchainQueryForTarget(myBazelRunner, myProjectView, target);
    }


    private static class DirsPriority {
        private final LinkedList<PathSet> myPrioritizedPathSets = new LinkedList<>();

        DirsPriority addLast(Set<Path> paths) {
            return addLast(paths, false);
        }

        DirsPriority addLast(Set<Path> paths, boolean isRecursive) {
            myPrioritizedPathSets.addLast(new PathSet(paths, isRecursive));
            return this;
        }

        void removeShadowedPaths() {
            for (int i = 0; i < myPrioritizedPathSets.size(); i++) {
                PathSet current = myPrioritizedPathSets.get(i);
                List<PathSet> higherPrioritySets = myPrioritizedPathSets.subList(i + 1, myPrioritizedPathSets.size());
                removeShadowedBy(current.getPaths(), higherPrioritySets);
            }
        }

        private static boolean removeShadowedBy(Set<Path> receiver, List<PathSet> higherPrioritySets) {
            return receiver.removeIf(path ->
                higherPrioritySets.stream().anyMatch(higherPrioritySet -> containsPath(higherPrioritySet, path))
            );
        }

        private static boolean containsPath(PathSet receiver, Path otherPath) {
            if (receiver.isRecursive()) {
                return receiver.getPaths().stream().anyMatch(otherPath::startsWith);
            }
            else {
                return receiver.getPaths().contains(otherPath);
            }
        }

        private static class PathSet {
            private final Set<Path> myPaths;
            private final boolean myIsRecursive;

            PathSet(Set<Path> paths, boolean isRecursive) {
                myPaths = paths;
                myIsRecursive = isRecursive;
            }

            Set<Path> getPaths() {
                return myPaths;
            }

            boolean isRecursive() {
                return myIsRecursive;
            }
        }
    }
}
