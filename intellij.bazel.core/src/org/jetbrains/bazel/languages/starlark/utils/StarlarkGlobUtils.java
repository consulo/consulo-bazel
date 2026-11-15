package org.jetbrains.bazel.languages.starlark.utils;

import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob;
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlobExpressionGlob;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class StarlarkGlobUtils {
    private StarlarkGlobUtils() {
    }

    /**
     * Heuristically checks if the given "srcs = ..." expression positively matches the source file
     */
    public static boolean srcsExpressionMatchPath(PsiElement expr, String relativePath) {
        if (expr instanceof StarlarkCallExpression callExpression) {
            PsiElement calledExpression = callExpression.getCalledExpression();
            return calledExpression != null && srcsExpressionMatchPath(calledExpression, relativePath);
        }

        if (expr instanceof StarlarkGlobExpression globExpression) {
            StarlarkGlob glob = StarlarkGlobExpressionGlob.getGlob(globExpression);
            return glob != null && glob.match(relativePath);
        }

        if (expr instanceof StarlarkStringLiteralExpression stringLiteralExpression) {
            String value = stringLiteralExpression.getStringContents();
            // TODO: support label refs
            if (value.contains(":") || value.startsWith("@") || value.startsWith("//")) {
                return false;
            }
            return value.equals(relativePath);
        }

        if (expr instanceof StarlarkListLiteralExpression listLiteralExpression) {
            return listLiteralExpression.getElements().stream().anyMatch(it -> srcsExpressionMatchPath(it, relativePath));
        }

        if (expr instanceof StarlarkBinaryExpression binaryExpression && binaryExpression.getOperator() == StarlarkTokenTypes.PLUS) {
            return binaryExpression.getOperands().stream().anyMatch(it -> srcsExpressionMatchPath(it, relativePath));
        }

        return false;
    }
}
