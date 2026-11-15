package org.jetbrains.bazel.languages.starlark.completion.lookups;

import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class StarlarkLoadLookupElement extends StarlarkLookupElement {
    private final StarlarkStringLoadValue myElement;

    public StarlarkLoadLookupElement(
        StarlarkStringLoadValue element
    ) {
        super(Objects.requireNonNullElse(element.getLoadValueExpressionContent(), ""));
        myElement = element;
    }

    public StarlarkStringLoadValue getElement() {
        return myElement;
    }

    @Override
    public @Nullable Image getIcon() {
        return myElement.getIcon(0);
    }
}
