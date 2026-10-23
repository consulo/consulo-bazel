package org.jetbrains.bazel.languages.bazelrc.matching;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.action.LanguageQuoteHandler;
import consulo.language.editor.action.SimpleTokenSetQuoteHandler;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenSets;

@ExtensionImpl
public class BazelrcQuoteHandler extends SimpleTokenSetQuoteHandler implements LanguageQuoteHandler {
    public BazelrcQuoteHandler() {
        super(BazelrcTokenSets.BIBI);
    }

    @Override
    public Language getLanguage() {
        return BazelrcLanguage.INSTANCE;
    }
}
