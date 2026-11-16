package org.jetbrains.bazel.ui.gutters;

import consulo.execution.action.Location;
import consulo.execution.action.PsiLocation;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.config.project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Needed because {@link consulo.execution.action.RunConfigurationProducer} can only produce one configuration per one location.
 */
// TODO consulo: MultipleRunLocationsProvider (com.intellij.execution.actions) is missing; the class is not registered
//  (upstream <multipleRunLocationsProvider>) and its methods override nothing
public class BazelContainingTargetsLocationsProvider {
    public List<Location<?>> getAlternativeLocations(Location<?> originalLocation) {
        if (!project.isBazelProject(originalLocation.getProject())) {
            return List.of();
        }
        if (!(originalLocation instanceof PsiLocation<?> psiLocation)) {
            return List.of();
        }
        List<BuildTargetInfo> targets = getTargets(psiLocation.getPsiElement());
        if (targets.isEmpty()) {
            return List.of();
        }
        List<Location<?>> result = new ArrayList<>();
        for (BuildTargetInfo target : targets) {
            result.add(new BazelRunLocation(target, psiLocation));
        }
        result.add(originalLocation);
        return result;
    }

    /**
     * This can also include non-executable targets (for synthetic run gutters)
     */
    private List<BuildTargetInfo> getTargets(PsiElement element) {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(element.getProject());
        PsiFile file = element.getContainingFile();
        VirtualFile containingFile = file != null ? file.getVirtualFile() : null;
        if (containingFile == null) {
            return List.of();
        }
        List<BuildTargetInfo> normalTargets = targetUtils.getTargetsForFile(containingFile)
            .stream()
            .map(it -> TargetStorage.getTargetInfo(targetUtils, it))
            .filter(Objects::nonNull)
            .toList();
        List<BuildTargetInfo> executableTargets = targetUtils.getExecutableTargetsForFile(containingFile)
            .stream()
            .map(it -> TargetStorage.getTargetInfo(targetUtils, it))
            .filter(Objects::nonNull)
            .toList();
        Set<Label> normalLabels = new HashSet<>();
        for (BuildTargetInfo it : normalTargets) {
            normalLabels.add(it.id());
        }
        List<BuildTargetInfo> dependentTargets = executableTargets.stream().filter(it -> !normalLabels.contains(it.id())).toList();
        Set<Label> seenIds = new HashSet<>();
        return Stream.concat(normalTargets.stream(), filterDependentTargets(element, dependentTargets).stream())
            .filter(it -> seenIds.add(it.id()))
            .toList();
    }

    private List<BuildTargetInfo> filterDependentTargets(PsiElement element, List<BuildTargetInfo> targets) {
        if (targets.size() < 2) {
            return targets;
        }
        Map<Label, ThreeState> matches = BazelEntryPointMatcher.matchAll(element, targets);
        // in case element is not recognized as an entry point of any given targets, keep all targets
        if (matches.values().stream().noneMatch(it -> it == ThreeState.YES)) {
            return targets;
        }
        return targets.stream().filter(it -> {
            ThreeState match = Objects.requireNonNull(matches.get(it.id()));
            return switch (match) {
                case YES, UNSURE -> true;
                case NO -> false;
            };
        }).toList();
    }

    public @Nullable String getLocationDisplayName(
        Location<?> locationCreatedFrom,
        Location<?> originalLocation
    ) {
        return null;
    }
}
