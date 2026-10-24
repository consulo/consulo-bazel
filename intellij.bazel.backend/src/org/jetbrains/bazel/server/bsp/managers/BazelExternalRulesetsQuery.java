package org.jetbrains.bazel.server.bsp.managers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import consulo.logging.Logger;
import consulo.util.lang.ControlFlowException;
import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelProcessResult;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.bazelrunner.JsonProto.Attribute;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.commons.gson.BazelGson;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import static org.jetbrains.bazel.server.bzlmod.RepoMapping.rootRulesToNeededTransitiveRules;

public interface BazelExternalRulesetsQuery {
    /**
     * the list of returned ruleset names should be in the apparent form as they will be used in aspect files
     */
    CompletableFuture<List<String>> fetchExternalRulesetNames();

    final class BazelExternalRulesetsQueryImpl implements BazelExternalRulesetsQuery {
        private final TaskId myTaskId;
        private final BazelRunner myBazelRunner;
        private final boolean myIsBzlModEnabled;
        private final boolean myIsWorkspaceEnabled;
        private final BazelTaskEventsHandler myTaskEventsHandler;
        private final ProjectView myProjectView;
        private final RepoMapping myRepoMapping;

        public BazelExternalRulesetsQueryImpl(
            TaskId taskId,
            BazelRunner bazelRunner,
            boolean isBzlModEnabled,
            boolean isWorkspaceEnabled,
            BazelTaskEventsHandler taskEventsHandler,
            ProjectView projectView,
            RepoMapping repoMapping
        ) {
            myTaskId = taskId;
            myBazelRunner = bazelRunner;
            myIsBzlModEnabled = isBzlModEnabled;
            myIsWorkspaceEnabled = isWorkspaceEnabled;
            myTaskEventsHandler = taskEventsHandler;
            myProjectView = projectView;
            myRepoMapping = repoMapping;
        }

        @Override
        public CompletableFuture<List<String>> fetchExternalRulesetNames() {
            List<String> enabledRules = ProjectViewExtensions.getEnabledRules(myProjectView);
            if (!enabledRules.isEmpty()) {
                return new BazelEnabledRulesetsQueryImpl(enabledRules).fetchExternalRulesetNames();
            }
            return new BazelBzlModExternalRulesetsQueryImpl(
                myTaskId,
                myBazelRunner,
                myIsBzlModEnabled,
                myTaskEventsHandler,
                myProjectView,
                myRepoMapping
            ).fetchExternalRulesetNames().thenCompose(bzlModRulesets ->
                new BazelWorkspaceExternalRulesetsQueryImpl(
                    myTaskId,
                    myBazelRunner,
                    myIsWorkspaceEnabled,
                    myTaskEventsHandler,
                    myProjectView
                ).fetchExternalRulesetNames().thenApply(workspaceRulesets -> {
                    List<String> result = new ArrayList<>(bzlModRulesets);
                    result.addAll(workspaceRulesets);
                    return result;
                })
            );
        }
    }

    /**
     * Returns the rulesets from the `enabled_rules` project view section, and the rulesets that they need.
     * Bazel is not queried.
     */
    final class BazelEnabledRulesetsQueryImpl implements BazelExternalRulesetsQuery {
        private final List<String> myEnabledRules;

        public BazelEnabledRulesetsQueryImpl(List<String> enabledRules) {
            myEnabledRules = enabledRules;
        }

        @Override
        public CompletableFuture<List<String>> fetchExternalRulesetNames() {
            return CompletableFuture.completedFuture(fetchExternalRulesetNamesImpl());
        }

        public List<String> fetchExternalRulesetNamesImpl() {
            List<String> neededTransitiveRules = new ArrayList<>();
            for (String it : myEnabledRules) {
                List<String> rules = rootRulesToNeededTransitiveRules.get(it);
                if (rules != null) {
                    neededTransitiveRules.addAll(rules);
                }
            }
            Set<String> result = new LinkedHashSet<>(myEnabledRules);
            result.addAll(neededTransitiveRules);
            return new ArrayList<>(result);
        }
    }

