package org.jetbrains.bazel.languages.starlark.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.action.ExtendWordSelectionHandlerBase;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ExtensionImpl
public class StarlarkStringLiteralSelectioner extends ExtendWordSelectionHandlerBase {
    @Override
    @RequiredReadAction
    public boolean canSelect(PsiElement element) {
        return element instanceof StarlarkStringLiteralExpression || element.getParent() instanceof StarlarkStringLiteralExpression;
    }

    @Override
    @RequiredReadAction
    public @Nullable List<TextRange> select(
        PsiElement element,
        CharSequence editorText,
        int cursorOffset,
        Editor editor
    ) {
        StarlarkStringLiteralExpression stringElement;
        if (element instanceof StarlarkStringLiteralExpression literal) {
            stringElement = literal;
        }
        else if (element.getParent() instanceof StarlarkStringLiteralExpression parentLiteral) {
            stringElement = parentLiteral;
        }
        else {
            return null;
        }
        return List.of(stringElement.getStringContentsOffset().shiftRight(stringElement.getTextRange().getStartOffset()));
    }
}
