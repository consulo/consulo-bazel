package org.jetbrains.bazel.languages.starlark.annotation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.SemVer;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral;
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.quickFixes.StarlarkGlobAllowEmptyQuickFix;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

import static org.jetbrains.bazel.languages.starlark.StarlarkConstants.ALLOW_EMPTY_KEYWORD;

// consulo: Consulo registers annotators through AnnotatorFactory; the stateless annotator is its own factory
@ExtensionImpl
public class StarlarkGlobAnnotator implements Annotator, AnnotatorFactory, DumbAware {
    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (element instanceof StarlarkGlobExpression globExpression && !isGlobValid(globExpression)) {
            holder
                .newAnnotation(HighlightSeverity.ERROR, StarlarkLocalize.annotatorGlobEmpty())
                .range(element)
                .withFix(new StarlarkGlobAllowEmptyQuickFix(globExpression))
                .create();
        }
        else if (element instanceof StarlarkStringLiteralExpression stringLiteralExpression) {
            StarlarkGlobExpression globExpression = findContainingGlobExpression(stringLiteralExpression);
            if (globExpression == null) {
                return;
            }
            if (isPatternValid(globExpression, stringLiteralExpression)) {
                return;
            }
            holder
                .newAnnotation(HighlightSeverity.WARNING, StarlarkLocalize.annotatorGlobEmptyPattern())
                .range(element)
                .withFix(new StarlarkGlobAllowEmptyQuickFix(globExpression))
                .create();
        }
    }

    private @Nullable StarlarkGlobExpression findContainingGlobExpression(StarlarkStringLiteralExpression element) {
        StarlarkCallExpression callExpr = PsiTreeUtil.getParentOfType(element, StarlarkCallExpression.class, true);
        if (callExpr == null) {
            return null;
        }
        return callExpr.getFirstChild() instanceof StarlarkGlobExpression globExpression ? globExpression : null;
    }

    /**
     * The pattern is valid if matches at least one file
     * or the 'allow_empty' argument is set to true.
     */
    private boolean isPatternValid(StarlarkGlobExpression globExpression, StarlarkStringLiteralExpression element) {
        if (isAllowedEmpty(globExpression)) {
            return true;
        }
        String pattern = element.getStringContents();
        PsiDirectory parent = Objects.requireNonNull(globExpression.getContainingFile()).getParent();
        VirtualFile containingDirectory = parent == null ? null : parent.getVirtualFile();
        if (containingDirectory == null) {
            return true;
        }
        try {
            return !StarlarkGlob
                .forPath(containingDirectory)
                .addPattern(pattern)
                .build()
                .execute()
                .isEmpty();
        }
        catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * The glob is valid if it resolves to a non-empty set of files,
     * or the 'allow_empty' argument is set to true.
     */
    private boolean isGlobValid(StarlarkGlobExpression expr) {
        if (isAllowedEmpty(expr)) {
            return true;
        }

        if (!(expr.getReference() instanceof PsiPolyVariantReference polyVariantReference)) {
            return true;
        }
        ResolveResult[] matchedFiles = polyVariantReference.multiResolve(false);
        return matchedFiles.length != 0;
    }

    private boolean isAllowedEmpty(StarlarkGlobExpression expr) {
        StarlarkNamedArgumentExpression keywordArgument = expr.getKeywordArgument(ALLOW_EMPTY_KEYWORD);
        PsiElement value = keywordArgument == null ? null : keywordArgument.getValue();
        String allowEmpty = value == null ? null : value.getText();
        Boolean allowEmptyValue = allowEmpty == null ? null : toBooleanStrictOrNull(allowEmpty.toLowerCase(Locale.ROOT));
        if (allowEmptyValue != null) {
            return allowEmptyValue;
        }
        BazelVersionLiteral currentBazelVersion = expr.getProject().getInstance(BazelVersionCheckerService.class).getCurrentBazelVersion();
        SemVer semVer = currentBazelVersion == null ? null : BazelVersionLiteral.toSemVer(currentBazelVersion);
        boolean defaultAllowEmptyTrue = semVer != null && semVer.getMajor() < 8;
        return defaultAllowEmptyTrue;
    }

    // consulo: Kotlin String.toBooleanStrictOrNull()
    private static @Nullable Boolean toBooleanStrictOrNull(String value) {
        if (value.equals("true")) {
            return Boolean.TRUE;
        }
        else if (value.equals("false")) {
            return Boolean.FALSE;
        }
        else {
            return null;
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
