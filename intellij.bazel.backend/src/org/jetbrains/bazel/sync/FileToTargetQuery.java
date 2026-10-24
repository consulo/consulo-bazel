package org.jetbrains.bazel.sync;

import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import org.jetbrains.bazel.bazelrunner.params.BazelFlag;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryOutput;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryParams;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.SyncFileClassifier.SyncFileKind;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class FileToTargetQuery {
    private FileToTargetQuery() {
    }

    // consulo: overload for the upstream default argument (taskId = null)
    public static CompletableFuture<Map<Label, List<Label>>> findDependantTargetsFromFiles(
        BazelServerFacade server,
        Set<Label> fileLabels
    ) {
        return findDependantTargetsFromFiles(server, fileLabels, null);
    }

    /**
     * Find targets that own specific source file. Source files are identified by labels.
     */
    public static CompletableFuture<Map<Label, List<Label>>> findDependantTargetsFromFiles(
        BazelServerFacade server,
        Set<Label> fileLabels,
        @Nullable TaskId taskId
    ) {
        if (fileLabels.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }
        return server.query(
                new BazelQueryParams<>(
                    expressionOf(fileLabels),
                    BazelQueryOutput.Targets.INSTANCE,
                    taskId,
                    true,
                    // we need canonical labels here
                    List.of(BazelFlag.consistentLabels(true))
                )
            )
            .thenApply(result -> {
                Map<Label, List<Label>> targets = new LinkedHashMap<>();
                for (Label fileLabel : fileLabels) {
                    targets.put(fileLabel, List.of());
                }
                targets.putAll(targetsByFile(result.result(), fileLabels));
                return targets;
            });
    }

    // consulo: overload for the upstream default argument (taskId = null)
    public static CompletableFuture<Map<Path, List<Label>>> findDependantTargetsFromPaths(
        BazelServerFacade server,
        Collection<Path> paths,
        RepoMapping repoMapping
    ) {
        return findDependantTargetsFromPaths(server, paths, repoMapping, null);
    }

    public static CompletableFuture<Map<Path, List<Label>>> findDependantTargetsFromPaths(
        BazelServerFacade server,
        Collection<Path> paths,
        RepoMapping repoMapping,
        @Nullable TaskId taskId
    ) {
        SyncFileClassifier classifier = new SyncFileClassifier(repoMapping, server.getBazelInfo());
        Map<Label, Path> pathsByFileLabel = new LinkedHashMap<>();
        for (Path path : paths) {
            if (classifier.classify(path).kind() instanceof SyncFileKind.Source kind) {
                pathsByFileLabel.put(kind.label(), path);
            }
        }
        return findDependantTargetsFromFiles(server, pathsByFileLabel.keySet(), taskId)
            .thenApply(targetsByFileLabel -> {
                Map<Path, List<Label>> result = new LinkedHashMap<>();
                for (Map.Entry<Label, Path> entry : pathsByFileLabel.entrySet()) {
                    List<Label> targets = targetsByFileLabel.get(entry.getKey());
                    result.put(entry.getValue(), targets != null ? targets : List.of());
                }
                return result;
            });
    }

    private static String expressionOf(Set<Label> fileLabels) {
        return fileLabels.stream()
            .map(it -> "\"" + it + "\"")
            .collect(Collectors.joining(" + ", "same_pkg_direct_rdeps(", ")"));
    }

    // here we build map of file to owning targets in bulk
    // `targetsByFile` scan all output attributes for presence of requested file labels (with respect to `nodep`)
    // and combine it into single map, by that we can query multiple files -> target mappings at once
    private static Map<Label, List<Label>> targetsByFile(List<Build.Target> receiver, Set<Label> fileLabels) {
        Map<Label, List<Label>> targetsByFile = new LinkedHashMap<>();
        for (Build.Target target : receiver) {
            if (target.getType() != Build.Target.Discriminator.RULE) {
                continue;
            }
            Label ruleLabel = Label.parseOrNull(target.getRule().getName());
            if (ruleLabel == null) {
                continue;
            }
            for (Label fileLabel : usedFileLabels(target.getRule(), fileLabels)) {
                targetsByFile.computeIfAbsent(fileLabel, it -> new ArrayList<>()).add(ruleLabel);
            }
        }
        return targetsByFile;
    }

    private static Set<Label> usedFileLabels(Build.Rule receiver, Set<Label> fileLabels) {
        Set<Label> result = new HashSet<>();
        for (Build.Attribute attribute : receiver.getAttributeList()) {
            if (attribute.getNodep()) {
                continue;
            }
            for (String value : labelValues(attribute)) {
                Label label = Label.parseOrNull(value);
                if (label != null && fileLabels.contains(label)) {
                    result.add(label);
                }
            }
        }
        return result;
    }

    private static List<String> labelValues(Build.Attribute receiver) {
        switch (receiver.getType()) {
            case LABEL:
            case OUTPUT:
                return List.of(receiver.getStringValue());
            case LABEL_LIST:
            case OUTPUT_LIST:
                return receiver.getStringListValueList();
            case LABEL_KEYED_STRING_DICT:
                return receiver.getLabelKeyedStringDictValueList().stream().map(it -> it.getKey()).toList();
            default:
                return List.of();
        }
    }
}
