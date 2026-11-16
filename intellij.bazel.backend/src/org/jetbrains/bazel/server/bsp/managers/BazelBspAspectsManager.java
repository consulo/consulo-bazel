package org.jetbrains.bazel.server.bsp.managers;

import com.intellij.aspect.lib.AspectConfig;
import com.intellij.aspect.lib.Deploy;
import com.intellij.aspect.lib.Rules;
import org.jetbrains.bazel.bazelrunner.ModuleResolver.ShowRepoResult;
import org.jetbrains.bazel.bazelrunner.params.BazelFlag;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.RepoMapping;
import consulo.bazel.aspect.BazelAspectModule;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.commons.TargetCollection;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.server.bep.BepOutput;
import org.jetbrains.bazel.server.bep.BepOutputBuilder;
import org.jetbrains.bazel.server.sync.ExecuteService;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;
import org.jetbrains.bsp.protocol.TaskId;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BazelBspAspectsManager {
    public record BazelBspAspectsManagerResult(BepOutput bepOutput, BazelStatus status) {
        public boolean isFailure() {
            return status != BazelStatus.SUCCESS;
        }

        public BazelBspAspectsManagerResult renameNamedSets(int runNumber) {
            return new BazelBspAspectsManagerResult(bepOutput.renameNamedSets(runNumber), status);
        }

        public BazelBspAspectsManagerResult merge(BazelBspAspectsManagerResult anotherResult) {
            return mergeAll(List.of(this, anotherResult));
        }

        public static class Accumulator {
            private final BepOutputBuilder myBepOutput = new BepOutputBuilder();
            private BazelStatus myStatus = BazelStatus.SUCCESS;

            public Accumulator add(BazelBspAspectsManagerResult result) {
                myBepOutput.add(result.bepOutput());
                myStatus = myStatus.merge(result.status());
                return this;
            }

            public BazelBspAspectsManagerResult build() {
                return new BazelBspAspectsManagerResult(myBepOutput.build(), myStatus);
            }
        }

        public static BazelBspAspectsManagerResult emptyResult() {
            return new BazelBspAspectsManagerResult(new BepOutput(), BazelStatus.SUCCESS);
        }

        public static BazelBspAspectsManagerResult mergeAll(Iterable<BazelBspAspectsManagerResult> results) {
            Accumulator accumulator = new Accumulator();
            results.forEach(accumulator::add);
            return accumulator.build();
        }
    }

    public sealed interface RuleSetName permits ApparentRulesetName, CanonicalRulesetName {
    }

    public record ApparentRulesetName(String name) implements RuleSetName {
    }

    public record CanonicalRulesetName(String name) implements RuleSetName {
    }

    public record RulesetInstance(@Nullable RuleSetName rulesetName, BazelRuleSet ruleset) {
    }

    private final Path myWorkspaceRoot;
    private final ExecuteService myExecuteService;
    private final BazelRelease myBazelRelease;

    // consulo: the aspect module the workspace depends on (bazel_dep on consulo_aspect or intellij_aspect), set by
    // deployIntelliJAspect(); null when the aspect is materialized into .bazelbsp
    private volatile BazelAspectModule.@Nullable Resolved myExternalAspect;

    public BazelBspAspectsManager(
        Path workspaceRoot,
        ExecuteService executeService,
        BazelRelease bazelRelease
    ) {
        myWorkspaceRoot = workspaceRoot;
        myExecuteService = executeService;
        myBazelRelease = bazelRelease;
    }

    public List<RulesetInstance> calculateRulesets(
        List<String> externalRulesetNames,
        Map<String, @Nullable ShowRepoResult> externalRulesetDefinitions,
        List<String> externalAutoloads
    ) {
        // All our canonical rulesets either have empty strip_prefix or strip the top-level directory (consiting of rule name plus version).
        // Filter out sub-rules which are built from the same archive, but stripping out top-level directory plus a subdirectory.
        Map<String, @Nullable ShowRepoResult> externalTopLevelHttpArchives = new LinkedHashMap<>();
        externalRulesetDefinitions.forEach((key, value) -> {
            if (value instanceof ShowRepoResult.HttpArchiveRepository httpArchive && httpArchive.stripPrefix().indexOf('/') == -1) {
                externalTopLevelHttpArchives.put(key, value);
            }
        });
        // consulo: upstream mapNotNull { it.name to it.urls }, then flatMap/associateBy/mapValues to a host location -> canonical name map
        Map<String, String> canonicalRepoByHostLocation = new LinkedHashMap<>();
        for (ShowRepoResult definition : externalTopLevelHttpArchives.values()) {
            if (definition instanceof ShowRepoResult.HttpArchiveRepository httpArchive) {
                for (String url : httpArchive.urls()) {
                    canonicalRepoByHostLocation.put(url, httpArchive.name());
                }
            }
        }
        List<RulesetInstance> result = new ArrayList<>();
        for (BazelRuleSet ruleSet : BazelRuleSetProvider.all()) {
            String hostLocation = null;
            for (String location : ruleSet.hostLocations()) {
                hostLocation = canonicalRepoByHostLocation.keySet().stream().filter(it -> it.startsWith(location)).findFirst().orElse(null);
                if (hostLocation != null) {
                    break;
                }
            }
            if (hostLocation != null) {
                if (!versionMatch(ruleSet, hostLocation)) {
                    // We have identified the rule, but it is the wrong version. If it cannot be used in bundled form,
                    // we cannot use it at all.
                    if (BazelRuleSetProvider.isBundledFor(ruleSet, myBazelRelease, externalAutoloads)) {
                        result.add(new RulesetInstance(null, ruleSet));
                    }
                    continue;
                }
                String canonicalName = canonicalRepoByHostLocation.get(hostLocation);
                result.add(new RulesetInstance(canonicalName != null ? new CanonicalRulesetName(canonicalName) : null, ruleSet));
                continue;
            }
            String rulesetName = ruleSet.rulesetNames().stream().filter(externalRulesetNames::contains).findFirst().orElse(null);
            if (rulesetName != null) {
                result.add(new RulesetInstance(new ApparentRulesetName(rulesetName), ruleSet));
                continue;
            }
            if (BazelRuleSetProvider.isBundledFor(ruleSet, myBazelRelease, externalAutoloads)) {
                result.add(new RulesetInstance(null, ruleSet));
            }
        }
        return result;
    }

    private boolean versionMatch(BazelRuleSet ruleSet, String hostLocation) {
        Pattern versionRegex = ruleSet.versionRegex();
        Integer minimalVersion = ruleSet.minimalVersion();
        if (versionRegex == null || minimalVersion == null) {
            return true; // no version constraint
        }
        Matcher match = versionRegex.matcher(hostLocation);
        if (!match.matches()) {
            return false; // versioning scheme outside  the supported one
        }
        int version = StringUtil.parseInt(match.group(1), 0);
        return version >= minimalVersion;
    }

    public void deployIntelliJAspect(
        List<RulesetInstance> ruleSetInstances,
        BazelRelease bazelRelease,
        RepoMapping repoMapping,
        @Nullable List<String> aspectAttrs,
        boolean dropNoIde
    ) {
        // consulo: a workspace that depends on a published aspect module uses it as is (no templating is needed for a
        // module), the aspect is materialized only otherwise
        BazelAspectModule.Resolved externalAspect = BazelAspectModule.detect(repoMapping);
        myExternalAspect = externalAspect;
        if (externalAspect != null) {
            return;
        }

        Map<Rules, String> ruleNameMapping = new LinkedHashMap<>();
        for (RulesetInstance it : ruleSetInstances) {
            // As the versions of rules_java for bazel 7 do not provide full information, we
            // prefer to the builtin rules.
            if ((it.ruleset().aspectLanguage() == Rules.JAVA) && (bazelRelease.major() <= 7)) {
                continue;
            }
            String canonicalRuleName = calculateCanonicalName(it, repoMapping);
            if (canonicalRuleName == null) {
                continue;
            }
            ruleNameMapping.put(it.ruleset().aspectLanguage(), "@" + canonicalRuleName);
        }

        // Languages for which the built-in rule set (bazel 8 and earlier) is used.
        // As the versions of rules_java for bazel 7 do not provide full information, we
        // prefer to the builtin rules.
        Set<Rules> builtInLanguages = new LinkedHashSet<>();
        for (RulesetInstance it : ruleSetInstances) {
            if (calculateCanonicalName(it, repoMapping) == null) {
                builtInLanguages.add(it.ruleset().aspectLanguage());
            }
        }
        if (bazelRelease.major() <= 7) {
            builtInLanguages.add(Rules.JAVA);
        }

        try {
            Deploy.deployAspectZip(
                myWorkspaceRoot,
                Path.of(Constants.DOT_BAZELBSP_DIR_NAME),
                new AspectConfig(
                    BazelInfo.toVersionString(bazelRelease),
                    ruleNameMapping,
                    builtInLanguages,
                    ruleSetInstances.stream().map(it -> it.ruleset().aspectLanguage()).collect(Collectors.toCollection(LinkedHashSet::new)),
                    aspectAttrs,
                    dropNoIde
                )
            );
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private @Nullable String calculateCanonicalName(RulesetInstance receiver, RepoMapping repoMapping) {
        RuleSetName rulesetName = receiver.rulesetName();
        // If the name is already a conical one, we can take it; however we have the canonical name without
        // prefix, so we have to add @ to indicate it as canonical, as the template adds a single @ as prefix.
        if (rulesetName instanceof CanonicalRulesetName canonicalRulesetName) {
            return "@" + canonicalRulesetName.name();
        }
        // bazel mod dump_repo_mapping returns everything without @@
        // and in aspects we have a @ prefix
        else if (repoMapping instanceof BzlmodRepoMapping bzlmodRepoMapping && rulesetName instanceof ApparentRulesetName apparentRulesetName) {
            String canonicalName = bzlmodRepoMapping.getApparentRepoNameToCanonicalName().get(apparentRulesetName.name());
            return canonicalName != null ? "@" + canonicalName : apparentRulesetName.name();
        }
        else {
            return rulesetName instanceof ApparentRulesetName apparentRulesetName ? apparentRulesetName.name() : null;
        }
    }

    public CompletableFuture<BazelBspAspectsManagerResult> fetchFilesFromOutputGroups(
        TargetCollection targetsSpec,
        List<String> aspects,
        List<String> outputGroups,
        ProjectView projectView,
        TaskId taskId
    ) {
        if (targetsSpec.values().isEmpty()) {
            return CompletableFuture.completedFuture(new BazelBspAspectsManagerResult(new BepOutput(), BazelStatus.SUCCESS));
        }
        List<String> defaultFlags =
            List.of(
                BazelFlag.aspect(aspects.stream().map(this::resolveWorkspaceAspectLabel).collect(Collectors.joining(","))),
                BazelFlag.outputGroups(outputGroups),
                BazelFlag.keepGoing(),
                // Validations don't contribute to the project model and only slow down sync, so disable them.
                BazelFlag.noRunValidations(),
                BazelFlag.skipIncompatibleExplicitTargets()
            );
        List<String> allowManualTargetsSyncFlags =
            ProjectViewExtensions.getAllowManualTargetsSync(projectView) ? List.of(BazelFlag.buildManualTests()) : List.of();
        List<String> syncFlags = ProjectViewExtensions.getSyncFlags(projectView);

        List<String> flagsToUse = new ArrayList<>(defaultFlags);
        flagsToUse.addAll(allowManualTargetsSyncFlags);
        flagsToUse.addAll(syncFlags);

        return myExecuteService.buildTargetsWithBep(
            // WARNINGs are intentionally not filtered here: this empty build is the first invocation of a sync and is
            // where Bazel reports "discarding analysis cache" when build options changed. We need that warning to reach
            // BepServer for FUS. The resulting empty-target-set warning is dropped from the UI by isEmptyTargetSetWarning.
            // See https://github.com/bazelbuild/bazel/issues/6811
            new TargetCollection(List.of()),
            flagsToUse,
            taskId
        ).thenCompose(emptyBuild -> {
            List<String> options = emptyBuild.bepOutput().getOptions();
            String existingBuildTagFilters = lastOrNull(options, "--build_tag_filters=");
            if (existingBuildTagFilters != null) {
                existingBuildTagFilters = existingBuildTagFilters.substring("--build_tag_filters=".length());
            }
            // Ensure remote_download_outputs is at least toplevel so that all required output files are downloaded
            List<String> remoteDownloadOverride;
            if ("--remote_download_outputs=minimal".equals(lastOrNull(options, "--remote_download_outputs="))) {
                remoteDownloadOverride = List.of(BazelFlag.remoteDownloadOutputsTopLevel());
            }
            else {
                // Do not modify the value if remote_download_outputs is set to a higher value or the default (toplevel) is used
                remoteDownloadOverride = List.of();
            }

            List<String> extraFlags = new ArrayList<>(flagsToUse);
            extraFlags.addAll(remoteDownloadOverride);
            extraFlags.add(
                "--build_tag_filters=" + (existingBuildTagFilters != null ? existingBuildTagFilters + "," : "") + "-" + Constants.NO_IDE
            );
            return myExecuteService
                .buildTargetsWithBep(
                    targetsSpec,
                    extraFlags,
                    taskId
                ).thenApply(it -> {
                    BepOutput bepOutput = it.bepOutput();
                    if (bepOutput.getBuildToolVersion().equals(BazelRelease.FALLBACK_VERSION)) {
                        bepOutput.setBuildToolVersion(myBazelRelease);
                    }
                    return new BazelBspAspectsManagerResult(bepOutput, it.processResult().getBazelStatus());
                });
        });
    }

    // consulo: upstream options.lastOrNull { it.startsWith(prefix) }
    private static @Nullable String lastOrNull(List<String> options, String prefix) {
        for (int i = options.size() - 1; i >= 0; i--) {
            if (options.get(i).startsWith(prefix)) {
                return options.get(i);
            }
        }
        return null;
    }

    // consulo: the aspect module the workspace depends on, see deployIntelliJAspect()
    public BazelAspectModule.@Nullable Resolved getExternalAspect() {
        return myExternalAspect;
    }

    // consulo: upstream calls the top-level resolveAspectLabel(); the aspect of an external aspect module is used if detected
    private String resolveWorkspaceAspectLabel(String aspect) {
        BazelAspectModule.Resolved externalAspect = myExternalAspect;
        if (externalAspect != null) {
            // consulo: the aspect of the module from the workspace's bazel_dep
            return externalAspect.aspectLabel(aspect);
        }
        return resolveAspectLabel(aspect);
    }

    /**
     * Returns the label of an aspect deployed to the {@code .bazelbsp} directory.
     */
    public static String resolveAspectLabel(String aspect) {
        return "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/" + aspect;
    }
}
