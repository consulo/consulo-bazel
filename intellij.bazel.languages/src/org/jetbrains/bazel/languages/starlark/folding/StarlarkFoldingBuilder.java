package org.jetbrains.bazel.languages.starlark.folding;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.CustomFoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Folds Starlark expressions.
 * <p>
 * The base class adds the custom folding regions, for example a {@code #region} and
 * {@code #endregion} comment pair.
 */
@ExtensionImpl
public class StarlarkFoldingBuilder extends CustomFoldingBuilder {
    @Override
    @RequiredReadAction
    protected void buildLanguageFoldRegions(
        List<FoldingDescriptor> descriptors,
        PsiElement root,
        Document document,
        boolean quick
    ) {
        List<PsiElement> blockElements = new ArrayList<>();
        blockElements.addAll(PsiTreeUtil.findChildrenOfType(root, StarlarkParenthesizedExpression.class));
        blockElements.addAll(PsiTreeUtil.findChildrenOfType(root, StarlarkListLiteralExpression.class));
        blockElements.addAll(PsiTreeUtil.findChildrenOfType(root, StarlarkCallExpression.class));

        for (PsiElement block : blockElements) {
            int startOffset = block.getTextRange().getStartOffset();
            int endOffset = block.getTextRange().getEndOffset();
            if (startOffset < endOffset) {
                descriptors.add(new FoldingDescriptor(block.getNode(), new TextRange(startOffset, endOffset)));
            }
        }
    }

    @Override
    protected String getLanguagePlaceholderText(ASTNode node, TextRange range) {
        PsiElement psiElement = node.getPsi();
        if (psiElement instanceof StarlarkParenthesizedExpression) {
            return "(...)";
        }
        else if (psiElement instanceof StarlarkListLiteralExpression) {
            return "[...]";
        }
        else if (psiElement instanceof StarlarkCallExpression callExpression) {
            return getCallExpressionPlaceholder(callExpression);
        }
        else {
            return "{...}";
        }
    }

    private String getCallExpressionPlaceholder(StarlarkCallExpression callExpression) {
        String calledFunctionName = callExpression.getCalledFunctionName();
        String functionName = calledFunctionName != null ? calledFunctionName : "unknown_rule";
        String nameAttributeValue = callExpression.getNameAttributeValue();
        String targetName = nameAttributeValue != null ? nameAttributeValue : "";
        return functionName + "(" + targetName + ")";
    }

    @Override
    protected boolean isRegionCollapsedByDefault(ASTNode node) {
        return false;
    }

    /**
     * Only a {@code #} comment can hold a custom folding marker.
     */
    @Override
    protected boolean isCustomFoldingCandidate(ASTNode node) {
        return node.getElementType() == StarlarkTokenTypes.COMMENT;
    }

    /**
     * Limits a custom folding region to one file or to one statement list.
     * <p>
     * A start marker and an end marker in two different function bodies do not make a region.
     */
    @Override
    protected boolean isCustomFoldingRoot(ASTNode node) {
        return node.getPsi() instanceof StarlarkFile || node.getElementType() == StarlarkElementTypes.STATEMENT_LIST;
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
