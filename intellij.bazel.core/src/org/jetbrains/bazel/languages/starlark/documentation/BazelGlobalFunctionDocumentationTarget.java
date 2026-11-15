package org.jetbrains.bazel.languages.starlark.documentation;

import consulo.bazel.documentation.DocumentationResult;
import consulo.bazel.documentation.DocumentationTarget;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Collectors;

public class BazelGlobalFunctionDocumentationTarget implements DocumentationTarget {
    // consulo: upstream symbol.createPointer(), the symbol is its own pointer
    private final BazelGlobalFunctionDocumentationSymbol mySymbolPtr;

    public BazelGlobalFunctionDocumentationTarget(BazelGlobalFunctionDocumentationSymbol symbol) {
        mySymbolPtr = symbol;
    }

    public BazelGlobalFunctionDocumentationSymbol getSymbolPtr() {
        return mySymbolPtr;
    }

    // consulo: createPointer(), dereference() and computePresentation() are dropped,
    // pointer/presentation are not used by the classic documentation provider

    private String computeFunctionDefinition(BazelGlobalFunction function) {
        String functionName = getStyledFragment(function.name(), StarlarkHighlightingColors.FUNCTION_DECLARATION);
        String comma = getStyledFragment(", ", StarlarkHighlightingColors.COMMA);
        String openParen = getStyledFragment("(", StarlarkHighlightingColors.PARENTHESES);
        String closeParen = getStyledFragment(")", StarlarkHighlightingColors.PARENTHESES);
        String params =
            function.params().stream().map(param -> {
                String name = getStyledFragment(param.name(), StarlarkHighlightingColors.NAMED_ARGUMENT);
                String defaultValue = param.defaultValue();
                if (defaultValue != null) {
                    String value = getStyledFragment(defaultValue, DefaultLanguageHighlighterColors.STRING);
                    return name + " = " + value;
                }
                else {
                    return name;
                }
            }).collect(Collectors.joining(comma));
        return functionName + openParen + params + closeParen;
    }

    @Override
    public @Nullable DocumentationResult computeDocumentation() {
        BazelGlobalFunction function = mySymbolPtr.getFunction();
        String functionDefinition = computeFunctionDefinition(function);
        String html = Objects.requireNonNullElse(function.doc(), "");
        return DocumentationResult.documentation("<pre>" + functionDefinition + "</pre><hr/>" + html);
    }

    // consulo: QuickDocHighlightingHelper.getStyledFragment(text, key) does not exist in Consulo,
    // the function name is bold text and the other fragments are plain text
    private static String getStyledFragment(String text, TextAttributesKey key) {
        return key == StarlarkHighlightingColors.FUNCTION_DECLARATION ? "<b>" + text + "</b>" : text;
    }
}
