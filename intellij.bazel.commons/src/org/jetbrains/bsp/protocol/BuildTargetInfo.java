package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record BuildTargetInfo(
    WorkspaceTargetKey key,
    TargetKind kind,
    @Nullable String generatorName,
    boolean isTestOnly,
    List<String> tags,
    String workspaceName
) {
    // consulo: upstream top-level factory function BuildTargetInfo(label, kind, tags = listOf())
    public BuildTargetInfo(Label label, TargetKind kind, List<String> tags) {
        this(
            new WorkspaceTargetKey(label),
            kind,
            null,
            false,
            tags,
            "_main"
        );
    }

    // consulo: overload for the upstream default argument (tags = listOf())
    public BuildTargetInfo(Label label, TargetKind kind) {
        this(label, kind, List.of());
    }

    public Label id() {
        return key.label();
    }

    /**
     * From Bazel doc (https://bazel.build/reference/be/common-definitions)
     * manual tag will exclude the target from expansion of target pattern wildcards (..., :*, :all, etc.) and test_suite rules which do not
     * list the test explicitly when computing the set of top-level targets to build/run for the build, test, and coverage commands
     */
    public static boolean isManual(BuildTargetInfo receiver) {
        return receiver.tags().contains(BuildTargetTag.MANUAL);
    }
}
