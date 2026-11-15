package org.jetbrains.bazel.languages.starlark.findusages;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.content.scope.SearchScope;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.find.FindUsagesOptions;
import consulo.language.psi.PsiElement;
import consulo.language.psi.search.ReferencesSearch.SearchParameters;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class StarlarkFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    @Override
    public boolean canFindUsages(PsiElement element) {
        return element instanceof StarlarkElement;
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(
        PsiElement element,
        boolean forHighlightUsages
    ) {
        return new StarlarkFindUsagesHandler(element);
    }
}

class StarlarkFindUsagesHandler extends FindUsagesHandler {
    StarlarkFindUsagesHandler(PsiElement element) {
        super(element);
    }

    @Override
    protected boolean isSearchForTextOccurrencesAvailable(PsiElement psiElement, boolean isSingleFile) {
        return false;
    }

    /**
     * Effectively, limits the search scope to starlark files.
     * Without this we might receive a search scope that is enlarged with {@link consulo.language.psi.UseScopeEnlarger},
     * which leads to unnecessary noise like e.g., Markdown file results.
     * <p>
     * Analogous limitation must be done in {@link org.jetbrains.bazel.languages.starlark.rename.StarlarkRenamePsiElementProcessor} for
     * consistency.
     */
    // TODO consulo: Consulo's FindUsagesHandler has no createSearchParameters hook (processElementUsages and
    //  findReferencesToHighlight build the SearchParameters inline), so this method is never called and the
    //  search scope is not limited to starlark files
    protected SearchParameters createSearchParameters(
        PsiElement target,
        SearchScope searchScope,
        @Nullable FindUsagesOptions findUsagesOptions
    ) {
        SearchScope scope = ReadAction
            .<SearchScope>nonBlocking(() -> searchScope.intersectWith(target.getUseScope()))
            .executeSynchronously();
        return new SearchParameters(target, scope, false, findUsagesOptions != null ? findUsagesOptions.fastTrack : null);
    }
}
