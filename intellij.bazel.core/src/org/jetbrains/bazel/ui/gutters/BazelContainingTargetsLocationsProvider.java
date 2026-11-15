package org.jetbrains.bazel.ui.gutters;

import consulo.execution.action.Location;
import consulo.execution.action.PsiLocation;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.config.project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        List<BuildTarget> targets = getTargets(psiLocation.getPsiElement());
        if (targets.isEmpty()) {
            return List.of();
        }
        List<Location<?>> result = new ArrayList<>();
        for (BuildTarget target : targets) {
            result.add(new BazelRunLocation(target, psiLocation));
        }
        result.add(originalLocation);
        return result;
    }

    /**
     * This can also include non-executable targets (for synthetic run gutters)
     */
    private List<BuildTarget> getTargets(PsiElement element) {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(element.getProject());
        PsiFile file = element.getContainingFile();
        VirtualFile containingFile = file != null ? file.getVirtualFile() : null;
        if (containingFile == null) {
            return List.of();
        }
        List<BuildTarget> normalTargets = targetUtils.getTargetsForFile(containingFile)
            .stream()
            .map(targetUtils::getTargetSummary)
            .filter(Objects::nonNull)
            .toList();
        List<BuildTarget> executableTargets = targetUtils.getExecutableTargetsForFile(containingFile)
            .stream()
            .map(targetUtils::getTargetSummary)
            .filter(Objects::nonNull)
            .toList();
        Set<Label> seenIds = new HashSet<>();
        return Stream.concat(normalTargets.stream(), executableTargets.stream())
            .filter(it -> seenIds.add(BuildTarget.getId(it)))
            .toList();
    }

    public @Nullable String getLocationDisplayName(
        Location<?> locationCreatedFrom,
        Location<?> originalLocation
    ) {
        return null;
    }
}
