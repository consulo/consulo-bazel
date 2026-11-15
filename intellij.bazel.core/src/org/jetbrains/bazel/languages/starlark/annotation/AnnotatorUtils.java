package org.jetbrains.bazel.languages.starlark.annotation;

import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;

public final class AnnotatorUtils {
    private AnnotatorUtils() {
    }

    public static void annotateError(
        AnnotationHolder receiver,
        PsiElement element,
        LocalizeValue message
    ) {
        receiver.newAnnotation(HighlightSeverity.ERROR, message)
            .range(element)
            .create();
    }

    public static void annotateSilentInfo(
        AnnotationHolder receiver,
        PsiElement element,
        TextAttributesKey attr
    ) {
        receiver.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(attr)
            .create();
    }

    public static void annotateInfoWithFix(
        AnnotationHolder receiver,
        PsiElement element,
        LocalizeValue message,
        IntentionAction fix
    ) {
        receiver.newAnnotation(HighlightSeverity.INFORMATION, message)
            .range(element)
            .withFix(fix)
            .create();
    }
}
