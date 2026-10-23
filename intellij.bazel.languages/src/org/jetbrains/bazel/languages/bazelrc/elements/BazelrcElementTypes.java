package org.jetbrains.bazel.languages.bazelrc.elements;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcImport;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine;

public final class BazelrcElementTypes {
    private BazelrcElementTypes() {
    }

    public static final BazelrcElementType LINE = new BazelrcElementType("LINE");
    public static final BazelrcElementType FLAG = new BazelrcElementType("FLAG");
    public static final BazelrcElementType IMPORT = new BazelrcElementType("IMPORT");

    public static PsiElement createElement(ASTNode node) {
        IElementType type = node.getElementType();
        if (type == LINE) {
            return new BazelrcLine(node);
        }
        else if (type == FLAG) {
            return new BazelrcFlag(node);
        }
        else if (type == IMPORT) {
            return new BazelrcImport(node);
        }
        else {
            throw new IllegalStateException("Unknown element type: " + type);
        }
    }
}
