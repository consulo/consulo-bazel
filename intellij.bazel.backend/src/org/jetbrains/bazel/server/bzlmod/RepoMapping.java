package org.jetbrains.bazel.server.bzlmod;

import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.bazelrunner.ModuleResolver;
import org.jetbrains.bazel.bazelrunner.ModuleResolver.ShowRepoResult;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.RepoMappingDisabled;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.Canonical;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler.BazelTaskLogger;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

// consulo: holder class for the top-level declarations of upstream RepoMapping.kt
// (org.jetbrains.bazel.commons.RepoMapping is referenced by its qualified name in this file)
public final class RepoMapping {
    private RepoMapping() {
    }

    public static final Map<String, List<String>> rootRulesToNeededTransitiveRules;

    static {
        // consulo: insertion ordered like Kotlin mapOf
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("rules_kotlin", List.of("rules_java"));
        map.put("rules_scala", List.of("rules_java"));
        rootRulesToNeededTransitiveRules = Collections.unmodifiableMap(map);
    }

    public static CompletableFuture<org.jetbrains.bazel.commons.RepoMapping> calculateRepoNameMappingOnly(
        ProjectView projectView,
        BazelRunner bazelRunner,
        BazelInfo bazelInfo,
        BazelTaskLogger taskLogger,
        TaskId taskId
    ) {
        if (!bazelInfo.isBzlModEnabled()) {
            return CompletableFuture.completedFuture(RepoMappingDisabled.INSTANCE);
        }
        ModuleResolver moduleResolver = new ModuleResolver(bazelRunner, projectView, taskId);
        CompletableFuture<Map<String, String>> moduleApparentNameToCanonicalNameFuture;
        try {
            // empty string is the name of the root module
            moduleApparentNameToCanonicalNameFuture = moduleResolver.getRepoMappings(List.of("")).thenApply(it -> {
                Map<String, String> rootMapping = it.get("");
                return rootMapping != null ? rootMapping : Map.<String, String>of();
            });
        }
        catch (Exception e) {
            moduleApparentNameToCanonicalNameFuture = CompletableFuture.failedFuture(e);
        }

        return moduleApparentNameToCanonicalNameFuture
            .handle((moduleApparentNameToCanonicalName, error) -> {
                if (error != null) {
                    Throwable e = unwrap(error);
                    if (e instanceof CancellationException || !(e instanceof Exception)) {
                        return CompletableFuture.<org.jetbrains.bazel.commons.RepoMapping>failedFuture(e);
                    }
                    taskLogger.error(e.toString());
                    return CompletableFuture.<org.jetbrains.bazel.commons.RepoMapping>completedFuture(RepoMappingDisabled.INSTANCE);
                }

                List<String> canonicalNamesOfNotCoveredRules = new ArrayList<>();
                for (Map.Entry<String, List<String>> it : rootRulesToNeededTransitiveRules.entrySet()) {
                    if (!coveredByMap(it.getValue(), moduleApparentNameToCanonicalName)) {
                        String canonicalName = moduleApparentNameToCanonicalName.get(it.getKey());
                        if (canonicalName != null) {
                            canonicalNamesOfNotCoveredRules.add(canonicalName);
                        }
                    }
                }

                return moduleResolver.getRepoMappings(canonicalNamesOfNotCoveredRules).thenApply(repoMappings -> {
                    // consulo: upstream values.reduceOrNull { acc, map -> acc + map }.orEmpty()
                    Map<String, String> moduleApparentNameToCanonicalNameForNeededTransitiveRules = new LinkedHashMap<>();
                    for (Map<String, String> map : repoMappings.values()) {
                        moduleApparentNameToCanonicalNameForNeededTransitiveRules.putAll(map);
                    }

                    Map<String, String> apparentRepoNameToCanonicalName =
                        new LinkedHashMap<>(moduleApparentNameToCanonicalNameForNeededTransitiveRules);
                    apparentRepoNameToCanonicalName.putAll(moduleApparentNameToCanonicalName);
                    return (org.jetbrains.bazel.commons.RepoMapping) new BzlmodRepoMapping(
                        Map.of(),
                        apparentRepoNameToCanonicalName,
                        Map.of(),
                        Set.of()
                    );
                });
            })
            .thenCompose(Function.identity());
    }

    // consulo: local function of upstream calculateRepoNameMappingOnly
    private static boolean coveredByMap(
        List<String> repositoryNames,
        Map<String, String> mapping
    ) {
        return repositoryNames.stream().allMatch(mapping::containsKey);
    }

