package org.jetbrains.bazel.languages.starlark.utils;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.starlark.StarlarkUtils;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkQualifiableExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAugAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkStaticValueUtils.StarlarkStaticValueKind;
import org.jspecify.annotations.Nullable;

public final class StarlarkMutationUtils {
    private StarlarkMutationUtils() {
    }

    public record Mutation(
        PsiElement target,
        PsiElement problemElement,
        Kind kind,
        @Nullable String methodName
    ) {
        // consulo: upstream default argument (methodName = null)
        public Mutation(PsiElement target, PsiElement problemElement, Kind kind) {
            this(target, problemElement, kind, null);
        }

        public enum Kind {
            METHOD_CALL, SUBSCRIPTION_ASSIGNMENT, AUGMENTED_ASSIGNMENT
        }
    }

    public static @Nullable Mutation mutationOrNull(PsiElement element) {
        Mutation mutation = getMutatingCall(element);
        if (mutation != null) {
            return mutation;
        }
        mutation = getMutatingAssignment(element);
        if (mutation != null) {
            return mutation;
        }
        return getMutatingAugAssignment(element);
    }

    public static boolean areReferencesEqual(PsiElement left, PsiElement right) {
        PsiReference leftReference = left.getReference();
        PsiElement leftResolved = leftReference == null ? null : leftReference.resolve();
        PsiReference rightReference = right.getReference();
        PsiElement rightResolved = rightReference == null ? null : rightReference.resolve();
        if (leftResolved != null && rightResolved != null) {
            return leftResolved.equals(rightResolved);
        }

        String leftName = StarlarkQualifiableExpression.getSimpleNameOrNull(left);
        if (leftName == null) {
            return false;
        }
        String rightName = StarlarkQualifiableExpression.getSimpleNameOrNull(right);
        if (rightName == null) {
            return false;
        }
        return leftName.equals(rightName);
    }

    public static boolean isKnownMutatingMethod(String name) {
        return StarlarkStaticValueKind.KNOWN_MUTATING_METHODS.contains(name);
    }

    private static @Nullable Mutation getMutatingCall(PsiElement element) {
        if (!(element instanceof StarlarkCallExpression call)) {
            return null;
        }
        if (!(call.getCalledExpression() instanceof StarlarkReferenceExpression calledExpression)) {
            return null;
        }
        String methodName = calledExpression.getName();
        if (methodName == null) {
            return null;
        }
        if (!isKnownMutatingMethod(methodName)) {
            return null;
        }

        PsiElement target = calledExpression.getQualifierExpression();
        if (target == null || !StarlarkQualifiableExpression.isSimpleNameExpression(target)) {
            return null;
        }
        return new Mutation(target, calledExpression, Mutation.Kind.METHOD_CALL, methodName);
    }

    private static @Nullable Mutation getMutatingAssignment(PsiElement element) {
        if (!(element instanceof StarlarkAssignmentStatement assignment)) {
            return null;
        }
        PsiElement lhs = assignment.leftHandSide();
        if (lhs == null) {
            return null;
        }
        PsiElement target = extractSubscriptionReceiver(lhs);
        if (target == null) {
            return null;
        }
        return new Mutation(target, lhs, Mutation.Kind.SUBSCRIPTION_ASSIGNMENT);
    }

    private static @Nullable Mutation getMutatingAugAssignment(PsiElement element) {
        if (!(element instanceof StarlarkAugAssignmentStatement assignment)) {
            return null;
        }
        PsiElement lhs = assignment.leftHandSide();
        if (lhs == null) {
            return null;
        }
        PsiElement target = StarlarkQualifiableExpression.isSimpleNameExpression(lhs) ? lhs : extractSubscriptionReceiver(lhs);
        if (target == null) {
            return null;
        }
        return new Mutation(target, lhs, Mutation.Kind.AUGMENTED_ASSIGNMENT);
    }

    private static @Nullable PsiElement extractSubscriptionReceiver(@Nullable PsiElement element) {
        if (!(element instanceof StarlarkSubscriptionExpression subscription)) {
            return null;
        }
        PsiElement receiver = StarlarkUtils.nearestRelevantBeforeOperator(subscription.getFirstChild());
        return receiver != null && StarlarkQualifiableExpression.isSimpleNameExpression(receiver) ? receiver : null;
    }
}
