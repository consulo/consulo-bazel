package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class StarlarkNamedArgumentExpression extends StarlarkBaseElement implements StarlarkArgumentElement {
    public StarlarkNamedArgumentExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitNamedArgumentExpression(this);
    }

    @Override
    public @Nullable PsiReference getReference() {
        StarlarkReferenceSupport support = StarlarkReferenceSupport.getInstance();
        return support == null ? null : support.getReference(this);
    }

    @Override
    public @Nullable String getName() {
        ASTNode nameNode = getNameNode();
        return nameNode == null ? null : nameNode.getText();
    }

    public @Nullable ASTNode getNameNode() {
        return getNode().findChildByType(StarlarkTokenTypes.IDENTIFIER);
    }

    public boolean isSrcsArgument() {
        return containsArgumentWithName("srcs");
    }

    public boolean isResourcesArgument() {
        return containsArgumentWithName("resources");
    }

    public boolean isNameArgument() {
        return containsArgumentWithName("name");
    }

    public boolean isDepsArgument() {
        return containsArgumentWithName("deps");
    }

    // Some companies may have custom macros that use another name
    public boolean isDependenciesArgument() {
        return containsArgumentWithName("dependencies");
    }

    public boolean containsArgumentWithName(String name) {
        ASTNode identifier = getNode().findChildByType(StarlarkTokenTypes.IDENTIFIER);
        return Objects.equals(identifier == null ? null : identifier.getText(), name);
    }

    public @Nullable String getArgumentStringValue() {
        StarlarkStringLiteralExpression literal = findChildByClass(StarlarkStringLiteralExpression.class);
        return literal == null ? null : literal.getStringContents();
    }

    // consulo: upstream PsiSymbolReference own references are merged into getReferences()
    public Collection<PsiReference> getOwnReferences() {
        StarlarkReferenceSupport support = StarlarkReferenceSupport.getInstance();
        return support == null ? List.of() : support.getOwnReferences(this);
    }

    @Override
    public PsiReference[] getReferences() {
        List<PsiReference> references = new ArrayList<>();
        PsiReference reference = getReference();
        if (reference != null) {
            references.add(reference);
        }
        references.addAll(getOwnReferences());
        return references.toArray(PsiReference.EMPTY_ARRAY);
    }
}
