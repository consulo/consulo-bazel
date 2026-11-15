package org.jetbrains.bazel.languages.starlark.highlighting;

import consulo.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jspecify.annotations.Nullable;

public final class StarlarkSemanticHighlighting {
    private StarlarkSemanticHighlighting() {
    }

    /**
     * Resolves the semantic highlighting color for a Starlark leaf token.
     * Returns {@code null} for non-leaf tokens or when the token has no special meaning
     * beyond what the lexer-based {@link StarlarkSyntaxHighlighter} provides.
     */
    public static @Nullable TextAttributesKey starlarkSemanticHighlightingColor(PsiElement receiver) {
        IElementType elementType = PsiUtilCore.getElementType(receiver);
        PsiElement parent = receiver.getParent();
        if (elementType == StarlarkTokenTypes.EQ) {
            if (parent instanceof StarlarkNamedArgumentExpression) {
                return StarlarkHighlightingColors.NAMED_ARGUMENT;
            }
            else {
                return null;
            }
        }
        else if (elementType == StarlarkTokenTypes.IDENTIFIER) {
            if (parent instanceof StarlarkNamedArgumentExpression) {
                return StarlarkHighlightingColors.NAMED_ARGUMENT;
            }
            else if (parent instanceof StarlarkFunctionDeclaration) {
                return StarlarkHighlightingColors.FUNCTION_DECLARATION;
            }
            // top-level calls in BUILD/MODULE files
            else if (parent instanceof StarlarkReferenceExpression &&
                parent.getParent() instanceof StarlarkCallExpression callExpression &&
                StarlarkCallExpression.isBazelFileTopLevelCall(callExpression)) {
                return StarlarkHighlightingColors.FUNCTION_DECLARATION;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }
}
