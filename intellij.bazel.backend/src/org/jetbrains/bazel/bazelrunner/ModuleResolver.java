package org.jetbrains.bazel.bazelrunner;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import consulo.platform.Platform;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.gson.BazelGson;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModuleResolver {
    // consulo: upstream top-level declarations ShowRepoResult, ResolvedModulesAndWarning and ModuleOutputParser of
    // ModuleResolver.kt are nested here

    public sealed interface ShowRepoResult
        permits ShowRepoResult.LocalRepository, ShowRepoResult.HttpArchiveRepository, ShowRepoResult.Unknown {
        String name();

        /**
         * ## @community:
         * # <builtin>
         * local_repository(
         *   name = "community~",
         *   path = "community",
         * )
         * # Rule community~ instantiated at (most recent call last):
         * #   <builtin> in <toplevel>
         */
        record LocalRepository(String name, String path) implements ShowRepoResult {
        }

        record HttpArchiveRepository(String name, List<String> urls, String stripPrefix) implements ShowRepoResult {
        }

        /**
         * Any other output that doesn't match the expected format but contains the name of the module.
         */
        record Unknown(String name) implements ShowRepoResult {
        }
    }

    public record ResolvedModulesAndWarning(Map<String, @Nullable ShowRepoResult> result, List<String> warnings) {
        public static class Builder {
            private final Map<String, @Nullable ShowRepoResult> myResult;
            private final List<String> myWarnings;

            // consulo: overload for the upstream default arguments
            public Builder() {
                this(Map.of(), List.of());
            }

            public Builder(
                Map<String, @Nullable ShowRepoResult> result,
                List<String> warnings
            ) {
                myResult = new LinkedHashMap<>(result);
                myWarnings = new ArrayList<>(warnings);
            }

            public Builder update(ResolvedModulesAndWarning other) {
                myResult.putAll(other.result());
                myWarnings.addAll(other.warnings());
                return this;
            }

            public ResolvedModulesAndWarning build() {
                return new ResolvedModulesAndWarning(myResult, myWarnings);
            }
        }

        public Builder builder() {
            return new Builder(result, warnings);
        }
    }

    public static class ModuleOutputParser {
        private String extractAttribute(List<String> lines, String attributeName) {
            return substringBefore(
                substringAfter(
                    lines.stream().filter(it -> it.contains(attributeName + " = ")).findFirst().orElseThrow(),
                    attributeName + " = \""
                ),
                "\","
            ).trim();
        }

        private List<String> extractAttributeList(List<String> lines, String attributeName) {
            String line = lines.stream().filter(it -> it.contains(attributeName + " = ")).findFirst().orElseThrow();
            String list = substringBefore(substringAfter(line, attributeName + " = ["), "]");
            List<String> result = new ArrayList<>();
            for (String it : list.split(",", -1)) {
                result.add(removeSuffix(removePrefix(it.trim(), "\""), "\""));
            }
            return result;
        }

        private Map<String, List<String>> splitInfoGroups(List<String> lines) {
            Map<String, List<String>> groups = new LinkedHashMap<>();
            List<String> currentGroup = new ArrayList<>();
            @Nullable String currentGroupName = null;
            for (String line : lines) {
                if (line.startsWith("## ")) {
                    if (currentGroupName != null) {
                        groups.put(currentGroupName, currentGroup);
                    }
                    currentGroup = new ArrayList<>();
                    currentGroupName = substringAfter(line, "## ").trim();
                    if (currentGroupName.endsWith(":")) {
                        currentGroupName = currentGroupName.substring(0, currentGroupName.length() - 1);
                    }
                }
                currentGroup.add(line);
            }
            if (currentGroupName != null) {
                groups.put(currentGroupName, currentGroup);
            }
            return groups;
        }

        public @Nullable ShowRepoResult parseShowRepoStanza(List<String> stanza) {
            try {
                // No matter how the repository is defined, it will always have a name
                String name = extractAttribute(stanza, "name");

                boolean isLocalRepository = stanza.stream().anyMatch(it -> it.contains("local_repository("));
                if (isLocalRepository) {
                    String path = extractAttribute(stanza, "path");
                    return new ShowRepoResult.LocalRepository(name, path);
                }
                else if (stanza.stream().anyMatch(it -> it.contains("http_archive"))) {
                    List<String> urls = extractAttributeList(stanza, "urls");
                    String stripPrefix = extractAttribute(stanza, "strip_prefix");
                    return new ShowRepoResult.HttpArchiveRepository(name, urls, stripPrefix);
                }
                else {
                    return new ShowRepoResult.Unknown(name);
                }
            }
            catch (Exception e) {
                return null;
            }
        }

        public ResolvedModulesAndWarning parseJsonRepoDescription(String jsonText) {
            if (jsonText.isEmpty()) {
                return new ResolvedModulesAndWarning(Map.of(), List.of()); // ignore empty lines that might be included in the output
            }

            try {
                JsonProto.Repository description = BazelGson.bazelGson.fromJson(jsonText, JsonProto.Repository.class);
                @Nullable String canonicalName = description.canonicalName();
                if (canonicalName == null) {
                    return new ResolvedModulesAndWarning(Map.of(), List.of("Incomplete Respository Description " + description));
                }
                @Nullable String moduleKey = description.moduleKey();
                String key = moduleKey != null ? moduleKey : canonicalName;

                if ("local_repository".equals(description.repoRuleName())) {
                    List<String> pathValues = description.attribute().stream()
                        .filter(it -> "path".equals(it.name()))
                        .map(JsonProto.Attribute::stringValue)
                        .filter(Objects::nonNull)
                        .toList();
                    if (pathValues.isEmpty()) {
                        return new ResolvedModulesAndWarning(
                            Map.of(key, new ShowRepoResult.Unknown(canonicalName)),
                            List.of()
                        );
                    }
                    return new ResolvedModulesAndWarning(
                        Map.of(key, new ShowRepoResult.LocalRepository(canonicalName, pathValues.get(pathValues.size() - 1))),
                        List.of()
                    );
                }
                else if ("http_archive".equals(description.repoRuleName())) {
                    return new ResolvedModulesAndWarning(
                        Map.of(
                            key, new ShowRepoResult.HttpArchiveRepository(
                                canonicalName,
                                description.attribute().stream()
                                    .filter(it -> "urls".equals(it.name()))
                                    .flatMap(it -> {
                                        List<String> stringListValue = it.stringListValue();
                                        return stringListValue != null ? stringListValue.stream() : List.<String>of().stream();
                                    })
                                    .toList(),
                                description.attribute().stream()
                                    .filter(it -> "strip_prefix".equals(it.name()))
                                    .map(JsonProto.Attribute::stringValue)
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse("")
                            )
                        ),
                        List.of()
                    );
                }
                return new ResolvedModulesAndWarning(Map.of(key, new ShowRepoResult.Unknown(canonicalName)), List.of());
            }
            catch (Throwable ex) {
                return new ResolvedModulesAndWarning(Map.of(), List.of("Could not parse repository json description: " + jsonText));
            }
        }

        public ResolvedModulesAndWarning parseShowRepoResults(BazelProcessResult bazelProcessResult, boolean isJson) {
            if (!isJson) {
                Map<String, @Nullable ShowRepoResult> result = new LinkedHashMap<>();
                splitInfoGroups(bazelProcessResult.getStdoutLines())
                    .forEach((name, stanza) -> result.put(name, parseShowRepoStanza(stanza)));
                return new ResolvedModulesAndWarning(result, List.of());
            }
            // The output is new-line-delimited JSON, i.e., each line is a JSON description of one repository.
            ResolvedModulesAndWarning.Builder builder = new ResolvedModulesAndWarning.Builder(Map.of(), List.of());
            bazelProcessResult.getStdoutLines().stream()
                .map(this::parseJsonRepoDescription)
                .toList()
                .forEach(builder::update);
            return builder.build();
        }

        // consulo: Kotlin String.substringAfter (returns the string itself when the delimiter is missing)
        private static String substringAfter(String value, String delimiter) {
            int index = value.indexOf(delimiter);
            return index == -1 ? value : value.substring(index + delimiter.length());
        }

        // consulo: Kotlin String.substringBefore (returns the string itself when the delimiter is missing)
        private static String substringBefore(String value, String delimiter) {
            int index = value.indexOf(delimiter);
            return index == -1 ? value : value.substring(0, index);
        }

        // consulo: Kotlin String.removePrefix
        private static String removePrefix(String value, String prefix) {
            return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
        }

        // consulo: Kotlin String.removeSuffix
        private static String removeSuffix(String value, String suffix) {
            return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
        }
    }

    private final BazelRunner myBazelRunner;
    private final ProjectView myProjectView;
    private final TaskId myTaskId;

    private final Gson myGson = BazelGson.bazelGson;
    private final ModuleOutputParser myModuleOutputParser = new ModuleOutputParser();

    public ModuleResolver(
        BazelRunner bazelRunner,
        ProjectView projectView,
        TaskId taskId
    ) {
        myBazelRunner = bazelRunner;
        myProjectView = projectView;
        myTaskId = taskId;
    }

    /**
     * The name can be @@repo, @repo or repo. It will be resolved in the context of the main workspace.
     */
    public CompletableFuture<ResolvedModulesAndWarning> resolveModules(List<String> unsortedModuleNames, BazelInfo bazelInfo) {
        if (unsortedModuleNames.isEmpty()) {
            return CompletableFuture.completedFuture(new ResolvedModulesAndWarning(Map.of(),
                List.of())); // avoid bazel call if no information is needed
        }
        List<String> moduleNames = unsortedModuleNames.stream().sorted().distinct().toList();
        boolean supportsAllRequest =
            (bazelInfo.release().major() >= 9 || (bazelInfo.release().major() == 8 && bazelInfo.release().minor() >= 6));
        // The `--all_repos` output names each repository by its canonical name, so only canonical requests can be matched.
        boolean allNamesCanonical = moduleNames.stream().allMatch(it -> it.startsWith("@@"));
        if (supportsAllRequest && allNamesCanonical && moduleNames.size() > USE_ALL_THRESHOLD) {
            // `--all_repos` returns all repositories of the module graph, so keep only the requested ones.
            return resolveOneModuleBatch(moduleNames, bazelInfo, true).thenApply(it -> onlyRepositories(it, moduleNames));
        }
        ResolvedModulesAndWarning.Builder builder = new ResolvedModulesAndWarning.Builder();
        // consulo: the batches are resolved one after another, like the upstream sequential suspend calls
        CompletableFuture<?> batches = CompletableFuture.completedFuture(null);
        for (List<String> it : batchModules(moduleNames)) {
            batches = batches
                .thenCompose(ignored -> resolveOneModuleBatch(it, bazelInfo, false))
                .thenApply(builder::update);
        }
        return batches.thenApply(ignored -> builder.build());
    }

    /**
     * Resolve one batch of modules.
     */
    private CompletableFuture<ResolvedModulesAndWarning> resolveOneModuleBatch(
        List<String> moduleNames,
        BazelInfo bazelInfo,
        boolean requestAll
    ) {
        List<String> currentModuleNames = moduleNames;
        Map<String, @Nullable ShowRepoResult> excludedRepos = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        // consulo: the upstream `while (true)` loop is a recursion over futures, `continue` is the recursive call
        return resolveOneModuleBatchIteration(currentModuleNames, bazelInfo, requestAll, excludedRepos, warnings);
    }

    // consulo: one iteration of the upstream `while (true)` loop of resolveOneModuleBatch
    private CompletableFuture<ResolvedModulesAndWarning> resolveOneModuleBatchIteration(
        List<String> currentModuleNames,
        BazelInfo bazelInfo,
        boolean requestAll,
        Map<String, @Nullable ShowRepoResult> excludedRepos,
        List<String> warnings
    ) {
        boolean json_output = bazelInfo.release().major() >= 9;
        BazelCommand command =
            myBazelRunner.buildBazelCommand(myProjectView, builder ->
                builder.showRepo(showRepo -> {
                    if (json_output) {
                        showRepo.getOptions().add("--output=streamed_jsonproto");
                    }
                    if (requestAll) {
                        showRepo.getOptions().add("--all_repos");
                    }
                    else {
                        showRepo.getOptions().addAll(currentModuleNames);
                    }
                })
            );
        return myBazelRunner
            .runBazelCommand(command, myTaskId, false)
            .waitAndGetResult()
            .thenCompose(processResult -> {
                if (bazelInfo.isWorkspaceEnabled() && processResult.isNotSuccess()) {
                    // work around https://github.com/bazelbuild/bazel/issues/28601
                    // Parse the error to identify the bad repo and retry the batch without it,
                    // falling back to per-repo resolution for unrecognized errors.
                    @Nullable String badRepo = extractBadRepoFromError(processResult);
                    if (badRepo != null && currentModuleNames.size() > 1 && currentModuleNames.contains(badRepo)) {
                        excludedRepos.put(badRepo, null);
                        warnings.add("Bazel failed to show_repo " + badRepo + ": excluded from batch and skipped");
                        List<String> nextModuleNames = currentModuleNames.stream().filter(it -> !it.equals(badRepo)).toList();
                        if (requestAll) {
                            // When switching from requesting all repositories to requesting individual repositories,
                            // we have to ensure we're not exceeding limits and batch accordingly.
                            ResolvedModulesAndWarning.Builder builder =
                                new ResolvedModulesAndWarning.Builder().update(new ResolvedModulesAndWarning(excludedRepos, warnings));
                            CompletableFuture<?> batches = CompletableFuture.completedFuture(null);
                            for (List<String> it : batchModules(nextModuleNames)) {
                                batches = batches
                                    .thenCompose(ignored -> resolveOneModuleBatch(it, bazelInfo, false))
                                    .thenApply(builder::update);
                            }
                            return batches.thenApply(ignored -> builder.build());
                        }
                        // continue
                        return resolveOneModuleBatchIteration(nextModuleNames, bazelInfo, requestAll, excludedRepos, warnings);
                    }
                    if (currentModuleNames.size() == 1) {
                        // Failure of a request asking for a single repository, so we just answer that we don't know.
                        excludedRepos.put(currentModuleNames.get(0), null);
                        warnings.add("Bazel failed to show_repo " + currentModuleNames.get(0) + ":\n" +
                            String.join("\n", processResult.getStdoutLines()));
                        return CompletableFuture.completedFuture(new ResolvedModulesAndWarning(excludedRepos, warnings));
                    }
                    return resolveModulesIndividually(currentModuleNames, bazelInfo).thenApply(individualResults ->
                        excludedRepos.isEmpty()
                            ? individualResults
                            : individualResults.builder().update(new ResolvedModulesAndWarning(excludedRepos, warnings)).build()
                    );
                }

                ResolvedModulesAndWarning it = myModuleOutputParser.parseShowRepoResults(
                    processResult,
                    json_output
                );
                ResolvedModulesAndWarning parsed;
                if (processResult.isSuccess()) {
                    parsed = it;
                }
                else {
                    StringBuilder warning = new StringBuilder();
                    warning.append("Project depends on broken modules; bazel failed to show_repo ");
                    warning.append(requestAll ? "--all_repos" : String.join(", ", currentModuleNames));
                    warning.append('\n');
                    warning.append(String.join("\n", processResult.getStderrLines()));
                    List<String> newWarnings = new ArrayList<>();
                    newWarnings.add(warning.toString());
                    newWarnings.addAll(it.warnings());
                    parsed = new ResolvedModulesAndWarning(it.result(), newWarnings);
                }
                return CompletableFuture.completedFuture(
                    excludedRepos.isEmpty()
                        ? parsed
                        : parsed.builder().update(new ResolvedModulesAndWarning(excludedRepos, warnings)).build()
                );
            });
    }

    /**
     * Keeps the repositories in {@code canonicalModuleNames}. A described repository matches by its canonical name.
     * A `null` entry marks a requested repository that Bazel could not describe, so it matches by its key.
     */
    private ResolvedModulesAndWarning onlyRepositories(ResolvedModulesAndWarning receiver, List<String> canonicalModuleNames) {
        Set<String> names = new HashSet<>(canonicalModuleNames);
        Map<String, @Nullable ShowRepoResult> result = new LinkedHashMap<>();
        receiver.result().forEach((key, repository) -> {
            if (repository == null ? names.contains(key) : names.contains("@@" + repository.name())) {
                result.put(key, repository);
            }
        });
        return new ResolvedModulesAndWarning(result, receiver.warnings());
    }

    private CompletableFuture<ResolvedModulesAndWarning> resolveModulesIndividually(List<String> moduleNames, BazelInfo bazelInfo) {
        ResolvedModulesAndWarning.Builder builder = new ResolvedModulesAndWarning.Builder();
        // consulo: the modules are resolved one after another, like the upstream sequential suspend calls
        CompletableFuture<?> modules = CompletableFuture.completedFuture(null);
        for (String it : moduleNames) {
            modules = modules
                .thenCompose(ignored -> resolveOneModuleBatch(List.of(it), bazelInfo, false))
                .thenApply(builder::update);
        }
        return modules.thenApply(ignored -> builder.build());
    }

    /**
     * Obtains the mappings from apparent repo names to canonical repo names in the context of `canonicalRepoNames`.
     * <p>
     * <canonicalRepoNames> is a list of canonical repo names without any leading @ characters.
     * The canonical repo name of the root module repository is the empty string.
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Map<String, String>>> getRepoMappings(List<String> unsortedCanonicalRepoNames) {
        if (unsortedCanonicalRepoNames.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }
        List<String> canonicalRepoNames = unsortedCanonicalRepoNames.stream().sorted().toList();
        canonicalRepoNames.forEach(it -> {
            if (it.startsWith("@")) {
                throw new IllegalStateException("Canonical repo name cannot contain '@' characters: " + it);
            }
        });

        BazelCommand command =
            myBazelRunner.buildBazelCommand(myProjectView, builder ->
                builder.dumpRepoMapping(dumpRepoMapping -> {
                    dumpRepoMapping.getOptions().addAll(canonicalRepoNames);
                })
            );
        return myBazelRunner
            .runBazelCommand(command, myTaskId, false)
            .waitAndGetResult()
            .thenApply(processResult -> {
                if (processResult.isNotSuccess()) {
                    // dumpRepoMapping was added in Bazel 7.1.0, so it's going to fail with 7.0.0: https://github.com/bazelbuild/bazel/issues/20972
                    throw new IllegalStateException(
                        "dumpRepoMapping failed with output: " + String.join("\n", processResult.getStdoutLines())
                    );
                }
                try {
                    List<Map<String, String>> maps = processResult.getStdoutLines().stream()
                        .map(line -> (Map<String, String>) myGson.fromJson(line, Map.class))
                        .toList();
                    // consulo: Kotlin zip(canonicalRepoNames).associate { (map, canonicalRepoName) -> canonicalRepoName to map }
                    Map<String, Map<String, String>> result = new LinkedHashMap<>();
                    for (int i = 0; i < Math.min(maps.size(), canonicalRepoNames.size()); i++) {
                        result.put(canonicalRepoNames.get(i), maps.get(i));
                    }
                    return result;
                }
                catch (Throwable ex) {
                    throw new Error(
                        "Failed to parse repo mapping from bazel. Bazel output:\n" + String.join("\n", processResult.getStdoutLines()),
                        ex
                    );
                }
            });
    }

    private List<List<String>> batchModules(List<String> modules) {
        if (!Platform.current().os().isWindows()) {
            // consulo: Kotlin List.chunked
            return Lists.partition(modules, BATCH_SIZE_COUNT_POSIX);
        }
        List<List<String>> batches = new ArrayList<>();
        List<String> currentBatch = new ArrayList<>();
        int currentBatchSize = 0;
        for (String moduleName : modules) {
            int moduleLength = moduleName.length() + 3; // account for quoting and separating space
            if (currentBatchSize + moduleLength < BATCH_SIZE_CHARS_WINDOWS) {
                currentBatch.add(moduleName);
                currentBatchSize += moduleLength;
            }
            else {
                batches.add(currentBatch);
                currentBatch = new ArrayList<>(List.of(moduleName));
                currentBatchSize = moduleLength;
            }
        }
        if (currentBatch.size() > 0) {
            batches.add(currentBatch);
        }
        return batches;
    }

    private static final List<Pattern> BAD_REPO_PATTERNS = List.of(
        Pattern.compile("In repo argument ([^\\s:]+): no such repo\\."),
        Pattern.compile("In repo argument ([^\\s:]+): No repo visible as .* from @.* repository .*"),
        Pattern.compile("In repo argument ([^\\s:]+): Module .* does not exist in the dependency graph\\..*")
    );

    public static @Nullable String extractBadRepoFromError(BazelProcessResult processResult) {
        for (String line : processResult.getStderrLines()) {
            for (Pattern it : BAD_REPO_PATTERNS) {
                Matcher matcher = it.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

    // Total size of the repository names that can fit into one batch.
    // Background: on Windows, the total command-line length is limited to 8191 characters, see
    // https://learn.microsoft.com/en-us/troubleshoot/windows-client/shell-experience/command-line-string-limitation
    // Let's leave roughly 2k characters for the (possibly qualified) path of the bazel binary, as well as other flags and arguments.
    public static final int BATCH_SIZE_CHARS_WINDOWS = 6_000;

    // On non-windows systems, the length of the argument vector typically is the limiting factor. POSIX guarantees that limit
    // to be at least 4096. We have 3 arguments (bazel mod show_repo) as well as a couple of flags. Let's leave a bit of headroom.
    public static final int BATCH_SIZE_COUNT_POSIX = 4_000;

    // Heuristically chosen threshold where requesting all repositories does not take much longer than requesting
    // this number of repositories specifically. Should be small enough that, assuming typical repository names, this
    // number of repositories does not exceed the batch length declared above.
    public static final int USE_ALL_THRESHOLD = 64;
}
