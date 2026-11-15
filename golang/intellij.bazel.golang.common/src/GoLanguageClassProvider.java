package org.jetbrains.bazel.golang;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.LanguageClass.LanguageClassProvider;

import java.util.List;
import java.util.Set;

@ExtensionImpl
public class GoLanguageClassProvider implements LanguageClassProvider {
    // consulo: upstream top-level `internal object GoLanguageClass` of GoLanguageClassProvider.kt
    public static final class GoLanguageClass {
        private GoLanguageClass() {
        }

        public static final LanguageClass GO = new LanguageClass("go", Set.of("go"));
    }

    @Override
    public List<LanguageClass> getLanguages() {
        return List.of(GoLanguageClass.GO);
    }
}
