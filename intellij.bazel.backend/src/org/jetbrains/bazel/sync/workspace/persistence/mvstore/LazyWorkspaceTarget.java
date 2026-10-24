package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot.HeavyWorkspaceTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

final class LazyWorkspaceTarget implements BuildTarget {
    private final WorkspaceTargetKey myKey;
    private final TargetKind myKind;
    private final @Nullable String myGeneratorName;
    private final boolean myIsTestOnly;
    private final List<String> myTags;
    private final String myWorkspaceName;
    private final TargetLoadOptions myLoaded;
    private final LazyTargetSection<List<DependencyLabel>> myDepsSection;
    private final LazyTargetSection<HeavyWorkspaceTarget> myFileSetsSection;
    private final LazyTargetSection<List<BuildTargetData>> myDataSection;

    LazyWorkspaceTarget(
        WorkspaceTargetKey key,
        TargetKind kind,
        @Nullable String generatorName,
        boolean isTestOnly,
        List<String> tags,
        String workspaceName,
        TargetLoadOptions loaded,
        LazyTargetSection<List<DependencyLabel>> depsSection,
        LazyTargetSection<HeavyWorkspaceTarget> fileSetsSection,
        LazyTargetSection<List<BuildTargetData>> dataSection
    ) {
        myKey = key;
        myKind = kind;
        myGeneratorName = generatorName;
        myIsTestOnly = isTestOnly;
        myTags = tags;
        myWorkspaceName = workspaceName;
        myLoaded = loaded;
        myDepsSection = depsSection;
        myFileSetsSection = fileSetsSection;
        myDataSection = dataSection;
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
    public @Nullable String getGeneratorName() {
        return myGeneratorName;
    }

    @Override
    public boolean isTestOnly() {
        return myIsTestOnly;
    }

    @Override
    public List<String> getTags() {
        return myTags;
    }

    @Override
    public String getWorkspaceName() {
        return myWorkspaceName;
    }

    @Override
    public TargetLoadOptions getLoaded() {
        return myLoaded;
    }

    @Override
    public List<DependencyLabel> getDependencies() {
        return myDepsSection.getValue();
    }

    @Override
    public OutputLocationCollection getSources() {
        return myFileSetsSection.getValue().getSources();
    }

    @Override
    public OutputLocationCollection getResources() {
        return myFileSetsSection.getValue().getResources();
    }

    @Override
    public List<BuildTargetData> getData() {
        return myDataSection.getValue();
    }
}

final class LazyTargetSection<T> {
    private final Supplier<T> mySection;

    LazyTargetSection(boolean isLoaded, Supplier<T> loader) {
        if (isLoaded) {
            // evaluate immediately
            T value = loader.get();
            mySection = () -> value;
        }
        else {
            // keep it unloaded until explicitly requested and then cache
            mySection = LazyValue.atomicNotNull(() -> {
                // TODO consulo: SlowOperations.assertSlowOperationsAreAllowed() - consulo.application.internal.SlowOperations is not exported
                return loader.get();
            });
        }
    }

    T getValue() {
        return mySection.get();
    }
}
