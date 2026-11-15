package org.jetbrains.bazel.languages.starlark.annotation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkSemanticHighlighting;
import org.jspecify.annotations.Nullable;

// consulo: Consulo registers annotators through AnnotatorFactory; the stateless annotator is its own factory
@ExtensionImpl
public class StarlarkFunctionAnnotator implements Annotator, AnnotatorFactory, DumbAware {

    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        TextAttributesKey highlighting = StarlarkSemanticHighlighting.starlarkSemanticHighlightingColor(element);
        if (highlighting != null) {
            AnnotatorUtils.annotateSilentInfo(holder, element, highlighting);
        }
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
