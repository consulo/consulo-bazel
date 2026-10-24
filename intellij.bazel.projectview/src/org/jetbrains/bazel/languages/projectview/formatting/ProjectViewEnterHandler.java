package org.jetbrains.bazel.languages.projectview.formatting;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.SplitLineAction;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.action.EnterHandlerDelegate;
import consulo.language.editor.action.EnterHandlerDelegateAdapter;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ExtensionImpl
public class ProjectViewEnterHandler extends EnterHandlerDelegateAdapter {
    public static final int INDENT = 2;

    public boolean isApplicable(PsiFile file, DataContext dataContext) {
        if (!(file instanceof ProjectViewPsiFile)) {
            return false;
        }
        Boolean isSplitLine = DataManager.getInstance().loadFromDataContext(dataContext, SplitLineAction.SPLIT_LINE_KEY);
        return isSplitLine == null;
    }

    public boolean insertIndent(PsiFile file, int offset) {
        if (offset == 0) {
            return false;
        }

        PsiElement element = file.findElementAt(offset - 1);
        while (element != null && element instanceof PsiWhiteSpace) {
            element = element.getPrevSibling();
        }
        if (element == null || element.getNode().getElementType() != ProjectViewTokenType.COLON) {
            return false;
        }
        ASTNode prev = element.getNode().getTreePrev();
        return prev != null && prev.getElementType() == ProjectViewTokenType.SECTION_KEYWORD;
    }

    public boolean isBlankLine(int caretOffset, Document document) {
        int lineNumber = document.getLineNumber(caretOffset);

        int lineStart = document.getLineStartOffset(lineNumber);
        int lineEnd = document.getLineEndOffset(lineNumber);

        String textInLine = document.getText(new TextRange(lineStart, lineEnd)).trim();

        return textInLine.isEmpty();
    }

    @Override
    public EnterHandlerDelegate.Result preprocessEnter(
        PsiFile originalFile,
        Editor originalEditor,
        SimpleReference<Integer> caretOffset,
        SimpleReference<Integer> caretAdvance,
        DataContext dataContext,
        @Nullable EditorActionHandler originalHandler
    ) {
        int offset = Objects.requireNonNull(caretOffset.get());
        PsiFile file = originalFile; // now it's mutable
        Editor editor = originalEditor;

        if (editor instanceof EditorWindow) {
            file = Objects.requireNonNull(InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file));
            editor = EditorWindow.getTopLevelEditor(editor);
            offset = editor.getCaretModel().getOffset();
        }

        if (!isApplicable(file, dataContext)) {
            return EnterHandlerDelegate.Result.Continue;
        }
        Document document = editor.getDocument();
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
        editor.getCaretModel().moveToOffset(offset);

        // remove indent if enter is pressed on the blank line
        if (isBlankLine(offset, document)) {
            String lineSeparator = FileDocumentManager.getInstance().getLineSeparator(file.getVirtualFile(), file.getProject());
            editor.getDocument().insertString(offset, lineSeparator);
            editor.getCaretModel().moveToOffset(offset + lineSeparator.length());
            return EnterHandlerDelegate.Result.Stop;
        }
        if (!insertIndent(file, offset)) {
            return EnterHandlerDelegate.Result.Continue;
        }

        if (originalHandler != null) {
            originalHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), dataContext);
        }
        LogicalPosition position = editor.getCaretModel().getLogicalPosition();
        if (position.column < INDENT) {
            String spacesPadding = StringUtil.repeat(" ", INDENT - position.column);
            document.insertString(editor.getCaretModel().getOffset(), spacesPadding);
        }
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(position.line, INDENT));
        return EnterHandlerDelegate.Result.Stop;
    }
}
