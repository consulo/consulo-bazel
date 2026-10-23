package org.jetbrains.bazel.languages.starlark.psi.functions;

import consulo.component.util.Iconable;
import consulo.content.scope.SearchScope;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jspecify.annotations.Nullable;

public abstract class StarlarkParameter extends StarlarkNamedElement implements Iconable {
    public StarlarkParameter(ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable Image getIcon(int flags) {
        return PlatformIconGroup.nodesParameter();
    }

    @Override
    public SearchScope getUseScope() {
        PsiElement callable = PsiTreeUtil
            .findFirstParent(this, it -> it instanceof StarlarkCallable);
        if (callable == null) {
            return super.getUseScope();
        }
        return new LocalSearchScope(callable);
    }
}