    /**
     * Finds the external rulesets that a `WORKSPACE` file declares with `git_repository` or `http_archive`.
     * <p>
     * The query uses `--output=streamed_jsonproto`, so Bazel prints one target per line. The parser reads the lines one by
     * one, and the output size has no limit. See {@link #parseWorkspaceExternalRulesetNames}.
     */
    final class BazelWorkspaceExternalRulesetsQueryImpl implements BazelExternalRulesetsQuery {
        private final TaskId myTaskId;
        private final BazelRunner myBazelRunner;
        private final boolean myIsWorkspaceEnabled;
        private final BazelTaskEventsHandler myTaskEventsHandler;
        private final ProjectView myProjectView;

        public BazelWorkspaceExternalRulesetsQueryImpl(
            TaskId taskId,
            BazelRunner bazelRunner,
            boolean isWorkspaceEnabled,
            BazelTaskEventsHandler taskEventsHandler,
            ProjectView projectView
        ) {
            myTaskId = taskId;
            myBazelRunner = bazelRunner;
            myIsWorkspaceEnabled = isWorkspaceEnabled;
            myTaskEventsHandler = taskEventsHandler;
            myProjectView = projectView;
        }

        @Override
        public CompletableFuture<List<String>> fetchExternalRulesetNames() {
            if (!myIsWorkspaceEnabled) {
                return CompletableFuture.completedFuture(List.of());
            }
            else {
                BazelCommand command =
                    myBazelRunner.buildBazelCommand(myProjectView, builder ->
                        builder.query(query -> {
                            query.getTargets().add(Label.parse("//external:*"));
                            query.getOptions().addAll(QUERY_OPTIONS);
                        })
                    );

                return myBazelRunner.runBazelCommand(command, myTaskId, false)
                    .waitAndGetResult()
                    .thenApply(result -> {
                        if (result.isNotSuccess()) {
                            String queryFailedMessage = getQueryFailedMessage(result);
                            BazelTaskEventsHandler.asLogger(myTaskEventsHandler, myTaskId).warn(queryFailedMessage);
                            log.warn(queryFailedMessage);
                            return List.<String>of();
                        }
                        else {
                            return parseWorkspaceExternalRulesetNames(result.getStdoutLines());
                        }
                    });
            }
        }

        private static final Logger log = Logger.getInstance(BazelWorkspaceExternalRulesetsQueryImpl.class);

        /**
         * The query options. Only the `name` and the `generator_function` attributes are needed, so the other attributes
         * are not printed.
         */
        private static final List<String> QUERY_OPTIONS =
            List.of(
                "--output=streamed_jsonproto",
                "--order_output=no",
                "--proto:output_rule_attrs=name,generator_function"
            );

        /**
         * Parses the ndjson output of `bazel query //external:* --output=streamed_jsonproto`.
         * <p>
         * Each line holds one target in the `blaze_query.Target` JSON form. The result holds the `name` attribute of each
         * eligible rule, in the output order. A rule is eligible when its class contains `git_repository`, or when its
         * class contains `http_archive` and the rule is not an output of a macro other than an `http_archive` wrapper.
         * A blank line is skipped. A line that does not parse is logged and skipped, so it does not drop the other rules.
         */
        public static List<String> parseWorkspaceExternalRulesetNames(List<String> stdoutLines) {
            List<String> result = new ArrayList<>();
            for (String line : stdoutLines) {
                JsonProto.Target target = parseTarget(line);
                JsonProto.Rule rule = target != null ? target.rule() : null;
                if (rule == null || !isEligible(rule)) {
                    continue;
                }
                String name = attributeValue(rule, "name");
                if (name != null) {
                    result.add(name);
                }
            }
            return result;
        }

        private static JsonProto.@Nullable Target parseTarget(String line) {
            if (line.isBlank()) {
                return null;
            }
            try {
                JsonProto.Target target = BazelGson.bazelGson.fromJson(line, JsonProto.Target.class);
                return target != null && "RULE".equals(target.type()) ? target : null;
            }
            catch (Exception e) {
                // consulo: upstream rethrowControlFlowException(e)
                if (e instanceof ControlFlowException || e instanceof CancellationException) {
                    throw e;
                }
                log.warn("Failed to parse a query output line as json: " + line, e);
                return null;
            }
        }

