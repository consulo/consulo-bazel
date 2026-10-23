package org.jetbrains.bazel.languages.starlark.matching;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiFile;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class StarlarkBraceMatcher implements PairedBraceMatcher {
    @Override
    public BracePair[] getPairs() {
        return new BracePair[]{
            new BracePair(StarlarkTokenTypes.LPAR, StarlarkTokenTypes.RPAR, false),
            new BracePair(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.RBRACKET, false),
            new BracePair(StarlarkTokenTypes.LBRACE, StarlarkTokenTypes.RBRACE, false),
        };
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(IElementType lbraceType, @Nullable IElementType contextType) {
        return StarlarkTokenSets.WHITESPACE.contains(contextType) ||
            contextType == StarlarkTokenTypes.COMMENT ||
            contextType == StarlarkTokenTypes.COLON ||
            contextType == StarlarkTokenTypes.COMMA ||
            contextType == StarlarkTokenTypes.RPAR ||
            contextType == StarlarkTokenTypes.RBRACKET ||
            contextType == StarlarkTokenTypes.RBRACE ||
            contextType == StarlarkTokenTypes.LBRACE ||
            contextType == null;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
