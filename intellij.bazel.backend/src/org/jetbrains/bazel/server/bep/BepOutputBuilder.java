package org.jetbrains.bazel.server.bep;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.NamedSetOfFiles;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.OutputGroup;
import com.intellij.aspect.lib.OutputGroups;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BepOutputBuilder {
    public record TextProtoDepSet(Collection<Path> files, Collection<String> children) {
    }

    private final BazelPathsResolver myBazelPathsResolver;
    private final Set<String> myInfoOutputGroupFileSets = new HashSet<>();
    private final Map<String, TextProtoDepSet> myTextProtoFileSets = new HashMap<>();
    private final Set<WorkspaceTargetKey> myRootTargets = new HashSet<>();
    private final List<String> myOptions = new ArrayList<>();
    private final Map<WorkspaceConfigurationId, WorkspaceConfiguration> myConfigurations = new LinkedHashMap<>();
    private BazelRelease myBuildToolVersion = BazelRelease.FALLBACK_VERSION;

    BepOutputBuilder(BazelPathsResolver bazelPathsResolver) {
        myBazelPathsResolver = bazelPathsResolver;
    }

    public Map<WorkspaceConfigurationId, WorkspaceConfiguration> getConfigurations() {
        return myConfigurations;
    }

    public BazelRelease getBuildToolVersion() {
        return myBuildToolVersion;
    }

    public void setBuildToolVersion(BazelRelease buildToolVersion) {
        myBuildToolVersion = buildToolVersion;
    }

    public void storeNamedSet(String id, NamedSetOfFiles namedSetOfFiles) {
        TextProtoDepSet textProtoDepSet =
            new TextProtoDepSet(
                namedSetOfFiles
                    .getFilesList()
                    .stream()
                    .filter(it -> it.getName().endsWith(Constants.ASPECT_INFO_OUTPUT_EXTENSION))
                    .map(this::toLocalPath)
                    .toList(),
                namedSetOfFiles.getFileSetsList().stream().map(it -> it.getId()).toList()
            );

        myTextProtoFileSets.put(id, textProtoDepSet);
    }

    private Path toLocalPath(BuildEventStreamProtos.File receiver) {
        Path mergedPathPrefix = Path.of(String.join(File.separator, receiver.getPathPrefixList()));
        Path bazelOutputRelativePath = mergedPathPrefix.resolve(receiver.getName());

        return myBazelPathsResolver.resolveOutput(bazelOutputRelativePath);
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
