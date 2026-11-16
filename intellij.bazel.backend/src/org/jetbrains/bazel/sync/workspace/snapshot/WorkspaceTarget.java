package org.jetbrains.bazel.sync.workspace.snapshot;

import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bsp.protocol.BuildTarget;

// consulo: upstream top-level functions of WorkspaceTarget.kt
public final class WorkspaceTarget {
    private WorkspaceTarget() {
    }

    public static boolean isTestTarget(BuildTarget receiver) {
        return receiver.isTestOnly() || receiver.getKind().ruleType() == RuleType.TEST;
    }
}
