package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

// TODO: make concrete data class in 2027.1, keep it sealed
public sealed interface BuildTarget permits BuildTargetImpl {
    BuildTargetInfo getInfo();

    List<DependencyLabel> getDependencies();

    OutputLocationCollection getSources();

    OutputLocationCollection getResources();

    List<BuildTargetData> getData();

    default WorkspaceTargetKey getKey() {
        return getInfo().key();
    }

    default TargetKind getKind() {
        return getInfo().kind();
    }

    default @Nullable String getGeneratorName() {
        return getInfo().generatorName();
    }

    default boolean isTestOnly() {
        return getInfo().isTestOnly();
    }

    default List<String> getTags() {
        return getInfo().tags();
    }

    default String getWorkspaceName() {
        return getInfo().workspaceName();
    }

    /**
     * @deprecated BuildTarget always has all its sections
     */
    @Deprecated
    default TargetLoadOptions getLoaded() {
        return TargetLoadOptions.ALL;
    }

    // consulo: upstream top-level factory function BuildTarget(info, dependencies, sources, resources, data)
    static BuildTarget create(
        BuildTargetInfo info,
        List<DependencyLabel> dependencies,
        OutputLocationCollection sources,
        OutputLocationCollection resources,
        List<BuildTargetData> data
    ) {
        return new BuildTargetImpl(info, dependencies, sources, resources, data);
    }

    // consulo: upstream extension BuildTarget.copy(...) with default arguments; pass receiver.getX() for an unchanged value
    static BuildTarget copy(
        BuildTarget receiver,
        BuildTargetInfo info,
        List<DependencyLabel> dependencies,
        OutputLocationCollection sources,
        OutputLocationCollection resources,
        List<BuildTargetData> data
    ) {
        return create(info, dependencies, sources, resources, data);
    }

    static Label getId(BuildTarget receiver) {
        return receiver.getKey().label();
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

    static boolean isManual(BuildTarget receiver) {
        return BuildTargetInfo.isManual(receiver.getInfo());
    }

    static Stream<OutputLocation> nonGeneratedSources(BuildTarget receiver) {
        return receiver.getSources().getOutputLocations().filter(OutputLocation::isSource);
    }
}

// consulo: upstream private data class BuildTargetImpl of BuildTarget.kt
record BuildTargetImpl(
    BuildTargetInfo info,
    List<DependencyLabel> dependencies,
    OutputLocationCollection sources,
    OutputLocationCollection resources,
    List<BuildTargetData> data
) implements BuildTarget {
    @Override
    public BuildTargetInfo getInfo() {
        return info;
    }

    @Override
    public List<DependencyLabel> getDependencies() {
        return dependencies;
    }

    @Override
    public OutputLocationCollection getSources() {
        return sources;
    }

    @Override
    public OutputLocationCollection getResources() {
        return resources;
    }

    @Override
    public List<BuildTargetData> getData() {
        return data;
    }
}
