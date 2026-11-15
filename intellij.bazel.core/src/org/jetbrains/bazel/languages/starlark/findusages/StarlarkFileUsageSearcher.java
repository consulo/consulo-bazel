package org.jetbrains.bazel.languages.starlark.findusages;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopeUtil;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.search.ReferencesSearch.SearchParameters;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.search.UsageSearchContext;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.util.query.QueryExecutorBase;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.StarlarkSearchScope;
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob;
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlobExpressionGlob;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;

import java.util.function.Predicate;

import static org.jetbrains.bazel.config.project.isBazelProject;

/**
 * Finds file usages in Starlark files. Requires {@link StarlarkFileUseScopeEnlarger} to enlarge the scope of the {@link PsiFile} with
 * Starlark files.
 */
@ExtensionImpl
public class StarlarkFileUsageSearcher extends QueryExecutorBase<PsiReference, SearchParameters> implements ReferencesSearchQueryExecutor {
    public StarlarkFileUsageSearcher() {
        super(true);
    }

    @Override
    @RequiredReadAction
    public void processQuery(SearchParameters params, Predicate<? super PsiReference> processor) {
        if (!isBazelProject(params.getProject())) {
            return;
        }
        if (!(params.getElementToSearch() instanceof PsiFile file)) {
            return;
        }
        GlobalSearchScope scope = StarlarkSearchScope.restrictToStarlarkFiles(
            GlobalSearchScopeUtil.toGlobalSearchScope(params.getEffectiveSearchScope(), params.getProject())
        );
        if (!processExplicitLabelReferences(file, scope, processor)) {
            return;
        }
        processGlobReferences(file, scope, processor);
    }

    /**
     * Uses the word index to find only Starlark files whose string literals might contain the target filename,
     * then checks those candidates for explicit label references.
     */
    private boolean processExplicitLabelReferences(
        PsiFile file,
        GlobalSearchScope scope,
        Predicate<? super PsiReference> processor
    ) {
        return PsiSearchHelper
            .getInstance(file.getProject())
            .processCandidateFilesForText(
                scope,
                UsageSearchContext.IN_STRINGS,
                true,
                file.getName(),
                candidate -> {
                    if (!(PsiManager.getInstance(file.getProject()).findFile(candidate) instanceof StarlarkFile starlarkFile)) {
                        return true;
                    }
                    return PsiTreeUtil.processElements(
                        starlarkFile,
                        StarlarkStringLiteralExpression.class,
                        element -> !isReferringToFile(element, file, starlarkFile) || processor.test(new BazelLabelReference(element, true))
                    );
                }
            );
    }

    /**
     * Finds the BUILD file for the Bazel package to which {@code file} belongs to and checks if any glob in this BUILD file contains
     * {@code file}.
     * <p>
     * It's enough to search through only single BUILD file due to the
     * <a href="https://bazel.build/reference/be/functions#glob">glob restrictions</a>.
     * Globs are matching only files in current Bazel package and cannot cross the package boundaries.
     */
    @RequiredReadAction
    private void processGlobReferences(
        PsiFile file,
        GlobalSearchScope scope,
        Predicate<? super PsiReference> processor
    ) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return;
        }
        VirtualFile dir = virtualFile.getParent();
        if (dir == null) {
            return;
        }
        // consulo: Kotlin generateSequence(dir) { it.parent }.firstNotNullOfOrNull(::findBuildFile)
        VirtualFile buildFile = null;
        for (VirtualFile it = dir; it != null && buildFile == null; it = it.getParent()) {
            buildFile = LabelResolveUtils.findBuildFile(it);
        }
        if (buildFile == null) {
            return;
        }
        if (!scope.contains(buildFile)) {
            return;
        }
        if (!(PsiManager.getInstance(file.getProject()).findFile(buildFile) instanceof StarlarkFile starlarkFile)) {
            return;
        }
        PsiTreeUtil.processElements(starlarkFile, StarlarkGlobExpression.class, element -> {
            StarlarkGlob glob = StarlarkGlobExpressionGlob.getGlob(element);
            PsiReference ref;
            if (glob != null && glob.match(virtualFile)) {
                ref = element.getReference();
            }
            else {
                ref = null;
            }
            return ref == null || processor.test(ref);
        });
    }

    @RequiredReadAction
    private boolean isReferringToFile(
        StarlarkStringLiteralExpression literal,
        PsiFile targetFile,
        PsiFile contextFile
    ) {
        Label label = Label.parseOrNull(literal.getStringContents());
        if (label == null) {
            return false;
        }
        VirtualFile contextVFile = contextFile.getOriginalFile().getVirtualFile();
        if (contextVFile == null) {
            return false;
        }
        PsiElement resolvedElement = LabelResolveUtils.resolveLabel(targetFile.getProject(), label, contextVFile, true);
        return resolvedElement instanceof PsiFile
            && PsiManager.getInstance(literal.getProject()).areElementsEquivalent(resolvedElement, targetFile);
    }
}
