package org.jetbrains.bazel.sync.workspace.languages.java;

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;

import java.util.List;

@ExtensionImpl
public class JavaBazelRuleSetPlugin implements BazelRuleSetProvider {
    // consulo: upstream top-level val JavaRuleSet
    public static final BazelRuleSet JavaRuleSet = new BazelRuleSet(
        Rules.JAVA,
        List.of("rules_java"),
        true,
        List.of("JavaInfo", "java_common", "JavaPluginInfo", "java_binary", "java_library"),
        List.of("https://github.com/bazelbuild/rules_java/")
    );

    public JavaBazelRuleSetPlugin() {
    }

    @Override
    public List<BazelRuleSet> ruleSets() {
        return List.of(JavaRuleSet);
    }
}
