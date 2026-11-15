package org.jetbrains.bazel.languages.bazelquery.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.bazel.documentation.BazelDocumentationElement;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunction;
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunctionSymbol;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryCommand;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

// consulo: upstream PsiSymbolReference (IntelliJ symbol API) -> classic soft reference resolving to a documentation element;
// the upstream property `element` is PsiReferenceBase.getElement()
class BazelQueryFunctionReference extends PsiReferenceBase<BazelQueryCommand> {
    @RequiredReadAction
    BazelQueryFunctionReference(BazelQueryCommand element) {
        super(element, textRange(element), true);
    }

    @RequiredReadAction
    private static TextRange textRange(BazelQueryCommand element) {
        PsiElement name = element.getNameElement();
        return name != null ? name.getTextRangeInParent() : element.getTextRange();
    }

    public TextRange getTextRange() {
        return getRangeInElement();
    }

    @RequiredReadAction
    public Collection<BazelQueryFunctionSymbol> resolveReference() {
        BazelQueryCommand element = getElement();
        BazelQueryFunction function = BazelQueryFunction.byName(getRangeInElement().substring(element.getText()));
        return function != null ? List.of(new BazelQueryFunctionSymbol(function, element.getProject())) : List.of();
    }

    // consulo: the function symbol is resolved to a light element carrying its documentation target
    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        Collection<BazelQueryFunctionSymbol> symbols = resolveReference();
        return symbols.isEmpty() ? null : BazelDocumentationElement.of(getElement(), symbols.iterator().next());
    }
}
