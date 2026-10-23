package org.jetbrains.bazel.languages.starlark;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiWhiteSpace;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class StarlarkUtils {
    private StarlarkUtils() {
    }

    public static final TokenSet STARLARK_ASSIGNMENT_OPERATORS = TokenSet.orSet(
        TokenSet.create(StarlarkTokenTypes.EQ),
        StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS,
        TokenSet.create(StarlarkTokenTypes.IN_KEYWORD)
    );
    public static final TokenSet STARLARK_WHITESPACES_AND_COMMENTS = TokenSet.orSet(
        StarlarkTokenSets.WHITESPACE,
        StarlarkTokenSets.COMMENT
    );

    public static @Nullable PsiElement nearestRelevantBeforeOperator(@Nullable PsiElement node) {
        PsiElement current = node;
        while (current != null) {
            IElementType elementType = PsiUtilCore.getElementType(current);
            if (STARLARK_ASSIGNMENT_OPERATORS.contains(elementType)) {
                return null;
            }
            if (current instanceof StarlarkBaseElement && !STARLARK_WHITESPACES_AND_COMMENTS.contains(elementType)) {
                return current;
            }
            current = current.getNextSibling();
        }
        return null;
    }

    public static @Nullable PsiElement previousRelevantSibling(PsiElement element) {
        PsiElement current = element.getPrevSibling();
        while (current != null) {
            IElementType elementType = PsiUtilCore.getElementType(current);
            if (current instanceof StarlarkBaseElement && !STARLARK_WHITESPACES_AND_COMMENTS.contains(elementType)) {
                return current;
            }
            current = current.getPrevSibling();
        }
        return null;
    }

    public static @Nullable PsiElement nextRelevantSibling(PsiElement element) {
        PsiElement current = element.getNextSibling();
        while (current != null) {
            IElementType elementType = PsiUtilCore.getElementType(current);
            if (current instanceof StarlarkBaseElement && !STARLARK_WHITESPACES_AND_COMMENTS.contains(elementType)) {
                return current;
            }
            current = current.getNextSibling();
        }
        return null;
    }

    public static @Nullable PsiElement nearestRelevantChildAfter(PsiElement node, StarlarkTokenType separator) {
        PsiElement current = node.getFirstChild();
        while (current != null) {
            if (PsiUtilCore.getElementType(current) == separator) {
                return nextRelevantSibling(current);
            }
            current = current.getNextSibling();
        }
        return null;
    }

    public static List<PsiElement> relevantChildrenAfterEach(PsiElement node, StarlarkTokenType separator) {
        List<PsiElement> result = new ArrayList<>();
        PsiElement current = node.getFirstChild();
        while (current != null) {
            if (PsiUtilCore.getElementType(current) == separator) {
                PsiElement next = nextRelevantSibling(current);
                if (next != null) {
                    result.add(next);
                }
            }
            current = current.getNextSibling();
        }
        return result;
    }

    public static List<Pair<PsiElement, PsiElement>> relevantChildrenAroundEach(PsiElement node, StarlarkTokenType separator) {
        List<Pair<PsiElement, PsiElement>> result = new ArrayList<>();
        PsiElement current = node.getFirstChild();
        while (current != null) {
            if (PsiUtilCore.getElementType(current) == separator) {
                PsiElement left = previousRelevantSibling(current);
                PsiElement right = nextRelevantSibling(current);
                if (left != null && right != null) {
                    result.add(Pair.create(left, right));
                }
            }
            current = current.getNextSibling();
        }
        return result;
    }

    /**
     * original version: {@code com.jetbrains.python.psi.impl.PyPsiUtils.getNextNonWhitespaceSibling(com.intellij.lang.ASTNode)}
     */
    public static @Nullable ASTNode getNextNonWhitespaceSibling(ASTNode after) {
        ASTNode next = after.getTreeNext();
        while (next != null) {
            if (!(next instanceof PsiWhiteSpace)) {
                return next;
            }
            next = next.getTreeNext();
        }
        return null;
    }
}
