package org.jetbrains.bazel.languages.starlark.references;

import consulo.application.util.function.Processor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class StarlarkResolveProcessor implements Processor<StarlarkElement> {
    private final List<StarlarkElement> myResult;
    private final StarlarkElement myReferenceElement;
    private final @Nullable String myNameToResolve;

    public StarlarkResolveProcessor(
        List<StarlarkElement> result,
        StarlarkElement referenceElement,
        @Nullable String nameToResolve
    ) {
        myResult = result;
        myReferenceElement = referenceElement;
        myNameToResolve = nameToResolve;
    }

    public List<StarlarkElement> getResult() {
        return myResult;
    }

    @Override
    public boolean process(StarlarkElement currentElement) {
        if (currentElement.equals(myReferenceElement)) {
            return true;
        }
        else if (isTargetElement(currentElement)) {
            return !myResult.add(currentElement);
        }
        else if (isLoadTargetElement(currentElement)) {
            return addLoadTarget((StarlarkStringLoadValue) currentElement);
        }
        else {
            return true;
        }
    }

    private boolean isTargetElement(StarlarkElement receiver) {
        return receiver instanceof StarlarkNamedElement && Objects.equals(myNameToResolve, receiver.getName());
    }

    private boolean isLoadTargetElement(StarlarkElement receiver) {
        return receiver instanceof StarlarkStringLoadValue loadValue
            && Objects.equals(myNameToResolve, loadValue.getLoadValueExpressionContent());
    }

    private boolean addLoadTarget(StarlarkStringLoadValue element) {
        StarlarkStringLiteralExpression loadValueExpression = element.getLoadValueExpression();
        PsiReference reference = loadValueExpression == null ? null : loadValueExpression.getReference();
        PsiElement resolved = reference == null ? null : reference.resolve();
        if (!(resolved instanceof StarlarkNamedElement namedElement)) {
            return true;
        }
        return !myResult.add(namedElement);
    }
}
