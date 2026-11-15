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
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAugAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator;

@ExtensionImpl
public class StarlarkInvalidAssignmentTargetInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new InvalidAssignmentTargetVisitor(holder);
    }

    private static class InvalidAssignmentTargetVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        InvalidAssignmentTargetVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitAssignmentStatement(StarlarkAssignmentStatement node) {
            PsiElement lhs = node.leftHandSide();
            if (lhs != null) {
                searchForInvalidTargetAndReport(lhs);
            }
        }

        @Override
        public void visitAugAssignmentStatement(StarlarkAugAssignmentStatement node) {
            PsiElement lhs = node.leftHandSide();
            if (lhs == null) {
                return;
            }
            if (lhs instanceof StarlarkTupleExpression ||
                lhs instanceof StarlarkListLiteralExpression ||
                (lhs instanceof StarlarkParenthesizedExpression parenthesizedExpression && parenthesizedExpression.getTuple() != null)) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionAssignmentTargetAugmentedSequence()).range(lhs).create();
                return;
            }
            searchForInvalidTargetAndReport(lhs);
        }

        @Override
        public void visitForStatement(StarlarkForStatement node) {
            List<StarlarkTargetExpression> loopVars = node.getLoopVariables();
            if (!loopVars.isEmpty()) {
                loopVars.forEach(this::searchForInvalidTargetAndReport);
            }
            else {
                PsiElement lhs = node.leftHandSide();
                if (lhs != null) {
                    searchForInvalidTargetAndReport(lhs);
                }
            }
        }

        private void searchForInvalidTargetAndReport(PsiElement target) {
            PsiElement invalid = firstInvalidTarget(target);
            if (invalid == null) {
                return;
            }
            myHolder.newProblem(StarlarkLocalize.inspectionDescriptionAssignmentTargetInvalid(invalid.getText()))
                .range(invalid)
                .create();
        }

        private @Nullable PsiElement firstInvalidTarget(PsiElement target) {
            if (target instanceof StarlarkTargetExpression ||
                target instanceof StarlarkSubscriptionExpression ||
                target instanceof StarlarkReferenceExpression) {
                return null;
            }
            else if (target instanceof StarlarkParenthesizedExpression parenthesizedExpression) {
                StarlarkTupleExpression tuple = parenthesizedExpression.getTuple();
                return tuple != null ? firstInvalidTarget(tuple) : target;
            }
            else if (target instanceof StarlarkTupleExpression || target instanceof StarlarkListLiteralExpression) {
                PsiElement elem = nearestRelevantBeforeOperator(target.getFirstChild());
                while (elem != null) {
                    PsiElement invalid = firstInvalidTarget(elem);
                    if (invalid != null) {
                        return invalid;
                    }
                    elem = nearestRelevantBeforeOperator(elem.getNextSibling());
                }
                return null;
            }
            else {
                return target;
            }
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkInvalidAssignmentTarget";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkInvalidAssignmentTargetCheck();
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
