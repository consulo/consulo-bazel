package org.jetbrains.bazel.languages.starlark.commenter;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Commenter;
import consulo.language.Language;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class StarlarkCommenter implements Commenter {
    @Override
    public String getLineCommentPrefix() {
        return "# ";
    }

    @Override
    public @Nullable String getBlockCommentPrefix() {
        return null;
    }

    @Override
    public @Nullable String getBlockCommentSuffix() {
        return null;
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix() {
        return null;
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
