package org.jetbrains.bazel.languages.starlark.completion.lookups;

import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class StarlarkNamedLookupElement extends StarlarkLookupElement {
    private final StarlarkNamedElement myElement;

    public StarlarkNamedLookupElement(StarlarkNamedElement element, StarlarkQuote wrapping) {
        super(Objects.requireNonNullElse(element.getName(), ""), wrapping);
        myElement = element;
    }

    // consulo: overload for the upstream default argument (wrapping = StarlarkQuote.UNQUOTED)
    public StarlarkNamedLookupElement(StarlarkNamedElement element) {
        this(element, StarlarkQuote.UNQUOTED);
    }

    public StarlarkNamedElement getElement() {
        return myElement;
    }

    @Override
    public @Nullable Image getIcon() {
        // consulo: upstream element.getIcon(0), Consulo PsiElement icons come from IconDescriptorUpdaters
        return IconDescriptorUpdaters.getIcon(myElement, 0);
    }
}
