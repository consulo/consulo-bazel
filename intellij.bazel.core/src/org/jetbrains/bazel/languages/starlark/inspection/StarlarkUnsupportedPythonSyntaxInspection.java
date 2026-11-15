package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiWhiteSpace;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.StarlarkUtils;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDoubleStarExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGeneratorExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStarExpression;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class StarlarkUnsupportedPythonSyntaxInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new UnsupportedSyntaxVisitor(holder);
    }

    private static class UnsupportedSyntaxVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        UnsupportedSyntaxVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element.getFirstChild() == null && UNSUPPORTED_KEYWORDS.contains(PsiUtilCore.getElementType(element))) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionUnsupportedKeyword(element.getText()))
                    .range(element)
                    .create();
            }
        }

        @Override
        public void visitGeneratorExpression(StarlarkGeneratorExpression node) {
            myHolder.newProblem(StarlarkLocalize.inspectionDescriptionUnsupportedGeneratorExpression())
                .range(node)
                .create();
        }

        @Override
        public void visitStarExpression(StarlarkStarExpression node) {
            myHolder.newProblem(StarlarkLocalize.inspectionDescriptionUnsupportedUnpackingExpression())
                .range(node)
                .create();
        }

        @Override
        public void visitDoubleStarExpression(StarlarkDoubleStarExpression node) {
            myHolder.newProblem(StarlarkLocalize.inspectionDescriptionUnsupportedUnpackingExpression())
                .range(node)
                .create();
        }

        @Override
        public void visitBinaryExpression(StarlarkBinaryExpression node) {
            PsiElement operator = node.getOperatorElement();
            if (operator == null) {
                return;
            }
            if (PsiUtilCore.getElementType(operator) == StarlarkTokenTypes.EXP) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionUnsupportedBinaryOperator(operator.getText()))
                    .range(operator)
                    .create();
            }
            else if (COMPARISON_OPERATORS.contains(PsiUtilCore.getElementType(operator)) && isPartOfChainedComparison(node)) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionUnsupportedChainedComparison())
                    .range(node, operatorProblemRange(node, operator))
                    .create();
            }
        }

        private boolean isPartOfChainedComparison(StarlarkBinaryExpression receiver) {
            return isComparisonExpression(receiver.getLeftOperand()) || isComparisonExpression(receiver.getRightOperand());
        }

        private boolean isComparisonExpression(@Nullable PsiElement receiver) {
            if (!(receiver instanceof StarlarkBinaryExpression binaryExpression)) {
                return false;
            }
            IElementType operator = binaryExpression.getOperator();
            return operator != null && COMPARISON_OPERATORS.contains(operator);
        }

        private TextRange operatorProblemRange(StarlarkBinaryExpression receiver, PsiElement operator) {
            PsiElement endElement;
            if (PsiUtilCore.getElementType(operator) == StarlarkTokenTypes.NOT_KEYWORD) {
                PsiElement next = nextRelevantSibling(operator);
                endElement = next != null && PsiUtilCore.getElementType(next) == StarlarkTokenTypes.IN_KEYWORD ? next : operator;
            }
            else {
                endElement = operator;
            }
            return new TextRange(
                operator.getTextRange().getStartOffset() - receiver.getTextRange().getStartOffset(),
                endElement.getTextRange().getEndOffset() - receiver.getTextRange().getStartOffset()
            );
        }

        private @Nullable PsiElement nextRelevantSibling(PsiElement receiver) {
            PsiElement current = receiver.getNextSibling();
            while (current != null) {
                if (isRelevant(current)) {
                    return current;
                }
                current = current.getNextSibling();
            }
            return null;
        }

        private boolean isRelevant(PsiElement receiver) {
            IElementType elementType = PsiUtilCore.getElementType(receiver);
            return !(receiver instanceof PsiErrorElement) && !(receiver instanceof PsiWhiteSpace) && elementType != null &&
                !StarlarkUtils.STARLARK_WHITESPACES_AND_COMMENTS.contains(elementType);
        }
    }

    private static final TokenSet UNSUPPORTED_KEYWORDS = TokenSet.create(
        StarlarkTokenTypes.AS_KEYWORD,
        StarlarkTokenTypes.ASSERT_KEYWORD,
        StarlarkTokenTypes.ASYNC_KEYWORD,
        StarlarkTokenTypes.AWAIT_KEYWORD,
        StarlarkTokenTypes.CLASS_KEYWORD,
        StarlarkTokenTypes.DEL_KEYWORD,
        StarlarkTokenTypes.EXCEPT_KEYWORD,
        StarlarkTokenTypes.FINALLY_KEYWORD,
        StarlarkTokenTypes.FROM_KEYWORD,
        StarlarkTokenTypes.GLOBAL_KEYWORD,
        StarlarkTokenTypes.IMPORT_KEYWORD,
        StarlarkTokenTypes.IS_KEYWORD,
        StarlarkTokenTypes.NONLOCAL_KEYWORD,
        StarlarkTokenTypes.RAISE_KEYWORD,
        StarlarkTokenTypes.TRY_KEYWORD,
        StarlarkTokenTypes.WHILE_KEYWORD,
        StarlarkTokenTypes.WITH_KEYWORD,
        StarlarkTokenTypes.YIELD_KEYWORD
    );

    private static final TokenSet COMPARISON_OPERATORS = TokenSet.create(
        StarlarkTokenTypes.LT,
        StarlarkTokenTypes.LE,
        StarlarkTokenTypes.GT,
        StarlarkTokenTypes.GE,
        StarlarkTokenTypes.EQEQ,
        StarlarkTokenTypes.NE,
        StarlarkTokenTypes.IN_KEYWORD,
        StarlarkTokenTypes.NOT_KEYWORD
    );

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkUnsupportedPythonSyntax";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkUnsupportedSyntaxCheck();
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return BazelPluginLocalize.starlarkInspectionGroupName();
    }

    // consulo: upstream groupPath="Bazel"
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{BazelPluginLocalize.bazelInspectionGroupName(), getGroupDisplayName()};
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
