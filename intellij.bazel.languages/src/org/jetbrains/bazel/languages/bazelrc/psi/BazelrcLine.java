package org.jetbrains.bazel.languages.bazelrc.psi;

import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes;
import org.jspecify.annotations.Nullable;

public class BazelrcLine extends BazelrcBaseElement {
    public BazelrcLine(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(BazelrcElementVisitor visitor) {
        visitor.visitLine(this);
    }

    public @Nullable String getConfig() {
        PsiElement config = this.findChildByType(BazelrcTokenTypes.CONFIG);
        return config != null ? config.getText() : null;
    }

    public String getCommand() {
        PsiElement command = this.findChildByType(COMMAND_TOKENS);
        return command != null ? command.getText() : "";
    }

    public BazelrcFlag[] getFlags() {
        return this.findChildrenByClass(BazelrcFlag.class);
    }

    public static final TokenSet COMMAND_TOKENS = TokenSet.create(BazelrcTokenTypes.COMMAND);
}