    public static CompletableFuture<org.jetbrains.bazel.commons.RepoMapping> extendRepoMappingByPathInfo(
        org.jetbrains.bazel.commons.RepoMapping nameMapping,
        ProjectView projectView,
        BazelRunner bazelRunner,
        BazelInfo bazelInfo,
        BazelTaskLogger taskLogger,
        Map<String, @Nullable ShowRepoResult> knownResolved,
        TaskId taskId
    ) {
        if (!(nameMapping instanceof BzlmodRepoMapping bzlmodNameMapping)) {
            return CompletableFuture.completedFuture(RepoMappingDisabled.INSTANCE);
        }
        Map<String, String> moduleApparentNameToCanonicalName = bzlmodNameMapping.getApparentRepoNameToCanonicalName();
        Map<String, ShowRepoResult> knownRepoDefinitions = new LinkedHashMap<>();
        for (ShowRepoResult value : knownResolved.values()) {
            if (value != null) {
                knownRepoDefinitions.put(value.name(), value);
            }
        }

        Map<String, Path> moduleCanonicalNameToLocalPath = new LinkedHashMap<>();
        Set<String> knownNonLocalRepoNames = new LinkedHashSet<>();
        ModuleResolver moduleResolver = new ModuleResolver(bazelRunner, projectView, taskId);

        List<String> known = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (String name : BazelRepoMappingUtils.getExternalRepositoriesTreatedAsInternal(projectView)) {
            String canonicalName = moduleApparentNameToCanonicalName.get(name);
            if (canonicalName != null && knownRepoDefinitions.containsKey(canonicalName)) {
                known.add(name);
            }
            else {
                unknown.add(name);
            }
        }
        List<@Nullable String> knownCanonicalNames = new ArrayList<>();
        for (String it : known) {
            knownCanonicalNames.add(moduleApparentNameToCanonicalName.get(it));
        }

        return moduleResolver.resolveModules(unknown, bazelInfo).thenApply(resolvedModulesAndWarning -> {
            Map<String, @Nullable ShowRepoResult> resolvedModules = new LinkedHashMap<>(resolvedModulesAndWarning.result());
            for (Map.Entry<String, ShowRepoResult> entry : knownRepoDefinitions.entrySet()) {
                if (knownCanonicalNames.contains(entry.getKey())) {
                    resolvedModules.put(entry.getKey(), entry.getValue());
                }
            }

            resolvedModules.forEach((externalRepo, showRepoResult) -> {
                try {
                    if (showRepoResult instanceof ShowRepoResult.LocalRepository localRepository) {
                        moduleCanonicalNameToLocalPath.put(localRepository.name(), Path.of(localRepository.path()));
                    }
                    else {
                        taskLogger.warn(
                            "Tried to import external module " + externalRepo + ", but it was not `local_path_override`: " + showRepoResult
                        );
                        knownNonLocalRepoNames.add(externalRepo);
                    }
                }
                catch (Exception e) {
                    taskLogger.error(e.toString());
                }
            });

            knownResolved.forEach((externalRepo, showRepoResult) -> {
                if (!(showRepoResult instanceof ShowRepoResult.LocalRepository)) {
                    knownNonLocalRepoNames.add(externalRepo);
                }
            });

            Map<String, Path> moduleCanonicalNameToPath = new LinkedHashMap<>(moduleCanonicalNameToLocalPath);
            for (String canonicalName : moduleApparentNameToCanonicalName.values()) {
                if (canonicalName.equals("")) {
                    moduleCanonicalNameToPath.put(canonicalName, bazelInfo.workspaceRoot());
                    continue;
                }
                Path localPath = moduleCanonicalNameToLocalPath.get(canonicalName);
                Path repoPath;
                if (localPath != null) {
                    repoPath = bazelInfo.workspaceRoot().resolve(localPath);
                }
                else {
                    // See https://bazel.build/external/overview#directory-layout
                    repoPath = bazelInfo.outputBase().resolve("external").resolve(canonicalName);
                }
                moduleCanonicalNameToPath.put(canonicalName, repoPath);
            }

            return new BzlmodRepoMapping(
                moduleCanonicalNameToLocalPath,
                moduleApparentNameToCanonicalName,
                moduleCanonicalNameToPath,
                knownNonLocalRepoNames
            );
        });
    }


    public static CompletableFuture<org.jetbrains.bazel.commons.RepoMapping> calculateRepoMapping(
        ProjectView projectView,
        BazelRunner bazelRunner,
        BazelInfo bazelInfo,
        BazelTaskLogger taskLogger,
        TaskId taskId
    ) {
        return calculateRepoNameMappingOnly(projectView, bazelRunner, bazelInfo, taskLogger, taskId).thenCompose(nameMapping ->
            extendRepoMappingByPathInfo(
                nameMapping,
                projectView, bazelRunner, bazelInfo, taskLogger, Map.of(), taskId
            )
        );
    }

