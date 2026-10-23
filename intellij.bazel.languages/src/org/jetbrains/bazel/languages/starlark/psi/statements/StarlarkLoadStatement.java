package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class StarlarkLoadStatement extends StarlarkBaseElement {
    public StarlarkLoadStatement(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitLoadStatement(this);
    }

    public @Nullable StarlarkStringLiteralExpression getLoadedFileNamePsi() {
        PsiElement filenameLoadValue = findChildByType(StarlarkElementTypes.FILENAME_LOAD_VALUE);
        return filenameLoadValue instanceof StarlarkFilenameLoadValue value ? value.getLoadValueExpression() : null;
    }

    public List<StarlarkElement> getLoadedSymbolsPsi() {
        return Arrays.stream(getChildren())
            .filter(it -> it instanceof StarlarkLoadValue)
            .map(it -> (StarlarkElement) it)
            .toList();
    }
}
