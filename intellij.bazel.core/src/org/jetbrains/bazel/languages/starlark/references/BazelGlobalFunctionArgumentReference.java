package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.bazel.documentation.BazelDocumentationElement;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunctionParameter;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions;
import org.jetbrains.bazel.languages.starlark.documentation.BazelGlobalFunctionArgumentDocumentationSymbol;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

// consulo: upstream PsiSymbolReference to a DocumentationSymbol -> classic reference resolving to a BazelDocumentationElement
public class BazelGlobalFunctionArgumentReference extends PsiReferenceBase<StarlarkNamedArgumentExpression> {
    private final TextRange myTextRange;

    @RequiredReadAction
    public BazelGlobalFunctionArgumentReference(StarlarkNamedArgumentExpression element) {
        this(element, Objects.requireNonNull(element.getNameNode()).getTextRange().shiftLeft(element.getTextRange().getStartOffset()));
    }

    private BazelGlobalFunctionArgumentReference(StarlarkNamedArgumentExpression element, TextRange textRange) {
        super(element, textRange, true);
        myTextRange = textRange;
    }

    public TextRange getTextRange() {
        return myTextRange;
    }

    @Override
    public StarlarkNamedArgumentExpression getElement() {
        return myElement;
    }

    @Override
    public TextRange getRangeInElement() {
        return myTextRange;
    }

    // consulo: upstream resolveReference(): Collection<Symbol>
    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        PsiElement argumentList = myElement.getParent();
        if (argumentList == null) {
            return null;
        }
        if (!(argumentList.getParent() instanceof StarlarkCallExpression callExpression)) {
            return null;
        }
        String argumentName = myElement.getName();
        if (argumentName == null) {
            return null;
        }
        String functionName = callExpression.getCalledFunctionName();
        if (functionName == null) {
            return null;
        }
        BazelGlobalFunction function = BazelGlobalFunctions.getFunctionByName(functionName, myElement.getProject());
        if (function == null) {
            return null;
        }
        BazelGlobalFunctionParameter arg = null;
        for (BazelGlobalFunctionParameter it : function.params()) {
            if (Objects.equals(it.name(), argumentName)) {
                arg = it;
                break;
            }
        }
        if (arg == null) {
            return null;
        }
        return BazelDocumentationElement.of(myElement, new BazelGlobalFunctionArgumentDocumentationSymbol(arg, myElement.getProject()));
    }
}
