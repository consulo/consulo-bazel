package org.jetbrains.bazel.clion.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.LanguageClass.LanguageClassProvider;
import org.jetbrains.bazel.commons.TargetKind;

import java.util.List;
import java.util.Set;

// consulo: upstream top-level declarations of CcLanguageClass.kt
public final class CcLanguageClass {
    private CcLanguageClass() {
    }

    public static final LanguageClass CC_LANGUAGE_CLASS = new LanguageClass("cc", Set.of());

    public static boolean includesCc(TargetKind receiver) {
        return receiver.languageClasses().contains(CC_LANGUAGE_CLASS);
    }
}

@ExtensionImpl
class CcLanguageClassProvider implements LanguageClassProvider {
    public CcLanguageClassProvider() {
    }

    @Override
    public List<LanguageClass> getLanguages() {
        return List.of(CcLanguageClass.CC_LANGUAGE_CLASS);
    }
}
