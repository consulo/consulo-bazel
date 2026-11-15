package org.jetbrains.bazel.languages.starlark.completion;

import consulo.document.Document;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public final class StarlarkCompletionLookupElement {
    private StarlarkCompletionLookupElement() {
    }

    // consulo: overloads for the upstream default arguments
    public static LookupElement getCompletionLookupElement(String name, Image icon) {
        return getCompletionLookupElement(name, icon, 0.0);
    }

    public static LookupElement getCompletionLookupElement(String name, Image icon, double priority) {
        return getCompletionLookupElement(name, icon, priority, null, "");
    }

    public static LookupElement getCompletionLookupElement(
        String name,
        Image icon,
        double priority,
        @Nullable String presentableText,
        String tailText
    ) {
        return getCompletionLookupElement(name, icon, priority, presentableText, tailText, true);
    }

    /**
     * @param isExpressionFinished isExpressionFinished indicates whether the lookup element finishes the expression:
     *                             {@code true} if the lookup element completes the entire expression (or its final part),
     *                             {@code false} if the lookup element completes only a part of the expression (for example,
     *                             when completing the package path to a class with subsequent subpackages)
     */
    public static LookupElement getCompletionLookupElement(
        String name,
        Image icon,
        double priority,
        @Nullable String presentableText,
        String tailText,
        boolean isExpressionFinished
    ) {
        return PrioritizedLookupElement.withPriority(
            LookupElementBuilder
                .create("\"" + name + "\"")
                .withIcon(icon)
                .withPresentableText(presentableText != null ? presentableText : name)
                .withTailText(tailText, true)
                .withInsertHandler((context, item) -> {
                    // This prevents inserting a duplicate quote at the end.
                    Document document = context.getDocument();
                    int offset = context.getTailOffset();
                    if (offset < document.getTextLength() && document.getCharsSequence().charAt(offset) == '"') {
                        document.deleteString(offset, offset + 1);
                    }

                    // If the expression is not finished, continue completion inside the quotation mark.
                    if (!isExpressionFinished) {
                        context.getEditor().getCaretModel().moveToOffset(offset - 1);
                        AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
                    }
                }),
            priority
        );
    }
}
