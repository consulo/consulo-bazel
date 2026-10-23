package org.jetbrains.bazel.languages.starlark.matching;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.action.LanguageQuoteHandler;
import consulo.language.editor.action.SimpleTokenSetQuoteHandler;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;

@ExtensionImpl
public class StarlarkQuoteHandler extends SimpleTokenSetQuoteHandler implements LanguageQuoteHandler {
    public StarlarkQuoteHandler() {
        super(StarlarkTokenSets.STRINGS);
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
