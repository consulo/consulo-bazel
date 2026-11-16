package org.jetbrains.bazel.server.bep;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.OutputGroup;
import com.intellij.aspect.lib.OutputGroups;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BepOutputBuilder {
    public record TextProtoDepSet(Collection<Path> files, Collection<String> children) {
    }

    private final Set<String> myInfoOutputGroupFileSets = new HashSet<>();
    private final Map<String, TextProtoDepSet> myTextProtoFileSets = new HashMap<>();
    private final Set<WorkspaceTargetKey> myRootTargets = new HashSet<>();
    private final List<String> myOptions = new ArrayList<>();
    private final Map<WorkspaceConfigurationId, WorkspaceConfiguration> myConfigurations = new LinkedHashMap<>();
    private BazelRelease myBuildToolVersion = BazelRelease.FALLBACK_VERSION;

    public Map<WorkspaceConfigurationId, WorkspaceConfiguration> getConfigurations() {
        return myConfigurations;
    }

    public BazelRelease getBuildToolVersion() {
        return myBuildToolVersion;
    }

    public void setBuildToolVersion(BazelRelease buildToolVersion) {
        myBuildToolVersion = buildToolVersion;
    }

    public void storeNamedSet(String id, TextProtoDepSet textProtoDepSet) {
        myTextProtoFileSets.put(id, textProtoDepSet);
    }

    public void storeTargetOutputGroups(WorkspaceTargetKey target, List<OutputGroup> outputGroups) {
        myRootTargets.add(target);
        OutputGroup infoGroup = outputGroups.stream()
            .filter(it -> it.getName().equals(OutputGroups.INFO.getGroupName()))
            .findFirst()
            .orElse(null);
        if (infoGroup == null) {
            return;
        }
        List<String> fileSets = infoGroup.getFileSetsList().stream().map(it -> it.getId()).toList();
        myInfoOutputGroupFileSets.addAll(fileSets);
    }

    public void storeOptions(List<String> cmdline) {
        myOptions.addAll(cmdline);
    }

    public void storeConfiguration(WorkspaceConfiguration configuration) {
        myConfigurations.put(configuration.id(), configuration);
    }

    public void add(BepOutput output) {
        myInfoOutputGroupFileSets.addAll(output.getInfoOutputGroupFileSets());
        for (Map.Entry<String, TextProtoDepSet> entry : output.getTextProtoFileSets().entrySet()) {
            myTextProtoFileSets.merge(entry.getKey(), entry.getValue(), (first, second) -> {
                List<Path> files = new ArrayList<>(first.files());
                files.addAll(second.files());
                List<String> children = new ArrayList<>(first.children());
                children.addAll(second.children());
                return new TextProtoDepSet(files, children);
            });
        }
        myRootTargets.addAll(output.rootTargets());
        myOptions.addAll(output.getOptions());
        myConfigurations.putAll(output.getConfigurations());
        if (myBuildToolVersion.equals(BazelRelease.FALLBACK_VERSION)) {
            myBuildToolVersion = output.getBuildToolVersion();
        }
    }

    public void clear() {
        myInfoOutputGroupFileSets.clear();
        myTextProtoFileSets.clear();
        myRootTargets.clear();
        myOptions.clear();
        myBuildToolVersion = BazelRelease.FALLBACK_VERSION;
    }

    public BepOutput build() {
        return new BepOutput(
            myInfoOutputGroupFileSets,
            myTextProtoFileSets,
            myRootTargets,
            myOptions,
            myConfigurations,
            myBuildToolVersion
        );
    }
}
