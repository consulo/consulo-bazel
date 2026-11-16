package org.jetbrains.bazel.rust.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.LanguageClass.LanguageClassProvider;
import org.jetbrains.bazel.commons.TargetKind;

import java.util.List;
import java.util.Set;

/**
 * consulo: Consulo-only, like {@code JavaLanguageClass} (upstream BSP had {@code Language.RUST} of the server model).
 */
public final class RustLanguageClass {
    private RustLanguageClass() {
    }

    public static final LanguageClass RUST = new LanguageClass("rust", Set.of("rs"));

    public static boolean includesRust(TargetKind receiver) {
        return receiver.languageClasses().contains(RUST);
    }
}

@ExtensionImpl
class RustLanguageClassProvider implements LanguageClassProvider {
    public RustLanguageClassProvider() {
    }

    @Override
    public List<LanguageClass> getLanguages() {
        return List.of(RustLanguageClass.RUST);
    }
}
