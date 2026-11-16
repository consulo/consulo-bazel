package org.jetbrains.bazel.run;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
     * Implementations should generally handle targets that are not imported as well.
     */
    boolean canRun(Project project, List<BuildTargetInfo> targets);

    default boolean canRunNonImported(Project project, List<Label> targets) {
        return false;
    }

    boolean isTestHandler();

    /**
     * If {@code true}, this provider's run handler can be manually chosen by the user to execute a run configuration
     */
    default boolean canBeForced() {
        return false;
    }

    /**
     * Name of the run handler this provider returns
     */
    LocalizeValue getProvidedRunHandlerName();

    static List<RunHandlerProvider> getEp() {
        return Application.get().getExtensionList(RunHandlerProvider.class);
    }

    /**
     * Finds a BspRunHandlerProvider that will be able to create a BspRunHandler for the given targets
     */
    static @Nullable RunHandlerProvider getRunHandlerProvider(Project project, List<BuildTargetInfo> targets) {
        for (RunHandlerProvider it : getEp()) {
            if (it.canRun(project, targets)) {
                return it;
            }
        }
        return null;
    }

    static @Nullable RunHandlerProvider getRunHandlerProviderOrNull(Project project, List<Label> targets) {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(project);
        List<BuildTargetInfo> targetInfos = new ArrayList<>();
        for (Label it : targets) {
            BuildTargetInfo targetInfo = TargetStorage.getTargetInfo(targetUtils, it);
            if (targetInfo != null) {
                targetInfos.add(targetInfo);
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

    /**
     * Finds the providers that a user can select in the run configuration editor
     */
    static List<RunHandlerProvider> findSelectableRunHandlerProviders() {
        List<RunHandlerProvider> result = new ArrayList<>();
        for (RunHandlerProvider it : getEp()) {
            if (it.canBeForced()) {
                result.add(it);
            }
        }
        return result;
    }
}
