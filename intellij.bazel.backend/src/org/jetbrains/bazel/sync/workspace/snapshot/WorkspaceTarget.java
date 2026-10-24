package org.jetbrains.bazel.sync.workspace.snapshot;

import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;

// consulo: internal upstream, public because the workspace mapper creates it
public class WorkspaceTarget implements BuildTarget {
    private final WorkspaceTargetKey myKey;
    private final List<DependencyLabel> myDependencies;
    private final TargetKind myKind;
    private final OutputLocationCollection mySources;
    private final OutputLocationCollection myResources;
    private final List<BuildTargetData> myData;
    private final @Nullable String myGeneratorName;
    private final boolean myIsTestOnly;
    private final List<String> myTags;
    private final String myWorkspaceName;

    public WorkspaceTarget(
        WorkspaceTargetKey key,
        List<DependencyLabel> dependencies,
        TargetKind kind,
        OutputLocationCollection sources,
        OutputLocationCollection resources,
        List<BuildTargetData> data,
        @Nullable String generatorName,
        boolean isTestOnly,
        List<String> tags,
        String workspaceName
    ) {
        myKey = key;
        myDependencies = dependencies;
        myKind = kind;
        mySources = sources;
        myResources = resources;
        myData = data;
        myGeneratorName = generatorName;
        myIsTestOnly = isTestOnly;
        myTags = tags;
        myWorkspaceName = workspaceName;
    }

    @Override
    public WorkspaceTargetKey getKey() {
        return myKey;
    }

    @Override
    public List<DependencyLabel> getDependencies() {
        return myDependencies;
    }

    @Override
    public TargetKind getKind() {
        return myKind;
    }

    @Override
    public OutputLocationCollection getSources() {
        return mySources;
    }

    @Override
    public OutputLocationCollection getResources() {
        return myResources;
    }

    @Override
    public List<BuildTargetData> getData() {
        return myData;
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
        return TargetLoadOptions.ALL;
    }

    public static boolean isTestTarget(BuildTarget receiver) {
        return receiver.isTestOnly() || receiver.getKind().ruleType() == RuleType.TEST;
    }
}
