package org.jetbrains.bazel.sync.workspace.languages.scala;

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;

import java.util.List;

@ExtensionImpl
public class ScalaBazelRuleSetPlugin implements BazelRuleSetProvider {
    // consulo: upstream private top-level val ScalaRuleSet
    private static final BazelRuleSet SCALA_RULE_SET = new BazelRuleSet(
        Rules.SCALA,
        List.of("rules_scala", "io_bazel_rules_scala"),
        false,
        List.of(),
        List.of("https://github.com/bazel-contrib/rules_scala/")
    );

    public ScalaBazelRuleSetPlugin() {
    }

    @Override
    public List<BazelRuleSet> ruleSets() {
        return List.of(SCALA_RULE_SET);
    }
}
