package org.jetbrains.bazel.languages.starlark.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.content.scope.SearchScope;
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.util.collection.MultiMap;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;

import java.util.Collection;
import java.util.Objects;

@ExtensionImpl
public class StarlarkRenamePsiElementProcessor extends RenamePsiElementProcessor {
    @Override
    public boolean canProcessElement(PsiElement element) {
        return element instanceof StarlarkElement;
    }

    /**
     * Effectively, limits the search scope to starlark files.
     * Without this we receive a search scope that is enlarged with {@link consulo.language.psi.UseScopeEnlarger},
     * which leads to unexpected renames in e.g., Markdown files.
     * <p>
     * Analogous limitation must be done in {@code org.jetbrains.bazel.languages.starlark.findusages.StarlarkFindUsagesHandler}
     * for consistency.
     */
    // consulo: Consulo has no findReferences(element, searchScope, searchInCommentsAndStrings) overload;
    // the scope it uses is the one of ReferencesSearch.search(element), i.e. GlobalSearchScope.allScope
    @Override
    public Collection<PsiReference> findReferences(
        PsiElement element,
        boolean searchInCommentsAndStrings
    ) {
        SearchScope searchScope = GlobalSearchScope.allScope(element.getProject());
        return ReferencesSearch.search(element, searchScope.intersectWith(element.getUseScope())).findAll();
    }

    @Override
    @RequiredReadAction
    public void findExistingNameConflicts(
        PsiElement element,
        String newName,
        MultiMap<PsiElement, LocalizeValue> conflicts
    ) {
        if (element instanceof StarlarkCallExpression callExpression) {
            findRuleTargetConflicts(callExpression, newName, conflicts);
        }
        else if (element instanceof StarlarkNamedElement namedElement) {
            findNamedElementConflicts(namedElement, newName, conflicts);
        }
    }

    @RequiredReadAction
    private void findRuleTargetConflicts(StarlarkCallExpression callExpr, String newName, MultiMap<PsiElement, LocalizeValue> conflicts) {
        if (!(callExpr.getContainingFile() instanceof StarlarkFile buildFile)) {
            return;
        }
        StarlarkCallExpression existingTarget = buildFile.findTargetRule(newName);
        if (existingTarget == null) {
            return;
        }
        if (!existingTarget.equals(callExpr)) {
            conflicts.putValue(existingTarget, StarlarkLocalize.renameTargetConflictAlreadyExists(newName, buildFile.getName()));
        }
    }

    @RequiredReadAction
    private void findNamedElementConflicts(StarlarkNamedElement element, String newName, MultiMap<PsiElement, LocalizeValue> conflicts) {
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return;
        }
        SearchScope useScope = element.getUseScope();
        PsiElement[] scopeElements =
            useScope instanceof LocalSearchScope localSearchScope ? localSearchScope.getScope() : new PsiElement[]{file};
        String scopeName = scopeElements.length == 1 && scopeElements[0] instanceof StarlarkNamedElement scopeNamedElement
            ? scopeNamedElement.getName()
            : null;
        String conflictScopeName = scopeName != null ? scopeName : file.getName();
        for (PsiElement scopeElement : scopeElements) {
            for (StarlarkNamedElement candidate : PsiTreeUtil.collectElementsOfType(scopeElement, StarlarkNamedElement.class)) {
                if (candidate.equals(element) || !Objects.equals(candidate.getName(), newName)) {
                    continue;
                }
                // Only flag a conflict if the candidate is declared in the same or enclosing scope,
                // not in a nested subscope (comprehension, lambda) that doesn't actually conflict.
                SearchScope candidateScope = candidate.getUseScope();
                if (candidateScope instanceof LocalSearchScope localCandidateScope
                    && !localCandidateScope.containsRange(file, element.getTextRange())) {
                    continue;
                }
                conflicts.putValue(candidate, StarlarkLocalize.renameNameConflictAlreadyExists(newName, conflictScopeName));
            }
        }
    }
}
