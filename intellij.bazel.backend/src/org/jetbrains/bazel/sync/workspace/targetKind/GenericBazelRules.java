package org.jetbrains.bazel.sync.workspace.targetKind;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService.TargetKindProvider;

import java.util.Set;

@ExtensionImpl(order = "last")
class GenericBazelRules implements TargetKindProvider {
    private final Set<TargetKind> myTargetKinds =
        Set.of(
            new TargetKind("test_suite", Set.of(), RuleType.TEST),
            new TargetKind("web_test", Set.of(), RuleType.TEST),
            new TargetKind("sh_test", Set.of(), RuleType.TEST),
            new TargetKind("sh_library", Set.of(), RuleType.LIBRARY),
            new TargetKind("sh_binary", Set.of(), RuleType.BINARY)
        );

    public GenericBazelRules() {
    }

    @Override
    public Set<TargetKind> getTargetKinds() {
        return myTargetKinds;
    }
}
