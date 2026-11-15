package org.jetbrains.bazel.golang.sync;

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;

import java.util.List;

@ExtensionImpl
public class GoRuleSetPlugin implements BazelRuleSetProvider {
    // consulo: upstream private top-level val GoRuleSet
    private static final BazelRuleSet GoRuleSet = new BazelRuleSet(
        Rules.GO,
        List.of("rules_go", "io_bazel_rules_go"),
        false,
        List.of(),
        List.of("https://github.com/bazel-contrib/rules_go/")
    );

    @Override
    public List<BazelRuleSet> ruleSets() {
        return List.of(GoRuleSet);
    }
}
