package org.jetbrains.bazel.sync.workspace.snapshot;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class WorkspaceHelper {
    private WorkspaceHelper() {
    }

    public static WorkspaceTargetKey toWorkspaceTargetKey(IntellijIdeInfo.TargetKey receiver) {
        return new WorkspaceTargetKey(
            Label.parse(receiver.getLabel()),
            WorkspaceConfigurationId.of(receiver.getConfiguration()),
            WorkspaceAspectIds.of(receiver.getAspectIdsList())
        );
    }

    public static @Nullable WorkspaceTargetKey getWorkspaceTargetKey(BuildEventStreamProtos.BuildEventId receiver) {
        if (!receiver.hasTargetCompleted()) {
            return null;
        }
        Label label = Label.parseOrNull(receiver.getTargetCompleted().getLabel());
        if (label == null) {
            return null;
        }
        return new WorkspaceTargetKey(
            label,
            WorkspaceConfigurationId.of(receiver.getTargetCompleted().getConfiguration().getId()),
            WorkspaceAspectIds.of(List.of(receiver.getTargetCompleted().getAspect()))
        );
    }

    public static List<Label> getLabelIds(WorkspaceAspectIds receiver) {
        List<Label> result = new ArrayList<>();
        for (String it : receiver.getIds()) {
            Label label = Label.parseOrNull(it);
            if (label != null) {
                result.add(label);
            }
        }
        return result;
    }

    public static boolean isExecConfig(WorkspaceConfiguration receiver) {
        return receiver.summary().mnemonic().endsWith("-exec") || receiver.summary().mnemonic().equals("exec");
    }
}
