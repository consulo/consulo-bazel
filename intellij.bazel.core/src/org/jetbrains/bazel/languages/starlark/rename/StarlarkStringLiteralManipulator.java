package org.jetbrains.bazel.languages.starlark.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.impl.ast.LeafElement;
import consulo.language.psi.AbstractElementManipulator;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralEscaper;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote;

/**
 * Allows the platform to change the contents of a Starlark string literal, e.g. when renaming a referenced target or
 * when a fragment of a language injected into the literal is edited. New content is escaped as needed for the kind of
 * string literal. A raw string literal stays raw as long as the new content can be represented in it; otherwise (e.g.
 * if the content contains a quote), it is turned into a regular string literal with the same value.
 */
@ExtensionImpl
public class StarlarkStringLiteralManipulator extends AbstractElementManipulator<StarlarkStringLiteralExpression> {
    @Override
    @RequiredWriteAction
    public StarlarkStringLiteralExpression handleContentChange(
        StarlarkStringLiteralExpression element,
        TextRange range,
        String newContent
    ) {
        if (!(element.getNode().getFirstChildNode() instanceof LeafElement stringLeafNode)) {
            return element;
        }
        TextRange contentRange = getRangeInElement(element);
        if (!contentRange.contains(range)) {
            return element;
        }
        String oldText = element.getText();
        StarlarkQuote quote = element.getQuote();
        String newText;
        if (!element.isRaw()) {
            newText = take(oldText, range.getStartOffset()) + StarlarkStringLiteralEscaper.escape(newContent, quote)
                + oldText.substring(range.getEndOffset());
        }
        else {
            // Raw strings have no escape sequences, so the contents are the value.
            String newRawContent =
                oldText.substring(contentRange.getStartOffset(), range.getStartOffset()) + newContent +
                    oldText.substring(range.getEndOffset(), contentRange.getEndOffset());
            if (StarlarkStringLiteralEscaper.isRepresentableAsRaw(newRawContent, quote)) {
                newText = take(oldText, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());
            }
            else {
                // consulo: Kotlin replace("r", "", ignoreCase = true)
                String prefix =
                    oldText.substring(0, contentRange.getStartOffset() - quote.getQuote().length()).replace("r", "").replace("R", "");
                newText = prefix + quote.wrap(StarlarkStringLiteralEscaper.escape(newRawContent, quote));
            }
        }
        stringLeafNode.replaceWithText(newText);
        return element;
    }

    // consulo: Kotlin String.take(n)
    private static String take(String receiver, int n) {
        return receiver.substring(0, Math.min(n, receiver.length()));
    }

    @Override
    @RequiredReadAction
    public TextRange getRangeInElement(StarlarkStringLiteralExpression element) {
        return element.getStringContentsOffset();
    }

    @Override
    public Class<StarlarkStringLiteralExpression> getElementClass() {
        return StarlarkStringLiteralExpression.class;
    }
}
