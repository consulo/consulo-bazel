package org.jetbrains.bazel.languages.starlark.completion.lookups;

import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote;
import org.jspecify.annotations.Nullable;

public abstract class StarlarkLookupElement extends LookupElement {
    private final String myName;
    private final StarlarkQuote myWrapping;

    public StarlarkLookupElement(String name, StarlarkQuote wrapping) {
        myName = name;
        myWrapping = wrapping;
    }

    // consulo: overload for the upstream default argument (wrapping = StarlarkQuote.UNQUOTED)
    public StarlarkLookupElement(String name) {
        this(name, StarlarkQuote.UNQUOTED);
    }

    public String getName() {
        return myName;
    }

    @Override
    public String getLookupString() {
        return myWrapping.wrap(myName);
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(myName);
        presentation.setIcon(getIcon());
    }

    @Override
    public void handleInsert(InsertionContext context) {
        if (myWrapping == StarlarkQuote.UNQUOTED) {
            super.handleInsert(context);
            return;
        }

        adjustQuotation(context);
        context.commitDocument();
    }

    private void adjustQuotation(InsertionContext context) {
        PsiElement suffixElement = context.getFile().findElementAt(context.getTailOffset());
        if (suffixElement == null) {
            return;
        }
        if (suffixElement.getText().startsWith(myWrapping.getQuote())) {
            int offset = suffixElement.getTextOffset();
            context.getDocument().deleteString(offset, offset + myWrapping.getQuote().length());
        }
    }

    public abstract @Nullable Image getIcon();
}
