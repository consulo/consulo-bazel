package org.jetbrains.bazel.label;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.Dependency.DependencyType;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelper;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;

// TODO: move to backend-only entity and use WorkspaceConfigurationId
public record DependencyLabel(
    WorkspaceTargetKey targetKey,
    DependencyLabelKind kind
) {
    public DependencyLabel(WorkspaceTargetKey targetKey) {
        this(targetKey, DependencyLabelKind.COMPILE);
    }

    public static DependencyLabel parse(String value) {
        return parse(value, false);
    }

    public static DependencyLabel parse(String value, boolean isRuntime) {
        return new DependencyLabel(
            new WorkspaceTargetKey(Label.parse(value)),
            isRuntime
                ? DependencyLabelKind.RUNTIME
                : DependencyLabelKind.COMPILE
        );
    }

    public boolean isRuntime() {
        return kind == DependencyLabelKind.RUNTIME;
    }

    public boolean getExported() {
        return kind == DependencyLabelKind.EXPORTED_COMPILE_TIME;
    }

    public enum DependencyLabelKind {
        COMPILE, EXPORTED_COMPILE_TIME, RUNTIME, TOOLCHAIN
    }

    public static DependencyLabel toDependencyLabel(IntellijIdeInfo.Dependency receiver) {
        DependencyLabelKind kind;
        DependencyType dependencyType = receiver.getDependencyType();
        if (dependencyType == DependencyType.EXPORTED_COMPILE_TIME) {
            kind = DependencyLabelKind.EXPORTED_COMPILE_TIME;
        }
        else if (dependencyType == DependencyType.RUNTIME) {
            kind = DependencyLabelKind.RUNTIME;
        }
        else if (dependencyType == DependencyType.TOOLCHAIN) {
            kind = DependencyLabelKind.TOOLCHAIN;
        }
        else {
            kind = DependencyLabelKind.COMPILE;
        }
        return new DependencyLabel(
            WorkspaceHelper.toWorkspaceTargetKey(receiver.getTarget()),
            kind
        );
    }

    public static boolean isCompile(IntellijIdeInfo.Dependency receiver) {
        return receiver.getDependencyType() == DependencyType.COMPILE_TIME
            || receiver.getDependencyType() == DependencyType.EXPORTED_COMPILE_TIME;
    }
}
