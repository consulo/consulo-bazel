package org.jetbrains.bazel.languages.starlark.psi.expressions;

import java.util.Objects;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jspecify.annotations.Nullable;

public class StarlarkGlobExpression extends StarlarkBaseElement {
    public StarlarkGlobExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitGlobExpression(this);
    }

    public @Nullable StarlarkArgumentList getArgList() {
        ASTNode argListNode = Objects.requireNonNull(getNode().getTreeParent()).findChildByType(StarlarkElementTypes.ARGUMENT_LIST);
        return argListNode == null ? null : (StarlarkArgumentList) argListNode.getPsi();
    }

    public StarlarkArgumentElement[] getArguments() {
        StarlarkArgumentList argList = getArgList();
        if (argList == null) {
            return new StarlarkArgumentElement[0];
        }
        return argList.getArguments();
    }

    public @Nullable StarlarkNamedArgumentExpression getKeywordArgument(String name) {
        StarlarkArgumentList list = getArgList();
        return list == null ? null : list.getKeywordArgument(name);
    }

    public @Nullable PsiElement getIncludes() {
        StarlarkArgumentElement arg = getKeywordArgument("include");
        if (arg == null) {
            StarlarkArgumentElement[] allArgs = getArguments();
            if (allArgs.length != 0 && allArgs[0] instanceof StarlarkArgumentExpression) {
                arg = allArgs[0];
            }
        }
        return arg == null ? null : arg.getValue();
    }

    public @Nullable PsiElement getExcludes() {
        StarlarkNamedArgumentExpression arg = getKeywordArgument("exclude");
        return arg == null ? null : arg.getValue();
    }

    public boolean areDirectoriesExcluded() {
        StarlarkNamedArgumentExpression arg = getKeywordArgument("exclude_directories");
        if (arg != null) {
            // '0' and '1' are the only accepted values
            PsiElement value = arg.getValue();
            return value == null || !value.getText().equals("0");
        }
        return true;
    }

    @Override
    public @Nullable PsiReference getReference() {
        StarlarkReferenceSupport support = StarlarkReferenceSupport.getInstance();
        return support == null ? null : support.getReference(this);
    }
}
