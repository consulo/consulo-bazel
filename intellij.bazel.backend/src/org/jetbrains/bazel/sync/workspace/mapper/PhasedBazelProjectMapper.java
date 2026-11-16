package org.jetbrains.bazel.sync.workspace.mapper;

import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.commons.phased.FirstPhaseTargetUtils;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jetbrains.bsp.protocol.OutputLocation;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PhasedBazelProjectMapper {
    private final BazelPathsResolver myBazelPathsResolver;
    private final ProjectView myProjectView;

    public PhasedBazelProjectMapper(
        BazelPathsResolver bazelPathsResolver,
        ProjectView projectView
    ) {
        myBazelPathsResolver = bazelPathsResolver;
        myProjectView = projectView;
    }

    public List<BuildTarget> mapTargets(
        Map<Label, Build.Target> targets
    ) {
        boolean shouldSyncManualTargets = ProjectViewExtensions.getAllowManualTargetsSync(myProjectView);
        // consulo: renamed from `targets`, Java cannot shadow the parameter
        List<BuildTarget> mappedTargets =
            targets.values()
                .stream()
                .filter(it -> isSupported(it))
                .filter(it -> shouldSyncManualTargets || !FirstPhaseTargetUtils.isManual(it))
                .filter(it -> !FirstPhaseTargetUtils.isNoIde(it))
                .map(it -> toBspBuildTarget(it, targets))
                .toList();
        return mappedTargets;
    }

    private BuildTarget toBspBuildTarget(Build.Target receiver, Map<Label, Build.Target> targets) {
        ResolvedLabel label = Label.assumeResolved(Label.parse(FirstPhaseTargetUtils.getName(receiver)));
        return BuildTarget.create(
            new BuildTargetInfo(
                new WorkspaceTargetKey(label),
                inferKind(receiver),
                FirstPhaseTargetUtils.getGeneratorName(receiver),
                false,
                FirstPhaseTargetUtils.getTags(receiver),
                "_main"
            ),
            FirstPhaseTargetUtils.getInterestingDeps(receiver).stream().map(it -> DependencyLabel.parse(it)).toList(),
            OutputLocationCollectionBuilder.ofLocations(calculateSources(receiver, targets)),
            OutputLocationCollectionBuilder.ofLocations(calculateResources(receiver, targets)),
            List.of()
        );
    }

    private TargetKind inferKind(Build.Target receiver) {
        TargetKind inferredRuleKind = TargetKindService.getInstance().guessFromRuleName(FirstPhaseTargetUtils.getKind(receiver));
        if (!inferredRuleKind.languageClasses().isEmpty()) {
            return inferredRuleKind;
        }
        Set<LanguageClass> languageClasses = languagesFromSources(receiver).collect(Collectors.toCollection(LinkedHashSet::new));
        return new TargetKind(inferredRuleKind.kind(), languageClasses, inferredRuleKind.ruleType());
    }

    private Stream<LanguageClass> languagesFromSources(Build.Target receiver) {
        return FirstPhaseTargetUtils.getSrcs(receiver).stream()
            // consulo: Kotlin substringAfterLast('.') returns the whole string when there is no '.'
            .map(it -> LanguageClass.fromExtension(it.substring(it.lastIndexOf('.') + 1)))
            .filter(Objects::nonNull);
    }

    private boolean isSupported(Build.Target receiver) {
        return TargetKindService.getInstance().findPredefinedRule(FirstPhaseTargetUtils.getKind(receiver)) != null ||
            languagesFromSources(receiver).findAny().isPresent();
    }

    private List<OutputLocation> calculateSources(Build.Target receiver, Map<Label, Build.Target> targets) {
        List<OutputLocation> sourceFiles = calculateFiles(FirstPhaseTargetUtils.getSrcs(receiver));
        List<OutputLocation> itemsFromDependencies = new ArrayList<>();
        for (Build.Target dependency : calculateModuleDependencies(FirstPhaseTargetUtils.getSrcs(receiver), targets)) {
            itemsFromDependencies.addAll(calculateSources(dependency, targets));
        }
        return Stream.concat(sourceFiles.stream(), itemsFromDependencies.stream()).distinct().toList();
    }

    private List<OutputLocation> calculateResources(Build.Target receiver, Map<Label, Build.Target> targets) {
        List<OutputLocation> directResources = calculateFiles(FirstPhaseTargetUtils.getResources(receiver));
        List<OutputLocation> resourcesFromDependencies = new ArrayList<>();
        for (Build.Target dependency : calculateModuleDependencies(FirstPhaseTargetUtils.getResources(receiver), targets)) {
            resourcesFromDependencies.addAll(calculateResources(dependency, targets));
        }
        return Stream.concat(directResources.stream(), resourcesFromDependencies.stream()).distinct().toList();
    }

    private List<Build.Target> calculateModuleDependencies(List<String> receiver, Map<Label, Build.Target> targets) {
        List<Build.Target> result = new ArrayList<>();
        for (String it : receiver) {
            Label label = Label.parseOrNull(it);
            if (label == null) {
                continue;
            }
            Build.Target target = targets.get(label);
            if (target != null) {
                result.add(target);
            }
        }
        return result;
    }

    private List<OutputLocation> calculateFiles(List<String> receiver) {
        return receiver.stream()
            .map(it -> bazelFileFormatToWorkspacePath(it))
            .filter(it -> Files.isRegularFile(myBazelPathsResolver.workspaceRoot().resolve(it)))
            .map(it -> (OutputLocation) new OutputLocation.Workspace(it))
            .toList();
    }

    // `//a/b:c.java` -> `a/b/c.java`
    private static String bazelFileFormatToWorkspacePath(String receiver) {
        return StringUtil.trimLeading(receiver.replace(':', '/'), '/');
    }
}
