package org.jetbrains.bazel.sync.workspace.snapshot;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.BuildTarget;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface ExecutableTargetsIndex {
    List<Label> executableTargetsFor(Label label);

    ExecutableTargetsIndex EMPTY = new ExecutableTargetsIndex() {
        @Override
        public List<Label> executableTargetsFor(Label label) {
            return List.of();
        }
    };

    class InMemoryExecutableTargetsIndex implements ExecutableTargetsIndex {
        private final Map<Label, List<Label>> myHash2Keys;

        InMemoryExecutableTargetsIndex(Map<Label, List<Label>> hash2Keys) {
            myHash2Keys = hash2Keys;
        }

        @Override
        public List<Label> executableTargetsFor(Label label) {
            List<Label> result = myHash2Keys.get(label);
            return result != null ? result : List.of();
        }
    }

    final class ExecutableTargetsIndexBuilder {
        private ExecutableTargetsIndexBuilder() {
        }

        public static ExecutableTargetsIndex build(
            WorkspaceTargetGraph targetGraph,
            int importDepth,
            Collection<BuildTarget> targets
        ) {
            return new InMemoryExecutableTargetsIndex(buildExecutableTargetsIndex(targetGraph, importDepth, targets));
        }

        // consulo: internal upstream, used by the persistent snapshot storage
        public static Map<Label, List<Label>> buildExecutableTargetsIndex(
            WorkspaceTargetGraph targetGraph,
            int importDepth,
            Collection<BuildTarget> targets
        ) {
            Map<WorkspaceTargetKey, BuildTarget> byKey = new HashMap<>(targets.size());
            for (BuildTarget target : targets) {
                byKey.put(target.getKey(), target);
            }
            List<BuildTarget> imported = targetGraph.findAllTargetsAtDepth(importDepth, importDepth < 0, true)
                .map(byKey::get)
                .filter(Objects::nonNull)
                .toList();
            Map<Label, BuildTarget> labelToTarget = new HashMap<>(imported.size());
            for (BuildTarget it : imported) {
                labelToTarget.put(BuildTarget.getId(it), it);
            }
            return new LinkedHashMap<Label, List<Label>>(ExecutableTargetsComputer.calculateExecutableTargets(imported, labelToTarget));
        }
    }
}
