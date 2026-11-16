package org.jetbrains.bazel.server.bep;

import consulo.logging.Logger;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.server.bep.BepOutputBuilder.TextProtoDepSet;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;

import java.nio.file.Path;
import java.util.ArrayDeque;
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

    Set<String> getInfoOutputGroupFileSets() {
        return myInfoOutputGroupFileSets;
    }

    Map<String, TextProtoDepSet> getTextProtoFileSets() {
        return myTextProtoFileSets;
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
        ArrayDeque<String> toVisit = new ArrayDeque<>(rootIds);
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
        return mergeAll(List.of(this, anotherBepOutput));
    }

    private static final Logger LOG = Logger.getInstance(BepOutput.class);

    public static BepOutput mergeAll(Iterable<BepOutput> outputs) {
        BepOutputBuilder builder = new BepOutputBuilder();
        outputs.forEach(builder::add);
        return builder.build();
    }
}
