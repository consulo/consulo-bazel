package org.jetbrains.bazel.languages.bazelrc.annotation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.documentation.BazelDocumentationElement;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes;
import org.jetbrains.bazel.languages.bazelrc.flags.Flag;
import org.jetbrains.bazel.languages.bazelrc.flags.OptionEffectTag;
import org.jetbrains.bazel.languages.bazelrc.flags.OptionMetadataTag;
import org.jetbrains.bazel.languages.bazelrc.highlighting.BazelrcHighlightingColors;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine;
import org.jetbrains.bazel.languages.bazelrc.quickfix.DeleteFlagUseFix;
import org.jetbrains.bazel.languages.bazelrc.quickfix.RenameFlagNameFix;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import static consulo.language.pattern.PlatformPatterns.psiElement;

// consulo: Consulo registers annotators through AnnotatorFactory; the stateless annotator is its own factory
@ExtensionImpl
class BazelrcFlagAnnotator implements Annotator, AnnotatorFactory {
    // consulo: upstream top-level declarations of BazelrcFlagAnnotator.kt are static members of this class
    @SuppressWarnings("unchecked")
    public static final PsiElementPattern.Capture<PsiElement> flagTokenPattern =
        psiElement(BazelrcTokenTypes.FLAG)
            .withParents(
                BazelrcFlag.class,
                BazelrcLine.class
            );

    private static final Pattern LABEL_FLAG_RE = Pattern.compile("^(--|--(?:no)?[@/].*)$");

    public BazelrcFlagAnnotator() {
    }

    // consulo: upstream field symbolReferenceService (PsiSymbolReferenceService) is dropped, see annotate()

    @Override
    @RequiredReadAction
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (!flagTokenPattern.accepts(element) || isLabel(element)) {
            return;
        }

        // consulo: PsiSymbolReferenceService.getReferences(flag).flatMap(resolveReference) -> classic getReferences()/resolve():
        // flag name references resolve to a BazelDocumentationElement whose target carries the BazelFlagSymbol
        Flag flag = null;
        if (element.getParent() instanceof BazelrcFlag bazelrcFlag) {
            PsiElement resolved =
                Arrays.stream(bazelrcFlag.getReferences())
                    .map(PsiReference::resolve)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (resolved instanceof BazelDocumentationElement documentationElement &&
                documentationElement.getTarget() instanceof BazelFlagDocumentationTarget target) {
                flag = target.getSymbolPtr().getFlag();
            }
        }

        if (flag == null) {
            holder
                .newAnnotation(HighlightSeverity.WARNING, BazelPluginLocalize.annotationBazelrcUnknownFlag(element.getText()))
                .range(element.getTextRange())
                .textAttributes(BazelrcHighlightingColors.UNKNOWN_FLAG)
                .create();
            return;
        }

        if (isHidden(flag)) {
            holder
                .newAnnotation(HighlightSeverity.INFORMATION, BazelPluginLocalize.annotationBazelrcUndocumentedFlag(element.getText()))
                .range(element.getTextRange())
                .textAttributes(BazelrcHighlightingColors.UNKNOWN_FLAG)
                .create();
        }
        else if (isNoOp(flag)) {
            holder
                .newAnnotation(HighlightSeverity.INFORMATION, BazelPluginLocalize.annotationBazelrcIdleFlag(element.getText()))
                .range(element.getTextRange())
                .textAttributes(BazelrcHighlightingColors.NOOP_FLAG)
                .withFix(new DeleteFlagUseFix(element, flag))
                .create();
        }
        else if (isDeprecated(flag, element)) {
            holder
                .newAnnotation(HighlightSeverity.WARNING, BazelPluginLocalize.annotationBazelrcDeprecatedFlag(element.getText()))
                .range(element.getTextRange())
                .textAttributes(BazelrcHighlightingColors.DEPRECATED_FLAG)
                .create();
        }
        else if (isOld(flag, element.getText())) {
            holder
                .newAnnotation(HighlightSeverity.INFORMATION, oldAnnotationMessage(flag, element.getText()))
                .range(element.getTextRange())
                .textAttributes(BazelrcHighlightingColors.DEPRECATED_FLAG)
                .withFix(new RenameFlagNameFix(element, flag))
                .create();
        }
        else if (isNotApplicable(flag, element)) {
            holder
                .newAnnotation(
                    HighlightSeverity.INFORMATION,
                    BazelPluginLocalize.annotationBazelrcFlagNotApplicable(
                        element.getText(),
                        Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, BazelrcLine.class, true)).getCommand()
                    )
                ).range(element.getTextRange())
                .textAttributes(BazelrcHighlightingColors.DEPRECATED_FLAG)
                .create();
        }
    }

    // consulo: upstream builds a plain (not bundled) String message
    private LocalizeValue oldAnnotationMessage(Flag flag, String flagText) {
        String it = flagText.startsWith("--no") ? "--no" : "--";
        return LocalizeValue.of("'" + flagText + "' is deprecated. use '" + it + flag.getOption().name() + "' instead.");
    }

    @RequiredReadAction
    private boolean isLabel(PsiElement e) {
        return LABEL_FLAG_RE.matcher(e.getText()).matches();
    }

    private boolean isHidden(Flag flag) {
        return Arrays.asList(flag.getOption().metadataTags()).contains(OptionMetadataTag.HIDDEN);
    }

    private boolean isDeprecated(Flag flag, PsiElement element) {
        if (flag.getName().equals("watchfs")) {
            BazelrcLine line = PsiTreeUtil.getParentOfType(element, BazelrcLine.class, false);
            return line != null && line.getCommand().equals("startup");
        }
        else {
            return Arrays.asList(flag.getOption().metadataTags()).contains(OptionMetadataTag.DEPRECATED);
        }
    }

    private boolean isNoOp(Flag flag) {
        return Arrays.asList(flag.getOption().effectTags()).contains(OptionEffectTag.NO_OP);
    }

    private boolean isOld(Flag flag, String name) {
        if (flag instanceof Flag.Boolean || flag instanceof Flag.TriState) {
            return name.equals("--" + flag.getOption().oldName()) || name.equals("--no" + flag.getOption().oldName());
        }
        else {
            return ("--" + flag.getOption().oldName()).equals(name);
        }
    }

    private boolean isNotApplicable(Flag flag, PsiElement element) {
        BazelrcLine line = PsiTreeUtil.getParentOfType(element, BazelrcLine.class, true);
        if (line == null) {
            return false;
        }
        String it = line.getCommand();
        return !it.equals("common") && !Arrays.asList(flag.getOption().commands()).contains(it);
    }

    @Override
    public Language getLanguage() {
        return BazelrcLanguage.INSTANCE;
    }

    @Override
    public @Nullable Annotator createAnnotator() {
        return this;
    }
}
