package org.jetbrains.bazel.languages.bazelquery.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.CharFilter;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.psi.PsiFile;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage;
import org.jspecify.annotations.Nullable;

/**
 * BazelQueryCharFilter is needed to avoid hiding
 * the completions after typing special characters
 * like '/' or ':', which often appear in targets.
 */
@ExtensionImpl
class BazelQueryCharFilter extends CharFilter {
    public BazelQueryCharFilter() {
    }

    @Override
    public @Nullable Result acceptChar(
        char c,
        int prefixLength,
        Lookup lookup
    ) {
        PsiFile psiFile = lookup.getPsiFile();
        if (psiFile == null || psiFile.getLanguage() != BazelQueryLanguage.INSTANCE) {
            return null;
        }

        return Result.ADD_TO_PREFIX;
    }
}
