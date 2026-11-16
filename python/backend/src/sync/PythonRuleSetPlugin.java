package com.intellij.bazel.python.backend.sync;

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;

import java.util.List;

@ExtensionImpl
public class PythonRuleSetPlugin implements BazelRuleSetProvider {
    // consulo: upstream public top-level val PythonRuleSet of PythonRuleSetPlugin.kt
    public static final BazelRuleSet PythonRuleSet = new BazelRuleSet(
        Rules.PYTHON,
        List.of("rules_python"),
        true,
        List.of("PyInfo"),
        List.of("https://github.com/bazel-contrib/rules_python/")
    );

    @Override
    public List<BazelRuleSet> ruleSets() {
        return List.of(PythonRuleSet);
    }
}
