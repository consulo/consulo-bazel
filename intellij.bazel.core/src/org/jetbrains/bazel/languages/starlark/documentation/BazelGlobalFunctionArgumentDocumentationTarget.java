package org.jetbrains.bazel.languages.starlark.documentation;

import consulo.bazel.documentation.DocumentationResult;
import consulo.bazel.documentation.DocumentationTarget;
import org.jspecify.annotations.Nullable;

public class BazelGlobalFunctionArgumentDocumentationTarget implements DocumentationTarget {
    // consulo: upstream symbol.createPointer(), the symbol is its own pointer
    private final BazelGlobalFunctionArgumentDocumentationSymbol mySymbolPtr;

    public BazelGlobalFunctionArgumentDocumentationTarget(BazelGlobalFunctionArgumentDocumentationSymbol symbol) {
        mySymbolPtr = symbol;
    }

    public BazelGlobalFunctionArgumentDocumentationSymbol getSymbolPtr() {
        return mySymbolPtr;
    }

    // consulo: createPointer(), dereference() and computePresentation() are dropped,
    // pointer/presentation are not used by the classic documentation provider

    @Override
    public @Nullable DocumentationResult computeDocumentation() {
        String html = mySymbolPtr.getArgument().doc();
        if (html == null) {
            return null;
        }
        return DocumentationResult.documentation(html);
    }
}
