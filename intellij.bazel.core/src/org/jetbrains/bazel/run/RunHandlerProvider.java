package org.jetbrains.bazel.run;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface RunHandlerProvider {
    /**
     * Returns the unique ID of this {@link RunHandlerProvider}. The ID is
     * used to store configuration settings and must not change between plugin versions.
     */
    String getId();

    /**
     * Creates a {@link BazelRunHandler} for the given configuration.
     */
    BazelRunHandler createRunHandler(BazelRunConfiguration configuration);

    /**
     * Returns true if this provider can create a {@link BazelRunHandler} for running the given targets.
     * Implementations should generally handle {@code org.jetbrains.bazel.ui.gutters.NonImportedBuildTarget} as well.
     */
    boolean canRun(Project project, List<BuildTarget> targets);

    default boolean canRunNonImported(Project project, List<Label> targets) {
        return false;
    }

    static List<RunHandlerProvider> getEp() {
        return Application.get().getExtensionList(RunHandlerProvider.class);
    }

    /**
     * Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets
     */
    static @Nullable RunHandlerProvider getRunHandlerProvider(Project project, List<BuildTarget> targets) {
        for (RunHandlerProvider it : getEp()) {
            if (it.canRun(project, targets)) {
                return it;
            }
        }
        return null;
    }

    /**
     * Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets.
     * Needs to query WM for Build Target Infos.
     */
    static RunHandlerProvider getRunHandlerProviderOrThrow(Project project, List<Label> targets) {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(project);
        List<BuildTarget> targetInfos = new ArrayList<>();
        for (Label it : targets) {
            BuildTarget targetSummary = targetUtils.getTargetSummary(it);
            if (targetSummary != null) {
                targetInfos.add(targetSummary);
            }
        }
        if (targetInfos.size() != targets.size()) {
            Set<Label> missing = new LinkedHashSet<>(targets);
            for (BuildTarget it : targetInfos) {
                missing.remove(BuildTarget.getId(it));
            }
            Logger.getInstance(RunHandlerProvider.class).warn("Some targets could not be found: " + new ArrayList<>(missing));
        }

        if (targetInfos.isEmpty()) {
            throw new IllegalArgumentException("targetInfos should not be empty");
        }

        RunHandlerProvider provider = getRunHandlerProvider(project, targetInfos);
        if (provider == null) {
            throw new IllegalArgumentException("No BspRunHandlerProvider found for targets: " + targets);
        }
        return provider;
    }

    static @Nullable RunHandlerProvider getRunHandlerProviderOrNull(Project project, List<Label> targets) {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(project);
        List<BuildTarget> targetInfos = new ArrayList<>();
        for (Label it : targets) {
            BuildTarget targetSummary = targetUtils.getTargetSummary(it);
            if (targetSummary != null) {
                targetInfos.add(targetSummary);
            }
        }
        if (!targetInfos.isEmpty()) {
            return getRunHandlerProvider(project, targetInfos);
        }
        else {
            for (RunHandlerProvider it : getEp()) {
                if (it.canRunNonImported(project, targets)) {
                    return it;
                }
            }
            return null;
        }
    }

    /**
     * Finds a BspRunHandlerProvider by its unique ID
     */
    static @Nullable RunHandlerProvider findRunHandlerProvider(String id) {
        for (RunHandlerProvider it : getEp()) {
            if (it.getId().equals(id)) {
                return it;
            }
        }
        return null;
    }
}
