package org.jetbrains.bazel.languages.starlark.utils;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue;
import org.jspecify.annotations.Nullable;


public final class StarlarkCallableResolver {
    private StarlarkCallableResolver() {
    }

    public static @Nullable StarlarkCallable resolve(StarlarkCallExpression call) {
        PsiElement expression = call.getCalledExpression();
        if (expression == null) {
            return null;
        }

        StarlarkLambdaExpression lambdaExpression = findLambdaExpression(expression);
        if (lambdaExpression != null) {
            return lambdaExpression;
        }

        StarlarkReferenceExpression refExpr = findReferenceExpression(expression);
        if (refExpr == null) {
            return null;
        }
        PsiReference reference = refExpr.getReference();
        PsiElement resolved = reference == null ? null : reference.resolve();
        if (resolved == null) {
            return null;
        }

        return getCallableFromResolvedReference(resolved);
    }

    private static @Nullable StarlarkLambdaExpression findLambdaExpression(PsiElement expression) {
        return PsiTreeUtil.getChildOfType(expression, StarlarkLambdaExpression.class);
    }

    private static @Nullable StarlarkReferenceExpression findReferenceExpression(PsiElement expression) {
        if (expression instanceof StarlarkReferenceExpression referenceExpression) {
            return referenceExpression;
        }
        return PsiTreeUtil.getChildOfType(expression, StarlarkReferenceExpression.class);
    }

    private static @Nullable StarlarkCallable getCallableFromResolvedReference(PsiElement resolved) {
        if (resolved instanceof StarlarkCallable callable) {
            return callable;
        }
        else if (resolved instanceof StarlarkTargetExpression targetExpression) {
            return resolveLambdaAlias(targetExpression);
        }
        else if (resolved instanceof StarlarkNamedLoadValue namedLoadValue) {
            return resolveLoadedCallable(namedLoadValue);
        }
        else {
            return null;
        }
    }

    private static @Nullable StarlarkCallable resolveLambdaAlias(StarlarkTargetExpression target) {
        StarlarkAssignmentStatement assignment = PsiTreeUtil.getParentOfType(target, StarlarkAssignmentStatement.class, false);
        if (assignment == null) {
            return null;
        }
        return findLambdaExpression(assignment);
    }

    private static @Nullable StarlarkCallable resolveLoadedCallable(StarlarkNamedLoadValue loadValue) {
        StarlarkStringLiteralExpression loaded = PsiTreeUtil.getChildOfType(loadValue, StarlarkStringLiteralExpression.class);
        if (loaded == null) {
            return null;
        }
        PsiReference reference = loaded.getReference();
        PsiElement resolved = reference == null ? null : reference.resolve();
        return resolved instanceof StarlarkCallable callable ? callable : null;
    }
}
