package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.bazel.documentation.BazelDocumentationElement;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;
import org.jetbrains.bazel.languages.starlark.documentation.BazelGlobalFunctionDocumentationSymbol;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jspecify.annotations.Nullable;

// consulo: upstream PsiSymbolReference to a DocumentationSymbol -> classic reference resolving to a BazelDocumentationElement
public class BazelGlobalFunctionReference extends PsiReferenceBase<StarlarkCallExpression> {
    private final BazelGlobalFunction myRule;

    public BazelGlobalFunctionReference(StarlarkCallExpression element, BazelGlobalFunction rule) {
        super(element, true);
        myRule = rule;
    }

    @Override
    public StarlarkCallExpression getElement() {
        return myElement;
    }

    @Override
    @RequiredReadAction
    public TextRange getRangeInElement() {
        PsiElement calledExpression = myElement.getCalledExpression();
        return calledExpression != null
            ? StarlarkBaseElement.relativeTo(calledExpression.getTextRange(), myElement)
            : TextRange.EMPTY_RANGE;
    }

    // consulo: upstream resolveReference(): Collection<Symbol>
    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        return BazelDocumentationElement.of(myElement, new BazelGlobalFunctionDocumentationSymbol(myRule, myElement.getProject()));
    }
}
