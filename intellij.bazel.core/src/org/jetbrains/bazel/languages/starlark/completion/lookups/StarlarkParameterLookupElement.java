package org.jetbrains.bazel.languages.starlark.completion.lookups;

import consulo.language.editor.completion.lookup.LookupElementPresentation;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote;

public class StarlarkParameterLookupElement extends StarlarkNamedLookupElement {
    public StarlarkParameterLookupElement(StarlarkParameter element) {
        super(element, StarlarkQuote.UNQUOTED);
    }

    @Override
    public String getLookupString() {
        return getName() + " = ";
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getName() + " =");
        presentation.setIcon(getIcon());
    }
}