        private static boolean isEligible(JsonProto.Rule receiver) {
            String ruleClass = receiver.ruleClass();
            if (ruleClass == null) {
                return false;
            }
            if (ruleClass.contains("git_repository")) {
                return true;
            }
            if (!ruleClass.contains("http_archive")) {
                return false;
            }
            // Bazel prints the default value of `generator_function`, an empty string, for a rule that no macro created.
            String generatorFunction = attributeValue(receiver, "generator_function");
            return generatorFunction == null || generatorFunction.isEmpty() || generatorFunction.contains("http_archive");
        }

        private static @Nullable String attributeValue(JsonProto.Rule receiver, String attributeName) {
            List<Attribute> attribute = receiver.attribute();
            if (attribute == null) {
                return null;
            }
            for (Attribute it : attribute) {
                if (it.name().equals(attributeName)) {
                    return it.stringValue();
                }
            }
            return null;
        }
    }

    final class BazelBzlModExternalRulesetsQueryImpl implements BazelExternalRulesetsQuery {
        private final TaskId myTaskId;
        private final BazelRunner myBazelRunner;
        private final boolean myIsBzlModEnabled;
        private final BazelTaskEventsHandler myTaskEventsHandler;
        private final ProjectView myProjectView;
        private final RepoMapping myRepoMapping;

        private final Gson myGson = BazelGson.bazelGson;

        public BazelBzlModExternalRulesetsQueryImpl(
            TaskId taskId,
            BazelRunner bazelRunner,
            boolean isBzlModEnabled,
            BazelTaskEventsHandler taskEventsHandler,
            ProjectView projectView,
            RepoMapping repoMapping
        ) {
            myTaskId = taskId;
            myBazelRunner = bazelRunner;
            myIsBzlModEnabled = isBzlModEnabled;
            myTaskEventsHandler = taskEventsHandler;
            myProjectView = projectView;
            myRepoMapping = repoMapping;
        }

        @Override
        public CompletableFuture<List<String>> fetchExternalRulesetNames() {
            if (!myIsBzlModEnabled) {
                return CompletableFuture.completedFuture(List.of());
            }
            BazelCommand command =
                myBazelRunner.buildBazelCommand(myProjectView, builder ->
                    builder.graph(graph -> {
                        graph.getOptions().add("--output=json");
                    })
                );
            return myBazelRunner
                .runBazelCommand(
                    command,
                    myTaskId,
                    false
                ).waitAndGetResult()
                .thenApply(result -> {
                    if (result.isNotSuccess()) {
                        String queryFailedMessage = getQueryFailedMessage(result);
                        BazelTaskEventsHandler.asLogger(myTaskEventsHandler, myTaskId).warn(queryFailedMessage);
                        log.warn(queryFailedMessage);

                        Label target = new Label.SyntheticLabel(Label.AllRuleTargets.INSTANCE);
                        List<PublishDiagnosticsParams> diagnostics =
                            new DiagnosticsService(myBazelRunner.getWorkspaceRoot())
                                .extractDiagnostics(result.getStderrLines(), target, myTaskId, true, false);
                        diagnostics.forEach(myTaskEventsHandler::onBuildPublishDiagnostics);
                    }
                    // best effort to parse the output even when there are errors
                    JsonElement parsedJson;
                    try {
                        parsedJson = JsonParser.parseReader(
                            new InputStreamReader(new ByteArrayInputStream(result.getStdout()), StandardCharsets.UTF_8)
                        );
                    }
                    catch (Exception e) {
                        log.error("Failed to parse string to json", e);
                        parsedJson = null;
                    }
                    JsonObject bzlmodGraphJson = parsedJson instanceof JsonObject jsonObject ? jsonObject : null;

                    try {
                        BzlmodGraph graph = myGson.fromJson(bzlmodGraphJson, BzlmodGraph.class);
                        List<String> directDependencyNames = graph.getAllDirectRulesetDependencyNames();
                        List<BzlmodDependency> indirectDeps = new ArrayList<>();
                        for (Map.Entry<String, List<String>> it : rootRulesToNeededTransitiveRules.entrySet()) {
                            if (!directDependencyNames.contains(it.getKey())) {
                                continue;
                            }
                            for (String transitiveDep : it.getValue()) {
                                indirectDeps.addAll(graph.includedByDirectDeps(it.getKey(), transitiveDep));
                            }
                        }
                        List<String> directDepsApparentNames = new ArrayList<>();
                        for (BzlmodDependency it : graph.dependencies()) {
                            String apparentName = it.toApparentName(myRepoMapping);
                            if (apparentName != null) {
                                directDepsApparentNames.add(apparentName);
                            }
                        }
                        List<String> indirectDepsApparentNames = new ArrayList<>();
                        for (BzlmodDependency it : indirectDeps) {
                            String apparentName = it.toApparentName(myRepoMapping);
                            if (apparentName != null) {
                                indirectDepsApparentNames.add(apparentName);
                            }
                        }
                        String graphName = graph.name();
                        String moduleName = graphName == null || graphName.isEmpty() ? null : graphName;
                        // We include the current module name to handle the edge case for the rules_kotlin repository.
                        // In this scenario, the Bazel module does not declare a dependency on rules_kotlin because the module itself *is* rules_kotlin.
                        // Without this, Kotlin support would not be enabled in this case, causing redcodes in the IDE.
                        Set<String> names = new LinkedHashSet<>(directDepsApparentNames);
                        names.addAll(indirectDepsApparentNames);
                        if (moduleName != null) {
                            names.add(moduleName);
                        }
                        return new ArrayList<>(names);
                    }
                    catch (Throwable e) {
                        log.warn("The returned bzlmod json is not parsable:\n" + bzlmodGraphJson, e);
                        return List.<String>of();
                    }
                });
        }

