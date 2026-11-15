package org.jetbrains.bazel.clion.sync;

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.clion.BazelCLionFeatureFlags;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;

import java.util.List;

@ExtensionImpl
public class CcRuleSetProvider implements BazelRuleSetProvider {
    // consulo: upstream private top-level val CcRuleSet
    private static final BazelRuleSet CC_RULE_SET = new BazelRuleSet(
        Rules.CC,
        List.of("rules_cc"),
        true,
        List.of("CcInfo"),
        List.of("https://github.com/bazelbuild/rules_cc")
    );
    // consulo: upstream private top-level val ForeignCcRuleSet
    private static final BazelRuleSet FOREIGN_CC_RULE_SET = new BazelRuleSet(
        Rules.FOREIGN_CC,
        List.of("rules_foreign_cc"),
        false,
        List.of(),
        List.of("https://github.com/bazel-contrib/rules_foreign_cc")
    );

    public CcRuleSetProvider() {
    }

    @Override
    public List<BazelRuleSet> ruleSets() {
        return BazelCLionFeatureFlags.isCLionEnabled() ? List.of(CC_RULE_SET, FOREIGN_CC_RULE_SET) : List.of();
    }
}
