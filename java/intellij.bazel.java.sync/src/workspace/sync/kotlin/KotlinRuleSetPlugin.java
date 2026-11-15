package org.jetbrains.bazel.sync.workspace.languages.kotlin;

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;

import java.util.List;

@ExtensionImpl
public class KotlinRuleSetPlugin implements BazelRuleSetProvider {
    // consulo: upstream private top-level val KotlinRuleSet
    private static final BazelRuleSet KOTLIN_RULE_SET = new BazelRuleSet(
        Rules.KOTLIN,
        List.of("rules_kotlin", "io_bazel_rules_kotlin"),
        false,
        List.of(),
        List.of("https://github.com/bazelbuild/rules_kotlin/")
    );

    public KotlinRuleSetPlugin() {
    }

    @Override
    public List<BazelRuleSet> ruleSets() {
        return List.of(KOTLIN_RULE_SET);
    }
}
