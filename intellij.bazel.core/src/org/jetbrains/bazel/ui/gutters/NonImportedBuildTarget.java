package org.jetbrains.bazel.ui.gutters;

import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

// consulo: upstream data class; it implements BuildTarget (getKey()/getKind()/getTags() accessors), so it is a class with
//   equals/hashCode/toString over the constructor properties instead of a record
public class NonImportedBuildTarget implements BuildTarget {
    private final WorkspaceTargetKey myKey;
    private final TargetKind myKind;
    private final List<String> myTags;

    private final String myWorkspaceName = "_main";

    public NonImportedBuildTarget(
        WorkspaceTargetKey key,
        TargetKind kind,
        List<String> tags
    ) {
        myKey = key;
        myKind = kind;
        myTags = tags;
    }

    // consulo: overload for the upstream default argument (tags = emptyList())
    public NonImportedBuildTarget(WorkspaceTargetKey key, TargetKind kind) {
        this(key, kind, List.of());
    }

    public NonImportedBuildTarget(
        Label label,
        TargetKind kind,
        List<String> tags
    ) {
        this(new WorkspaceTargetKey(label), kind, tags);
    }

    // consulo: overload for the upstream default argument (tags = emptyList())
    public NonImportedBuildTarget(Label label, TargetKind kind) {
        this(label, kind, List.of());
    }

    @Override
    public WorkspaceTargetKey getKey() {
        return myKey;
    }

    @Override
    public TargetKind getKind() {
        return myKind;
    }

    @Override
    public List<String> getTags() {
        return myTags;
    }

    @Override
    public TargetLoadOptions getLoaded() {
        return TargetLoadOptions.MINIMAL;
    }

    @Override
    public @Nullable String getGeneratorName() {
        return null;
    }

    @Override
    public boolean isTestOnly() {
        return false;
    }

    @Override
    public String getWorkspaceName() {
        return myWorkspaceName;
    }

    @Override
    public List<DependencyLabel> getDependencies() {
        return List.of();
    }

    @Override
    public OutputLocationCollection getSources() {
        return OutputLocationCollection.EMPTY;
    }

    @Override
    public OutputLocationCollection getResources() {
        return OutputLocationCollection.EMPTY;
    }

    @Override
    public List<BuildTargetData> getData() {
        return List.of();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NonImportedBuildTarget that)) {
            return false;
        }
        return myKey.equals(that.myKey) && myKind.equals(that.myKind) && myTags.equals(that.myTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myKey, myKind, myTags);
    }

    @Override
    public String toString() {
        return "NonImportedBuildTarget(key=" + myKey + ", kind=" + myKind + ", tags=" + myTags + ")";
    }
}
