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
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.search.UsageSearchContext;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.project.util.query.QueryExecutorBase;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label.Package;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.StarlarkSearchScope;
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.jetbrains.bazel.config.project.isBazelProject;

/**
 * Finds all references to a Bazel target name (e.g., `":lib"`, `"//core:lib"`) across Starlark files.
 */
@ExtensionImpl
public class BazelTargetReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>
    implements ReferencesSearchQueryExecutor {
    public BazelTargetReferenceSearcher() {
        super(true);
    }

    @Override
    @RequiredReadAction
    public void processQuery(ReferencesSearch.SearchParameters queryParameters, Predicate<? super PsiReference> consumer) {
        Project project = queryParameters.getProject();
        if (!isBazelProject(project)) {
            return;
        }
        PsiElement elementToSearch = queryParameters.getElementToSearch();
        if (!StarlarkCallExpression.isRuleTarget(elementToSearch)) {
            return;
        }
        StarlarkCallExpression element = (StarlarkCallExpression) elementToSearch;
        String targetName = element.getNameAttributeValue();
        if (targetName == null) {
            return;
        }
        if (targetName.isEmpty()) {
            return;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        GlobalSearchScope scope = StarlarkSearchScope.restrictToStarlarkFiles(
            GlobalSearchScopeUtil.toGlobalSearchScope(queryParameters.getEffectiveSearchScope(), project),
            BazelFileType.BUILD
        );
        PsiFile containingFile = element.getContainingFile();
        VirtualFile ownBuildFile = containingFile == null ? null : containingFile.getOriginalFile().getVirtualFile();
        if (ownBuildFile != null
            && scope.contains(ownBuildFile)
            && !processTargetReferences(element, ownBuildFile, psiManager, consumer)) {
            return;
        }
        String text = computeTargetSearchText(targetName, element);
        PsiSearchHelper
            .getInstance(project)
            .processCandidateFilesForText(
                scope,
                UsageSearchContext.IN_STRINGS,
                true,
                text,
                virtualFile -> {
                    if (virtualFile.equals(ownBuildFile)) {
                        return true;
                    }
                    return processTargetReferences(element, virtualFile, psiManager, consumer);
                }
            );
    }

    /**
     * Computes the search text for {@link PsiSearchHelper#processCandidateFilesForText}.
     * <p>
     * The word index finds files containing ALL words from the search text.
     * Using just the target name (e.g., "core") matches too many files.
     * Cross-package labels must contain the package path segments (e.g., "//lib/core:core"),
     * so we search for all package path segments plus the target name.
     * For shorthand labels like "//lib/bar" (meaning "//lib/bar:bar"), the package path
     * segments alone are sufficient to find the file.
     */
    @RequiredReadAction
    private String computeTargetSearchText(String targetName, StarlarkCallExpression element) {
        PsiFile containingFile = element.getContainingFile();
        VirtualFile buildFile = containingFile == null ? null : containingFile.getOriginalFile().getVirtualFile();
        if (buildFile == null) {
            return targetName;
        }
        ResolvedLabel label = BazelRepoMappingUtils.calculateLabel(element.getProject(), buildFile);
        if (label == null || !(label.packagePath() instanceof Package packagePath)) {
            return targetName;
        }
        Set<String> searchWords = new LinkedHashSet<>(packagePath.pathSegments());
        searchWords.add(targetName);
        return String.join(" ", searchWords);
    }

    @RequiredReadAction
    private boolean processTargetReferences(
        StarlarkCallExpression target,
        VirtualFile candidate,
        PsiManager psiManager,
        Predicate<? super PsiReference> consumer
    ) {
        if (!(psiManager.findFile(candidate) instanceof StarlarkFile starlarkFile)) {
            return true;
        }
        for (StarlarkStringLiteralExpression literal :
            PsiTreeUtil.findChildrenOfType(starlarkFile, StarlarkStringLiteralExpression.class)) {
            if (literal.isTargetNameAttributeValue()) {
                continue;
            }
            if (!pointsToTarget(psiManager, literal, target)) {
                continue;
            }
            PsiReference reference = literal.getReference();
            if (reference == null) {
                continue;
            }
            if (!consumer.test(reference)) {
                return false;
            }
        }
        return true;
    }

    @RequiredReadAction
    private static boolean pointsToTarget(PsiManager receiver, StarlarkStringLiteralExpression candidate, StarlarkCallExpression target) {
        String targetName = target.getNameAttributeValue();
        if (targetName == null) {
            return false;
        }
        // quick exit to avoid reference resolution for unrelated strings
        if (!candidate.getStringContents().contains(targetName)) {
            return false;
        }
        PsiReference reference = candidate.getReference();
        PsiElement resolvedRef = reference == null ? null : reference.resolve();
        if (resolvedRef == null) {
            return false;
        }
        return receiver.areElementsEquivalent(resolvedRef, target);
    }
}
