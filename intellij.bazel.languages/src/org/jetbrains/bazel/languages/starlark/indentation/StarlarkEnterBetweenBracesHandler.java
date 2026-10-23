package org.jetbrains.bazel.languages.starlark.indentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.action.EnterBetweenBracesDelegate;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;

@ExtensionImpl
public class StarlarkEnterBetweenBracesHandler extends EnterBetweenBracesDelegate {
    @Override
    public boolean isBracePair(char lBrace, char rBrace) {
        return super.isBracePair(lBrace, rBrace) || lBrace == '[' && rBrace == ']';
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