        private static final Logger log = Logger.getInstance(BazelBzlModExternalRulesetsQueryImpl.class);
    }

    private static String getQueryFailedMessage(BazelProcessResult result) {
        return "Bazel query failed with output:\n" + String.join("\n", result.getStderrLines());
    }

    record BzlmodDependency(
        String key,
        @Nullable String name,
        @Nullable String apparentName,
        List<BzlmodDependency> dependencies
    ) {
        public @Nullable String toApparentName(RepoMapping repoMapping) {
            if (apparentName != null) {
                return apparentName;
            }
            // Bazel 7.4.1 and lower don't provide apparentName in the graph JSON output
            if (!(repoMapping instanceof BzlmodRepoMapping bzlmodRepoMapping)) {
                return null;
            }
            int atIndex = key.indexOf('@');
            String keyBeforeAt = atIndex == -1 ? key : key.substring(0, atIndex);
            String apparentNameFromTilde = bzlmodRepoMapping.getCanonicalRepoNameToApparentName().get(keyBeforeAt + "~");
            return apparentNameFromTilde != null
                ? apparentNameFromTilde
                : bzlmodRepoMapping.getCanonicalRepoNameToApparentName().get(keyBeforeAt + "+");
        }
    }

    record BzlmodGraph(
        @Nullable String name,
        List<BzlmodDependency> dependencies
    ) {
        public List<String> getAllDirectRulesetDependencyNames() {
            List<String> result = new ArrayList<>();
            for (BzlmodDependency it : dependencies) {
                String name = it.name();
                if (name != null) {
                    result.add(name);
                }
            }
            return result;
        }

        public List<BzlmodDependency> includedByDirectDeps(String rootRulesetName, String transitiveRulesetName) {
            BzlmodDependency rootDependency = null;
            for (BzlmodDependency it : dependencies) {
                if (rootRulesetName.equals(it.name())) {
                    rootDependency = it;
                    break;
                }
            }
            // consulo: upstream ?.dependencies?.filter { ... } ?: emptyList(), Gson may leave dependencies null
            @Nullable List<BzlmodDependency> rootDependencies = rootDependency != null ? rootDependency.dependencies() : null;
            if (rootDependencies == null) {
                return List.of();
            }
            return rootDependencies.stream().filter(it -> transitiveRulesetName.equals(it.name())).toList();
        }
    }
}

/**
 * Representations of the messages from
 * https://github.com/bazelbuild/bazel/blob/master/src/main/protobuf/build.proto
 * in the JSON form that `--output=streamed_jsonproto` prints.
 */
final class JsonProto {
    private JsonProto() {
    }

    record Target(
        @Nullable String type,
        @Nullable Rule rule
    ) {
    }

    record Rule(
        @Nullable String name,
        @Nullable String ruleClass,
        @Nullable List<Attribute> attribute
    ) {
    }
}
