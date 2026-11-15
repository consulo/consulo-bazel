package org.jetbrains.bazel.sync.workspace.languages.jvm;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService.TargetKindProvider;

import java.util.Set;

@ExtensionImpl
public class JavaBazelRules implements TargetKindProvider {
    private final Set<TargetKind> myTargetKinds =
        Set.of(
            new TargetKind("java_library", Set.of(JavaLanguageClass.JAVA), RuleType.LIBRARY),
            new TargetKind("java_binary", Set.of(JavaLanguageClass.JAVA), RuleType.BINARY),
            JAVA_TEST_KIND,
            // a workaround, register this target type as Java module in IntelliJ IDEA
            new TargetKind("intellij_plugin_debug_target", Set.of(JavaLanguageClass.JAVA), RuleType.BINARY),
            new TargetKind("kt_jvm_library", Set.of(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.LIBRARY),
            new TargetKind("kt_jvm_binary", Set.of(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.BINARY),
            new TargetKind("kt_jvm_test", Set.of(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.TEST),
            new TargetKind("scala_library", Set.of(JavaLanguageClass.JAVA, JavaLanguageClass.SCALA), RuleType.LIBRARY),
            new TargetKind("scala_binary", Set.of(JavaLanguageClass.JAVA, JavaLanguageClass.SCALA), RuleType.BINARY),
            new TargetKind("scala_test", Set.of(JavaLanguageClass.JAVA, JavaLanguageClass.SCALA), RuleType.TEST)
        );

    public JavaBazelRules() {
    }

    @Override
    public Set<TargetKind> getTargetKinds() {
        return myTargetKinds;
    }

    public static final TargetKind JAVA_TEST_KIND = new TargetKind("java_test", Set.of(JavaLanguageClass.JAVA), RuleType.TEST);
}
