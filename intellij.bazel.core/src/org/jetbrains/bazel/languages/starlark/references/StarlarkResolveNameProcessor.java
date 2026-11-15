package org.jetbrains.bazel.languages.starlark.references;

import consulo.application.util.function.Processor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;

import java.util.List;
import java.util.Objects;

public class StarlarkResolveNameProcessor implements Processor<StarlarkElement> {
    private final List<StarlarkElement> myResult;
    private final String myName;

    public StarlarkResolveNameProcessor(List<StarlarkElement> result, String name) {
        myResult = result;
        myName = name;
    }

    public List<StarlarkElement> getResult() {
        return myResult;
    }

    @Override
    public boolean process(StarlarkElement currentElement) {
        if (Objects.equals(currentElement.getName(), myName)) {
            return !myResult.add(currentElement);
        }
        else {
            return true;
        }
    }
}
