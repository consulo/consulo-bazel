package org.jetbrains.bazel.sync.workspace.snapshot;

import org.jetbrains.bazel.label.Label;

// consulo: upstream declares the workspace snapshot types in WorkspaceSnapshot.kt (commons); each public type
// has its own file with the upstream fully qualified name
/**
 * Unique target identifier
 *
 * @param label         Bazel target label
 * @param configuration Unique target configuration identifier can be empty
 */
public record WorkspaceTargetKey(
    Label label,
    WorkspaceConfigurationId configuration,
    WorkspaceAspectIds aspectIds
) {
    public WorkspaceTargetKey(Label label) {
        this(label, WorkspaceConfigurationId.EMPTY);
    }

    public WorkspaceTargetKey(Label label, WorkspaceConfigurationId configuration) {
        this(label, configuration, WorkspaceAspectIds.EMPTY);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TargetKey(");
        builder.append(label);
        if (!configuration.equals(WorkspaceConfigurationId.EMPTY)) {
            builder.append(", conf=").append(configuration);
        }
        if (!aspectIds.equals(WorkspaceAspectIds.EMPTY)) {
            builder.append(", aspects=").append(String.join(";", aspectIds.getIds()));
        }
        builder.append(')');
        return builder.toString();
    }
}
