package org.jetbrains.bazel.languages.bazelquery.elements;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryElementType.BazelQueryFlagsElementType;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryCommand;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFlag;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFlagVal;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryInteger;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryWord;

public final class BazelQueryElementTypes {
    private BazelQueryElementTypes() {
    }

    public static final BazelQueryElementType COMMAND = new BazelQueryElementType("COMMAND");
    public static final BazelQueryElementType WORD = new BazelQueryElementType("WORD");
    public static final BazelQueryElementType INTEGER = new BazelQueryElementType("INTEGER");
    public static final BazelQueryFlagsElementType FLAG = new BazelQueryFlagsElementType("FLAG");
    public static final BazelQueryFlagsElementType FLAG_VAL = new BazelQueryFlagsElementType("FLAG_VAL");

    public static PsiElement createElement(ASTNode node) {
        IElementType type = node.getElementType();
        if (type == COMMAND) {
            return new BazelQueryCommand(node);
        }
        else if (type == WORD) {
            return new BazelQueryWord(node);
        }
        else if (type == FLAG) {
            return new BazelQueryFlag(node);
        }
        else if (type == FLAG_VAL) {
            return new BazelQueryFlagVal(node);
        }
        else if (type == INTEGER) {
            return new BazelQueryInteger(node);
        }
        else {
            throw new IllegalStateException("Unknown element type: " + type);
        }
    }
}
