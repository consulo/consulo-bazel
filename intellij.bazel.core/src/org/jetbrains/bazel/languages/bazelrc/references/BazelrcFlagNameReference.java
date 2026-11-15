package org.jetbrains.bazel.languages.bazelrc.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.bazel.documentation.BazelDocumentationElement;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol;
import org.jetbrains.bazel.languages.bazelrc.flags.Flag;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

// consulo: upstream PsiSymbolReference (IntelliJ symbol API) -> classic soft reference resolving to a documentation element;
// the upstream property `element` is PsiReferenceBase.getElement()
class BazelrcFlagNameReference extends PsiReferenceBase<BazelrcFlag> {
    @RequiredReadAction
    BazelrcFlagNameReference(BazelrcFlag element) {
        super(element, textRange(element), true);
    }

    @RequiredReadAction
    private static TextRange textRange(BazelrcFlag element) {
        PsiElement name = element.getNameElement();
        return name != null ? name.getTextRangeInParent() : element.getTextRange();
    }

    public TextRange getTextRange() {
        return getRangeInElement();
    }

    @RequiredReadAction
    public Collection<BazelFlagSymbol> resolveReference() {
        BazelrcFlag element = getElement();
        Flag flag = Flag.byName(getRangeInElement().substring(element.getText()));
        return flag != null ? List.of(new BazelFlagSymbol(flag, element.getProject())) : List.of();
    }

    // consulo: the flag symbol is resolved to a light element carrying its documentation target
    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        Collection<BazelFlagSymbol> symbols = resolveReference();
        return symbols.isEmpty() ? null : new BazelDocumentationElement(getElement(), symbols.iterator().next().getDocumentationTarget());
    }
}
