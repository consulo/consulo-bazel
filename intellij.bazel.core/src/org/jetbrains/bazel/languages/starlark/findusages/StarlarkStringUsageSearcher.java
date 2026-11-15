package org.jetbrains.bazel.languages.starlark.findusages;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch.SearchParameters;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.search.UsageSearchContext;
import consulo.project.util.query.QueryExecutorBase;
import org.jetbrains.bazel.languages.starlark.StarlarkSearchScope;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;

import java.util.function.Predicate;

@ExtensionImpl
public class StarlarkStringUsageSearcher extends QueryExecutorBase<PsiReference, SearchParameters>
    implements ReferencesSearchQueryExecutor {
    @Override
    public void processQuery(SearchParameters params, Predicate<? super PsiReference> processor) {
        if (!(params.getElementToSearch() instanceof StarlarkNamedElement searchee)) {
            return;
        }
        String name = searchee.getName();
        if (name == null) {
            return;
        }
        if (!(params.getScopeDeterminedByUser() instanceof GlobalSearchScope userScope)) {
            return;
        }
        GlobalSearchScope searchScope = StarlarkSearchScope.restrictToStarlarkFiles(userScope);
        params.getOptimizer().searchWord(name, searchScope, UsageSearchContext.IN_STRINGS, true, searchee);
    }
}
