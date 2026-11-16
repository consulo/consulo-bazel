package org.jetbrains.bazel.rust.sync;

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;

import java.util.List;

/**
 * Enables the {@code rust_info} module of the aspect for workspaces that use rules_rust.
 * <p>
 * consulo: Consulo-only, like {@code JavaBazelRuleSetPlugin}.
 */
@ExtensionImpl
public class RustBazelRuleSetPlugin implements BazelRuleSetProvider {
    public static final BazelRuleSet RustRuleSet = new BazelRuleSet(
        Rules.RUST,
        List.of("rules_rust"),
        false,
        List.of(),
        List.of("https://github.com/bazelbuild/rules_rust/")
    );

    public RustBazelRuleSetPlugin() {
    }

    @Override
    public List<BazelRuleSet> ruleSets() {
        return List.of(RustRuleSet);
    }
}
