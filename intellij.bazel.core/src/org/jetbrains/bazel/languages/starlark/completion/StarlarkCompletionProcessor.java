package org.jetbrains.bazel.languages.starlark.completion;

import consulo.application.util.function.Processor;
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLoadLookupElement;
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement;
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkNamedLookupElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote;

import java.util.Map;

public class StarlarkCompletionProcessor implements Processor<StarlarkElement> {
    private final Map<String, StarlarkLookupElement> myResults;
    private final StarlarkElement myInputElement;
    private final StarlarkQuote myInputWrapping;

    public StarlarkCompletionProcessor(
        Map<String, StarlarkLookupElement> results,
        StarlarkElement inputElement,
        StarlarkQuote inputWrapping
    ) {
        myResults = results;
        myInputElement = inputElement;
        myInputWrapping = inputWrapping;
    }

    // consulo: overload for the upstream default argument (inputWrapping = StarlarkQuote.UNQUOTED)
    public StarlarkCompletionProcessor(
        Map<String, StarlarkLookupElement> results,
        StarlarkElement inputElement
    ) {
        this(results, inputElement, StarlarkQuote.UNQUOTED);
    }

    public Map<String, StarlarkLookupElement> getResults() {
        return myResults;
    }

    @Override
    public boolean process(StarlarkElement currentElement) {
        if (currentElement.equals(myInputElement)) {
            return true;
        }
        processElement(currentElement);
        return true;
    }

    private void processElement(StarlarkElement currentElement) {
        if (currentElement instanceof StarlarkNamedElement namedElement) {
            processNamedElement(namedElement);
        }
        else if (currentElement instanceof StarlarkStringLoadValue stringLoadValue) {
            processLoadValue(stringLoadValue);
        }
    }

    private void processNamedElement(StarlarkNamedElement namedElement) {
        String name = namedElement.getName();
        if (name != null) {
            myResults.put(name, new StarlarkNamedLookupElement(namedElement, myInputWrapping));
        }
    }

    private void processLoadValue(StarlarkStringLoadValue stringLoadValue) {
        String content = stringLoadValue.getLoadValueExpressionContent();
        if (content != null) {
            myResults.putIfAbsent(content, new StarlarkLoadLookupElement(stringLoadValue));
        }
    }
}
