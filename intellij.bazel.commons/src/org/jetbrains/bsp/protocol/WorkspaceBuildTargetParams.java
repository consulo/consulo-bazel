package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record WorkspaceBuildTargetParams(
    WorkspaceBuildTargetSelector selector,
    boolean build,
    @Nullable List<Label> allTargets, /* all known targets, if any, from first phase */
    RepoMapping repoMapping,
    TaskId taskId) {
    public sealed interface WorkspaceBuildTargetSelector {
        final class AllTargets implements WorkspaceBuildTargetSelector {
            public static final AllTargets INSTANCE = new AllTargets();

            private AllTargets() {
            }
        }

        record SpecificTargets(List<Label> targets) implements WorkspaceBuildTargetSelector {
        }
    }
}
