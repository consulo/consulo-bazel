package org.jetbrains.bazel.server.bep;

import com.google.common.collect.Queues;
import consulo.logging.Logger;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.server.bep.BepOutputBuilder.TextProtoDepSet;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class BepOutput {
    private final Set<String> myInfoOutputGroupFileSets;
    private final Map<String, TextProtoDepSet> myTextProtoFileSets;
    private final Set<WorkspaceTargetKey> myRootTargets;
    private final List<String> myOptions;
    private final Map<WorkspaceConfigurationId, WorkspaceConfiguration> myConfigurations;
    private BazelRelease myBuildToolVersion;

    public BepOutput(
        Set<String> infoOutputGroupFileSets,
        Map<String, TextProtoDepSet> textProtoFileSets,
        Set<WorkspaceTargetKey> rootTargets,
        List<String> options,
        Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations,
        BazelRelease buildToolVersion
    ) {
        myInfoOutputGroupFileSets = infoOutputGroupFileSets;
        myTextProtoFileSets = textProtoFileSets;
        myRootTargets = rootTargets;
        myOptions = options;
        myConfigurations = configurations;
        myBuildToolVersion = buildToolVersion;
    }

    // consulo: upstream default values of all constructor parameters
    public BepOutput() {
        this(Set.of(), Map.of(), Set.of(), List.of(), Map.of(), BazelRelease.FALLBACK_VERSION);
    }

    public List<String> getOptions() {
        return myOptions;
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

    public Set<WorkspaceTargetKey> rootTargets() {
        return myRootTargets;
    }

    public Set<Path> filesByInfoOutputGroup() {
        Set<String> rootIds = myInfoOutputGroupFileSets;
        if (rootIds.isEmpty()) {
            return Set.of();
        }
        Set<Path> result = new HashSet<>(rootIds.size());
        ArrayDeque<String> toVisit = Queues.newArrayDeque(rootIds);
        Set<String> visited = new HashSet<>(rootIds);
        while (!toVisit.isEmpty()) {
            String fileSetId = toVisit.remove();
            TextProtoDepSet fileSet = myTextProtoFileSets.get(fileSetId);
            if (fileSet == null) {
                LOG.error("Missing textproto for " + fileSetId);
                continue;
            }
            result.addAll(fileSet.files());
            for (String child : fileSet.children()) {
                if (visited.add(child)) {
                    toVisit.add(child);
                }
            }
        }
        return result;
    }

    /**
     * Rename all namedSetOfFiles occurring in the BepOutput by encoding the run number.
     */
    public BepOutput renameNamedSets(int runNumber) {
        Function<String, String> renameSetName = name -> runNumber + "." + name;
        Set<String> infoOutputGroupFileSets = new LinkedHashSet<>();
        for (String it : myInfoOutputGroupFileSets) {
            infoOutputGroupFileSets.add(renameSetName.apply(it));
        }
        Map<String, TextProtoDepSet> textProtoFileSets = new LinkedHashMap<>();
        for (Map.Entry<String, TextProtoDepSet> entry : myTextProtoFileSets.entrySet()) {
            textProtoFileSets.put(
                renameSetName.apply(entry.getKey()),
                new TextProtoDepSet(entry.getValue().files(), entry.getValue().children().stream().map(renameSetName).toList())
            );
        }
        return new BepOutput(
            infoOutputGroupFileSets,
            textProtoFileSets,
            myRootTargets,
            myOptions,
            myConfigurations,
            myBuildToolVersion
        );
    }


    public BepOutput merge(BepOutput anotherBepOutput) {
        Set<String> infoOutputGroupFileSets = new LinkedHashSet<>(myInfoOutputGroupFileSets);
        infoOutputGroupFileSets.addAll(anotherBepOutput.myInfoOutputGroupFileSets);

        Set<String> textProtoFileSetKeys = new LinkedHashSet<>(myTextProtoFileSets.keySet());
        textProtoFileSetKeys.addAll(anotherBepOutput.myTextProtoFileSets.keySet());
        Map<String, TextProtoDepSet> textProtoFileSets = new LinkedHashMap<>();
        for (String k : textProtoFileSetKeys) {
            TextProtoDepSet left = myTextProtoFileSets.getOrDefault(k, new TextProtoDepSet(List.of(), List.of()));
            TextProtoDepSet right = anotherBepOutput.myTextProtoFileSets.getOrDefault(k, new TextProtoDepSet(List.of(), List.of()));
            List<Path> files = new ArrayList<>(left.files());
            files.addAll(right.files());
            List<String> children = new ArrayList<>(left.children());
            children.addAll(right.children());
            textProtoFileSets.put(k, new TextProtoDepSet(files, children));
        }

        Set<WorkspaceTargetKey> rootTargets = new LinkedHashSet<>(myRootTargets);
        rootTargets.addAll(anotherBepOutput.myRootTargets);

        List<String> options = new ArrayList<>(myOptions);
        options.addAll(anotherBepOutput.myOptions);

        Map<WorkspaceConfigurationId, WorkspaceConfiguration> configurations = new LinkedHashMap<>(myConfigurations);
        configurations.putAll(anotherBepOutput.myConfigurations);

        // pick first that isn't `BazelRelease.FALLBACK_VERSION`
        BazelRelease buildToolVersion;
        if (!myBuildToolVersion.equals(BazelRelease.FALLBACK_VERSION)) {
            buildToolVersion = myBuildToolVersion;
        }
        else if (!anotherBepOutput.myBuildToolVersion.equals(BazelRelease.FALLBACK_VERSION)) {
            buildToolVersion = anotherBepOutput.myBuildToolVersion;
        }
        else {
            buildToolVersion = BazelRelease.FALLBACK_VERSION;
        }

        return new BepOutput(
            infoOutputGroupFileSets,
            textProtoFileSets,
            rootTargets,
            options,
            configurations,
            buildToolVersion
        );
    }

    private static final Logger LOG = Logger.getInstance(BepOutput.class);
}
