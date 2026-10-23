package org.jetbrains.bazel.languages.starlark.psi.expressions;

import java.util.Objects;
import consulo.component.util.Iconable;
import consulo.content.scope.SearchScope;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.starlark.StarlarkSearchScope;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableElement;
import org.jspecify.annotations.Nullable;

public class StarlarkTargetExpression extends StarlarkNamedElement
    implements StarlarkLocalVariableElement, StarlarkQualifiableExpression, Iconable {
    public StarlarkTargetExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitTargetExpression(this);
    }

    @Override
    public @Nullable PsiReference getReference() {
        StarlarkReferenceSupport support = StarlarkReferenceSupport.getInstance();
        return support == null ? null : support.getReference(this);
    }

    @Override
    public @Nullable Image getIcon(int flags) {
        return PlatformIconGroup.nodesVariable();
    }

    @Override
    public SearchScope getUseScope() {
        if (isTopLevel() && !isFilePrivate()) {
            return StarlarkSearchScope.starlarkProjectScope(getProject());
        }
        PsiElement parent = PsiTreeUtil
            .findFirstParent(this, it -> it instanceof StarlarkCompExpression || it instanceof StarlarkCallable);
        if (parent == null) {
            return super.getUseScope();
        }
        return new LocalSearchScope(parent);
    }

    public boolean isTopLevel() {
        PsiElement parent = getParent();
        PsiElement nextParent = Objects.requireNonNull(parent).getParent();
        StarlarkAssignmentStatement assignmentAncestor;
        if (parent instanceof StarlarkAssignmentStatement statement) {
            assignmentAncestor = statement;
        }
        else if (parent instanceof StarlarkTupleExpression && nextParent instanceof StarlarkAssignmentStatement statement) {
            assignmentAncestor = statement;
        }
        else {
            return false;
        }
        return assignmentAncestor.isTopLevel();
    }

    private boolean isFilePrivate() {
        String name = getName();
        return name != null && name.startsWith("_");
    }
}
