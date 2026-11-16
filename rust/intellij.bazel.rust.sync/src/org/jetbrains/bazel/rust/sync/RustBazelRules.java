package org.jetbrains.bazel.rust.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService.TargetKindProvider;

import java.util.Set;

/**
 * The rules_rust rule kinds.
 * <p>
 * consulo: Consulo-only, like {@code JavaBazelRules}.
 */
@ExtensionImpl
public class RustBazelRules implements TargetKindProvider {
    private final Set<TargetKind> myTargetKinds =
        Set.of(
            new TargetKind("rust_library", Set.of(RustLanguageClass.RUST), RuleType.LIBRARY),
            new TargetKind("rust_proc_macro", Set.of(RustLanguageClass.RUST), RuleType.LIBRARY),
            new TargetKind("rust_shared_library", Set.of(RustLanguageClass.RUST), RuleType.LIBRARY),
            new TargetKind("rust_static_library", Set.of(RustLanguageClass.RUST), RuleType.LIBRARY),
            new TargetKind("rust_binary", Set.of(RustLanguageClass.RUST), RuleType.BINARY),
            new TargetKind("rust_test", Set.of(RustLanguageClass.RUST), RuleType.TEST)
        );

    public RustBazelRules() {
    }

    @Override
    public Set<TargetKind> getTargetKinds() {
        return myTargetKinds;
    }
}
