package org.jetbrains.bazel.languages.starlark.annotation;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jspecify.annotations.Nullable;

// consulo: Consulo registers annotators through AnnotatorFactory; the stateless annotator is its own factory
@ExtensionImpl
public class StarlarkStringAnnotator implements Annotator, AnnotatorFactory {
    private static final String TRIPLE_QUOTE = "\"\"\"";
    private static final String TRIPLE_APOSTROPHE = "\'\'\'";

    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (element instanceof StarlarkStringLiteralExpression stringLiteralExpression) {
            annotateUnterminated(stringLiteralExpression.getTextWithQuotes(), element, holder);
        }
    }

    private void annotateUnterminated(String receiver, PsiElement element, AnnotationHolder holder) {
        if (isTripleQuoteUnterminated(receiver)) {
            annotate(
                holder,
                element,
                StarlarkLocalize.annotatorMissingClosingTripleQuote()
            );
        }
        else if (isSingleQuoteUnterminated(receiver)) {
            annotate(
                holder,
                element,
                StarlarkLocalize.annotatorMissingClosingQuote(receiver.charAt(0))
            );
        }
    }

    private boolean isTripleQuoted(String receiver) {
        return receiver.startsWith(TRIPLE_QUOTE) || receiver.startsWith(TRIPLE_APOSTROPHE);
    }

    private boolean isSingleQuoteUnterminated(String receiver) {
        return receiver.length() == 1 ||
            receiver.charAt(0) != receiver.charAt(receiver.length() - 1) ||
            hasOddNumberOfPrecedingBackslashes(receiver);
    }

    private boolean isTripleQuoteUnterminated(String receiver) {
        return isTripleQuoted(receiver) &&
            (receiver.length() < 6 || !receiver.substring(0, 3).equals(receiver.substring(receiver.length() - 3)));
    }

    private boolean hasOddNumberOfPrecedingBackslashes(String receiver) {
        // consulo: Kotlin dropLast(1).takeLastWhile { it == '\\' }.length
        int end = Math.max(receiver.length() - 1, 0);
        int count = 0;
        for (int i = end - 1; i >= 0 && receiver.charAt(i) == '\\'; i--) {
            count++;
        }
        return count % 2 == 1;
    }

    private void annotate(AnnotationHolder receiver, PsiElement element, LocalizeValue message) {
        receiver.newAnnotation(HighlightSeverity.ERROR, message).range(element).create();
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public @Nullable Annotator createAnnotator() {
        return this;
    }
}
