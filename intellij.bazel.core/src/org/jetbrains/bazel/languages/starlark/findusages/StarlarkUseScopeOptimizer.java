package org.jetbrains.bazel.languages.starlark.findusages;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.DelegatingGlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ScopeOptimizer;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.starlark.index.StarlarkLoadsIndex;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jspecify.annotations.Nullable;

/**
 * Narrows the use scope to only the defining file and files that actually load the searched Starlark element.
 */
// consulo: upstream registers it as `useScopeOptimizer`; Consulo has a single ScopeOptimizer extension point,
// applied by SearchRequestCollector to the code usage search scope
@ExtensionImpl
public class StarlarkUseScopeOptimizer implements ScopeOptimizer {
    @Override
    public @Nullable SearchScope getRestrictedUseScope(PsiElement element) {
        if (!(element instanceof StarlarkNamedElement namedElement)) {
            return null;
        }
        if (!(element.getUseScope() instanceof GlobalSearchScope baseScope)) {
            return null;
        }
        return new StarlarkLoadersScope(baseScope, namedElement);
    }
}

/**
 * A search scope that wraps the {@code baseScope} and limits it to files that actually load the {@code element}.
 */
class StarlarkLoadersScope extends DelegatingGlobalSearchScope {
    private final StarlarkNamedElement myElement;

    StarlarkLoadersScope(
        GlobalSearchScope baseScope,
        StarlarkNamedElement element
    ) {
        super(baseScope);
        myElement = element;
    }

    @Override
    @RequiredReadAction
    public boolean contains(VirtualFile file) {
        if (!super.contains(file)) {
            return false;
        }
        PsiFile containingFile = myElement.getContainingFile();
        if (file.equals(containingFile == null ? null : containingFile.getVirtualFile())) {
            return true;
        }
        return StarlarkLoadsIndex.isLoaded(myElement, GlobalSearchScope.fileScope(myElement.getProject(), file));
    }
}
