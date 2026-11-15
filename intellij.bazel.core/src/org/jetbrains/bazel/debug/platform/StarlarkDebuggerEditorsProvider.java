package org.jetbrains.bazel.debug.platform;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.execution.debug.evaluation.XDebuggerEditorsProviderBase;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList;
import org.jspecify.annotations.Nullable;

public class StarlarkDebuggerEditorsProvider extends XDebuggerEditorsProviderBase {
    @Override
    public FileType getFileType() {
        return StarlarkFileType.INSTANCE;
    }

    @Override
    protected PsiFile createExpressionCodeFragment(
        Project project,
        String text,
        @Nullable PsiElement context,
        boolean isPhysical
    ) {
        String content = text.trim();
        return new StarlarkExpressionCodeFragment(project, "fragment", content, context);
    }

    @RequiredReadAction
    @Override
    protected @Nullable PsiElement getContextElement(
        VirtualFile file,
        int offset,
        Project project
    ) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return null;
        }
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        return psiFile != null ? findInterestingElement(psiFile, document, offset) : null;
    }

    @RequiredReadAction
    private static @Nullable PsiElement findInterestingElement(PsiFile receiver, Document document, int initialOffset) {
        int cursor = initialOffset;
        if (0 <= cursor && cursor < document.getTextLength()) {
            int lineEndOffset = document.getLineEndOffset(document.getLineNumber(cursor));
            do {
                PsiElement element = receiver.findElementAt(cursor);
                if (element == null) {
                    break;
                }
                if (!(element instanceof PsiWhiteSpace) && !(element instanceof PsiComment)) {
                    return getStatement(element);
                }
                cursor = element.getTextRange().getEndOffset();
            }
            while (cursor < lineEndOffset);
        }
        return null;
    }

    private static @Nullable PsiElement getStatement(PsiElement receiver) {
        StarlarkElement topLevelStatement = getTopLevelStatement(receiver);
        return topLevelStatement != null ? getAncestorRightBelow(receiver, topLevelStatement) : null;
    }

    private static @Nullable StarlarkElement getTopLevelStatement(PsiElement receiver) {
        if (receiver instanceof StarlarkFile || receiver instanceof StarlarkStatementList) {
            return (StarlarkElement) receiver;
        }
        else {
            // consulo: PsiTreeUtil.getParentOfType(element, false, classes...) -> getNonStrictParentOfType(element, classes...)
            return PsiTreeUtil.<StarlarkElement>getNonStrictParentOfType(receiver, StarlarkFile.class, StarlarkStatementList.class);
        }
    }

    /**
     * Returns element's ancestor, whose direct parent is given superParent (or `null` if nonexistent)
     */
    private static @Nullable PsiElement getAncestorRightBelow(PsiElement receiver, PsiElement superParent) {
        return PsiTreeUtil.findFirstParent(receiver, true, it -> it.getParent() == superParent);
    }
}
