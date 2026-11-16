package org.jetbrains.bazel.java.run;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.util.ClassUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.run.synthetic.MainClassSyntheticRunTargetTemplateGenerator;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class JavaSyntheticRunTargetTemplateGenerator extends MainClassSyntheticRunTargetTemplateGenerator {
    @Override
    public boolean isSupported(BuildTargetInfo target) {
        return JavaLanguageClass.isJvmTarget(target.kind());
    }

    @Override
    protected @Nullable String getMainClass(PsiElement element) {
        PsiClass psiClass = PsiTreeUtil.getNonStrictParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return null;
        }
        // consulo: JvmClassUtil.getJvmClassName -> ClassUtil.getJVMClassName
        return ClassUtil.getJVMClassName(psiClass);
    }

    @Override
    protected String getBuildContent(BuildTargetInfo target, String syntheticTarget, String mainClass) {
        // consulo: upstream @Language("starlark") annotated local
        String build = """
            load("@rules_java//java:defs.bzl", "java_binary")

            java_binary(
              name = "%s",
              main_class = "%s",
              runtime_deps = ["%s"],
            )""".formatted(syntheticTarget, mainClass, target.id());
        return build;
    }

    // consulo: language-bound extension (upstream XML language="JAVA")
    @Override
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }
}