    public static CompletableFuture<org.jetbrains.bazel.commons.RepoMapping> extendRepoMapping(
        BazelServerFacade server,
        org.jetbrains.bazel.commons.RepoMapping repoMapping,
        Collection<Label> targetLabels,
        TaskId taskId
    ) {
        return switch (repoMapping) {
            case RepoMappingDisabled repoMappingDisabled -> CompletableFuture.completedFuture(RepoMappingDisabled.INSTANCE);
            case BzlmodRepoMapping bzlmodRepoMapping -> {
                // If we discovered new repositories in the transitive dependencies, verify if some of
                // them are local repositories and update our mapping to local paths accordingly.
                // Additionally, for those newly discovered local repositories, update the path to
                // point to the source tree (rather than the output map).
                // consulo: upstream mapNotNull { ... }.distinct()
                Set<Canonical> distinctInvolvedRepos = new LinkedHashSet<>();
                for (Label it : targetLabels) {
                    if (it instanceof ResolvedLabel resolvedLabel && resolvedLabel.repo() instanceof Canonical canonical) {
                        distinctInvolvedRepos.add(canonical);
                    }
                }
                List<Canonical> involvedRepos = new ArrayList<>(distinctInvolvedRepos);
                List<String> needsPath = involvedRepos.stream()
                    .filter(it -> !(bzlmodRepoMapping.getNonLocalCanonicalRepoNames().contains(it.repoName())))
                    .filter(it -> !(bzlmodRepoMapping.getCanonicalRepoNameToLocalPath().containsKey(it.repoName())))
                    .map(Canonical::toString)
                    .toList();
                yield server.showRepos(needsPath, taskId).thenApply(showReposResult -> {
                    Map<String, @Nullable ShowRepoResult> extraRepositoryDescriptions = showReposResult.result();
                    // consulo: upstream map { ... }.reduceOrNull { acc, map -> acc + map }.orEmpty()
                    Map<String, Path> extraPaths = new LinkedHashMap<>();
                    extraRepositoryDescriptions.forEach((name, description) -> {
                        if (description instanceof ShowRepoResult.LocalRepository localRepository) {
                            extraPaths.put(localRepository.name(), Path.of(localRepository.path()));
                        }
                    });
                    Map<String, Path> extraPathsResolved = new LinkedHashMap<>();
                    extraPaths.forEach((name, path) -> extraPathsResolved.put(name, server.getBazelInfo().workspaceRoot().resolve(path)));
                    List<String> extraNonLocalCanonicalRepoNames = new ArrayList<>();
                    for (ShowRepoResult value : extraRepositoryDescriptions.values()) {
                        if (value != null && !(value instanceof ShowRepoResult.LocalRepository)) {
                            extraNonLocalCanonicalRepoNames.add(value.name());
                        }
                    }
                    // `bazel mod show_repo` does not describe every repository, for example `@@bazel_tools`.
                    // add such a repository as non-local, so that the next sync does not ask Bazel again.
                    List<String> undescribedCanonicalRepoNames = needsPath.stream()
                        .map(it -> it.startsWith("@@") ? it.substring("@@".length()) : it)
                        .filter(it -> !extraPaths.containsKey(it))
                        .toList();

                    Map<String, Path> canonicalRepoNameToLocalPath = new LinkedHashMap<>(bzlmodRepoMapping.getCanonicalRepoNameToLocalPath());
                    canonicalRepoNameToLocalPath.putAll(extraPaths);
                    Map<String, Path> canonicalRepoNameToPath = new LinkedHashMap<>(bzlmodRepoMapping.getCanonicalRepoNameToPath());
                    canonicalRepoNameToPath.putAll(extraPathsResolved);
                    Set<String> nonLocalCanonicalRepoNames = new LinkedHashSet<>(bzlmodRepoMapping.getNonLocalCanonicalRepoNames());
                    nonLocalCanonicalRepoNames.addAll(extraNonLocalCanonicalRepoNames);
                    nonLocalCanonicalRepoNames.addAll(undescribedCanonicalRepoNames);
                    return new BzlmodRepoMapping(
                        canonicalRepoNameToLocalPath,
                        bzlmodRepoMapping.getApparentRepoNameToCanonicalName(),
                        canonicalRepoNameToPath,
                        nonLocalCanonicalRepoNames
                    );
                });
            }
        };
    }

    // consulo: the failure of a CompletableFuture stage is wrapped into a CompletionException
    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }
}
