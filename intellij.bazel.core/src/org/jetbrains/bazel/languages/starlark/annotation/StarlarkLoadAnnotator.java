package org.jetbrains.bazel.languages.starlark.annotation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.RawFileLoaderHelper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.webBrowser.BrowserUtil;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue;
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference;
import org.jetbrains.bazel.languages.starlark.references.StarlarkLoadReference;
import org.jspecify.annotations.Nullable;

// consulo: Consulo registers annotators through AnnotatorFactory; the stateless annotator is its own factory
@ExtensionImpl
public class StarlarkLoadAnnotator implements Annotator, AnnotatorFactory, DumbAware {
    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (isLoadedSymbolLiteral(element) && isNotResolvable(element)) {
            AnnotatorUtils.annotateError(
                holder,
                element,
                StarlarkLocalize.annotatorUnresolvedReference(element.getText())
            );
        }
        else if (isLoadFilenameLiteral(element) && isTooLargeLoadedFile(element)) {
            AnnotatorUtils.annotateInfoWithFix(
                holder,
                element,
                StarlarkLocalize.annotatorTooLargeFile(),
                new OpenCodeInsightFileSizeHelpAction()
            );
        }
    }

    private boolean isNotResolvable(PsiElement element) {
        PsiReference reference = element.getReference();
        if (reference == null) {
            return true;
        }
        if (!(reference instanceof StarlarkLoadReference loadReference)) {
            return false;
        }
        return loadReference.getLoadedFileReference().resolve() instanceof StarlarkFile && !reference.isSoft() && reference.resolve() == null;
    }

    private boolean isLoadedSymbolLiteral(PsiElement receiver) {
        PsiElement parent = receiver.getParent();
        return PsiUtilCore.getElementType(receiver) == StarlarkElementTypes.STRING_LITERAL_EXPRESSION &&
            (PsiUtilCore.getElementType(parent) == StarlarkElementTypes.STRING_LOAD_VALUE ||
                PsiUtilCore.getElementType(parent) == StarlarkElementTypes.NAMED_LOAD_VALUE);
    }

    private boolean isTooLargeLoadedFile(PsiElement element) {
        if (!(element.getReference() instanceof BazelLabelReference reference)) {
            return false;
        }
        if (!(reference.resolve() instanceof PsiFile loadedFile)) {
            return false;
        }
        VirtualFile virtualFile = loadedFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        // consulo: SingleRootFileViewProvider.isTooLargeForIntelligence -> RawFileLoaderHelper (non-deprecated API)
        return !(loadedFile instanceof StarlarkFile) && RawFileLoaderHelper.isTooLargeForIntelligence(virtualFile);
    }

    private boolean isLoadFilenameLiteral(PsiElement receiver) {
        return PsiUtilCore.getElementType(receiver) == StarlarkElementTypes.STRING_LITERAL_EXPRESSION &&
            receiver.getParent() instanceof StarlarkFilenameLoadValue;
    }

    private static class OpenCodeInsightFileSizeHelpAction implements IntentionAction {
        @Override
        public LocalizeValue getText() {
            return StarlarkLocalize.quickfixOpenCodeInsightFileSizeHelp();
        }

        // consulo: Consulo intention actions have no family name; kept from upstream
        public LocalizeValue getFamilyName() {
            return getText();
        }

        @Override
        public boolean isAvailable(Project project, Editor editor, PsiFile file) {
            return true;
        }

        @Override
        public void invoke(Project project, Editor editor, PsiFile file) {
            BrowserUtil.browse("https://www.jetbrains.com/help/idea/tuning-the-ide.html#-v6psi5_124");
        }

        @Override
        public boolean startInWriteAction() {
            return false;
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
