package org.jetbrains.bazel.languages.starlark.utils;

import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.starlark.StarlarkUtils;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class StarlarkAssignmentUtils {
    private StarlarkAssignmentUtils() {
    }

    public static @Nullable PsiElement assignedValueForTarget(StarlarkTargetExpression target) {
        StarlarkAssignmentStatement assignment = containingAssignment(target);
        if (assignment == null) {
            return null;
        }
        return assignedValueFor(assignment, target);
    }

    private static @Nullable StarlarkAssignmentStatement containingAssignment(PsiElement receiver) {
        if (receiver instanceof StarlarkAssignmentStatement assignmentStatement) {
            return assignmentStatement;
        }
        return PsiTreeUtil.getParentOfType(receiver, StarlarkAssignmentStatement.class, false);
    }

    private static @Nullable PsiElement assignedValueFor(StarlarkAssignmentStatement receiver, StarlarkTargetExpression target) {
        PsiElement lhs = receiver.leftHandSide();
        if (lhs == null) {
            return null;
        }
        PsiElement rhs = receiver.rightHandSide();
        if (rhs == null) {
            return null;
        }
        return matchAssignedValue(lhs, rhs, target);
    }

    private static @Nullable PsiElement matchAssignedValue(PsiElement lhs, PsiElement rhs, StarlarkTargetExpression target) {
        if (lhs.equals(target)) {
            return rhs;
        }

        List<PsiElement> lhsElements = destructuringTargets(lhs);
        if (lhsElements == null) {
            return null;
        }
        List<PsiElement> rhsElements = destructuringElements(rhs);
        if (rhsElements == null) {
            return null;
        }
        if (lhsElements.size() != rhsElements.size()) {
            return null;
        }

        for (int i = 0; i < lhsElements.size(); i++) {
            PsiElement matched = matchAssignedValue(lhsElements.get(i), rhsElements.get(i), target);
            if (matched != null) {
                return matched;
            }
        }

        return null;
    }

    public static @Nullable List<PsiElement> destructuringTargets(PsiElement element) {
        if (element instanceof StarlarkParenthesizedExpression parenthesizedExpression) {
            PsiElement inner = parenthesizedExpression.getTuple();
            if (inner == null) {
                inner = singleOrNull(expressionElements(parenthesizedExpression));
            }
            return inner == null ? null : destructuringTargets(inner);
        }
        else if (element instanceof StarlarkTupleExpression
            || element instanceof StarlarkListLiteralExpression) {
            return expressionElements(element);
        }
        else {
            return null;
        }
    }

    public static @Nullable List<PsiElement> destructuringElements(PsiElement element) {
        if (element instanceof StarlarkParenthesizedExpression parenthesizedExpression) {
            PsiElement inner = parenthesizedExpression.getTuple();
            if (inner == null) {
                inner = singleOrNull(expressionElements(parenthesizedExpression));
            }
            return inner == null ? null : destructuringElements(inner);
        }
        else if (element instanceof StarlarkTupleExpression
            || element instanceof StarlarkListLiteralExpression) {
            return expressionElements(element);
        }
        else if (element instanceof StarlarkDictLiteralExpression dictLiteralExpression) {
            return dictLiteralExpression.getKeys();
        }
        else if (element instanceof StarlarkFalseLiteralExpression
            || element instanceof StarlarkFloatLiteralExpression
            || element instanceof StarlarkIntegerLiteralExpression
            || element instanceof StarlarkNoneLiteralExpression
            || element instanceof StarlarkStringLiteralExpression
            || element instanceof StarlarkTrueLiteralExpression) {
            return List.of(element);
        }
        else {
            return null;
        }
    }

    private static List<PsiElement> expressionElements(PsiElement receiver) {
        List<PsiElement> result = new ArrayList<>();
        PsiElement child = StarlarkUtils.nearestRelevantBeforeOperator(receiver.getFirstChild());
        while (child != null) {
            result.add(child);
            child = StarlarkUtils.nearestRelevantBeforeOperator(child.getNextSibling());
        }
        return result;
    }

    // consulo: Kotlin List.singleOrNull()
    private static @Nullable PsiElement singleOrNull(List<PsiElement> list) {
        return list.size() == 1 ? list.get(0) : null;
    }
}
