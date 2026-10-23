package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public interface BuildTarget {
    WorkspaceTargetKey getKey();

    TargetKind getKind();

    TargetLoadOptions getLoaded();

    // TargetSection.INFO
    @Nullable String getGeneratorName();

    boolean isTestOnly();

    List<String> getTags();

    String getWorkspaceName();

    // TargetSection.DEPS
    List<DependencyLabel> getDependencies();

    // TargetSection.FILE_SETS
    OutputLocationCollection getSources();

    OutputLocationCollection getResources();

    // language-specific data, selected by TargetLoadOptions.targetData
    List<BuildTargetData> getData();

    static Label getId(BuildTarget receiver) {
        return receiver.getKey().label();
    }

    /**
     * whether this target already carries every section, i.e. reading it triggers no further loading
     */
    static boolean isFull(BuildTarget receiver) {
        return receiver.getLoaded().equals(TargetLoadOptions.ALL);
    }

    static <T extends BuildTargetData> @Nullable T data(BuildTarget receiver, Class<T> type) {
        return receiver.getData().stream().filter(type::isInstance).findFirst().map(type::cast).orElse(null);
    }

    interface BuildTargetData {
    }

    enum StrictDependencyCheckedType {
        OFF,
        WARNING,
        ERROR,
    }

    // consulo: upstream is `inline fun <reified Data>`, the type is passed explicitly
    static <Data> @Nullable Data extractData(BuildTarget receiver, Class<Data> type) {
        List<Data> filtered = receiver.getData().stream().filter(type::isInstance).map(type::cast).toList();
        return filtered.size() == 1 ? filtered.get(0) : null;
    }

    /**
     * From Bazel doc (https://bazel.build/reference/be/common-definitions)
     * manual tag will exclude the target from expansion of target pattern wildcards (..., :*, :all, etc.) and test_suite rules which do not
     * list the test explicitly when computing the set of top-level targets to build/run for the build, test, and coverage commands
     */
    static boolean isManual(BuildTarget receiver) {
        return receiver.getTags().contains(BuildTargetTag.MANUAL);
    }

    static Stream<OutputLocation> nonGeneratedSources(BuildTarget receiver) {
        return receiver.getSources().getOutputLocations().filter(OutputLocation::isSource);
    }
}
