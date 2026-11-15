// consulo: package org.jetbrains.bazel.sync is also a package of consulo.bazel.base (JPMS split package)
package org.jetbrains.bazel.sync.java;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.LanguageClass.LanguageClassProvider;
import org.jetbrains.bazel.commons.TargetKind;

import java.util.List;
import java.util.Set;

public final class JavaLanguageClass {
    private JavaLanguageClass() {
    }

    public static final LanguageClass JAVA = new LanguageClass("java", Set.of("java"));
    public static final LanguageClass SCALA = new LanguageClass("scala", Set.of("scala"), JAVA);
    public static final LanguageClass KOTLIN = new LanguageClass("kotlin", Set.of("kt"), JAVA);

    public static boolean includesKotlin(TargetKind receiver) {
        return receiver.languageClasses().contains(KOTLIN);
    }

    public static boolean includesJava(TargetKind receiver) {
        return receiver.languageClasses().contains(JAVA);
    }

    public static boolean includesScala(TargetKind receiver) {
        return receiver.languageClasses().contains(SCALA);
    }

    public static boolean isJvmTarget(TargetKind receiver) {
        return includesJava(receiver) || includesKotlin(receiver) || includesScala(receiver);
    }
}

@ExtensionImpl
class JavaLanguageClassProvider implements LanguageClassProvider {
    public JavaLanguageClassProvider() {
    }

    @Override
    public List<LanguageClass> getLanguages() {
        return List.of(JavaLanguageClass.JAVA, JavaLanguageClass.SCALA, JavaLanguageClass.KOTLIN);
    }
}
