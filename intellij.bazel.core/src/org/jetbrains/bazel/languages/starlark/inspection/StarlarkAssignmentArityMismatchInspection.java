package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static org.jetbrains.bazel.languages.starlark.StarlarkUtils.relevantChildrenAroundEach;
import static org.jetbrains.bazel.languages.starlark.utils.StarlarkAssignmentUtils.destructuringElements;
import static org.jetbrains.bazel.languages.starlark.utils.StarlarkAssignmentUtils.destructuringTargets;

@ExtensionImpl
public class StarlarkAssignmentArityMismatchInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new AssignmentArityMismatchVisitor(holder);
    }

    private static class AssignmentArityMismatchVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        AssignmentArityMismatchVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitAssignmentStatement(StarlarkAssignmentStatement node) {
            PsiElement left = node.leftHandSide();
            if (left == null) {
                return;
            }
            PsiElement right = node.rightHandSide();
            if (right == null) {
                return;
            }
            checkAssignmentArity(left, right);
        }

        @Override
        public void visitForStatement(StarlarkForStatement node) {
            PsiElement left = node.leftHandSide();
            if (left == null) {
                return;
            }
            PsiElement right = node.rightHandSide();
            if (right == null) {
                return;
            }
            checkIterationTargetArity(left, right);
        }

        @Override
        public void visitListCompExpression(StarlarkListCompExpression node) {
            checkComprehensionArity(node);
        }

        @Override
        public void visitDictCompExpression(StarlarkDictCompExpression node) {
            checkComprehensionArity(node);
        }

        private void checkComprehensionArity(StarlarkCompExpression node) {
            for (Pair<PsiElement, PsiElement> pair : relevantChildrenAroundEach(node, StarlarkTokenTypes.IN_KEYWORD)) {
                checkIterationTargetArity(pair.getFirst(), pair.getSecond());
            }
        }

        private void checkIterationTargetArity(PsiElement left, PsiElement iterable) {
            List<PsiElement> iteratedValues = destructuringElements(iterable);
            if (iteratedValues == null) {
                return;
            }

            for (PsiElement iteratedValue : iteratedValues) {
                checkAssignmentArity(left, iteratedValue);
            }
        }

        private boolean checkAssignmentArity(PsiElement left, PsiElement right) {
            List<PsiElement> leftElements = destructuringTargets(left);
            if (leftElements == null) {
                return true;
            }
            List<PsiElement> rightElements = destructuringElements(right);
            if (rightElements == null) {
                return true;
            }
            int leftArity = leftElements.size();
            int rightArity = rightElements.size();

            if (leftArity != rightArity) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionAssignmentArityMismatch(leftArity, rightArity))
                    .range(left)
                    .create();
                return false;
            }

            boolean isValid = true;
            for (int index = 0; index < leftElements.size(); index++) {
                if (!checkAssignmentArity(leftElements.get(index), rightElements.get(index))) {
                    isValid = false;
                }
            }

            return isValid;
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkAssignmentArityMismatch";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkAssignmentArityMismatchCheck();
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
