package com.intellij.bazel.python.backend;

import com.jetbrains.python.impl.codeInsight.imports.AutoImportQuickFix;
import com.jetbrains.python.impl.codeInsight.imports.ImportFromExistingAction;
import com.jetbrains.python.impl.codeInsight.imports.PyImportCandidateProvider;
import com.jetbrains.python.impl.psi.PyUtil;
import com.jetbrains.python.impl.psi.search.PyProjectScopeBuilder;
import com.jetbrains.python.impl.psi.stubs.PyClassNameIndex;
import com.jetbrains.python.impl.psi.stubs.PyFunctionNameIndex;
import com.jetbrains.python.impl.psi.stubs.PyVariableNameIndex;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.QualifiedName;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Offers Python import quickfix suggestions for Bazel dependencies
 */
@ExtensionImpl
public class BazelPyImportCandidateProvider implements PyImportCandidateProvider {
    @Override
    @RequiredReadAction
    public void addImportCandidates(PsiReference reference, String name, AutoImportQuickFix quickFix) {
        PsiElement node = reference.getElement();
        Project project = node.getProject();
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return;
        }

        PythonResolveIndexService indexService = project.getInstance(PythonResolveIndexService.class);
        // consulo: IntelliJ PySearchUtilBase.defaultSuggestionScope(node) -> PyProjectScopeBuilder.excludeSdkTestsScope(node), the
        // scope that the Consulo import quick fix uses for its own suggestions
        GlobalSearchScope defaultSuggestionScope = PyProjectScopeBuilder.excludeSdkTestsScope(node);

        // expand the scope to contain third-party dependencies
        GlobalSearchScope scope = defaultSuggestionScope.uniteWith(indexService.getStubScope());

        List<PsiNamedElement> symbols = new ArrayList<>();
        symbols.addAll(PyClassNameIndex.find(name, project, scope));
        symbols.addAll(PyFunctionNameIndex.find(name, project, scope));
        symbols.addAll(PyVariableNameIndex.find(name, project, scope));

        Set<QualifiedName> seen = new HashSet<>();
        for (PsiNamedElement symbol : symbols) {
            if (!PyUtil.isTopLevel(symbol)) {
                continue;
            }
            PsiFile containingFile = symbol.getContainingFile();
            if (containingFile == null) {
                continue;
            }
            if (Objects.equals(containingFile, node.getContainingFile())) {
                continue;
            }
            VirtualFile virtualFile = containingFile.getVirtualFile();
            if (virtualFile == null) {
                continue;
            }

            // do not contribute an import if PyCharm will do it for us
            if (defaultSuggestionScope.contains(virtualFile) && BazelPyCanonicalPathProvider.isImportableByPyCharm(symbol, node)) {
                continue;
            }
            QualifiedName moduleQName = indexService.findShortestQualifiedName(virtualFile);
            if (moduleQName == null) {
                continue;
            }
            if (!seen.add(moduleQName.append(name))) {
                continue;
            }

            // If it's a single-file source root, the candidate file must be its parent.
            // Otherwise, the generated import will be just "import symbol" without its qualified path
            // consulo: IntelliJ PyUtil.isRoot -> ImportFromExistingAction.isRoot (the same check in Consulo)
            PsiFileSystemItem candidateFile;
            if (ImportFromExistingAction.isRoot(containingFile)) {
                PsiDirectory parent = containingFile.getParent();
                candidateFile = parent != null ? parent : containingFile;
            }
            else {
                candidateFile = containingFile;
            }

            quickFix.addImport(symbol, candidateFile, moduleQName);
        }
    }
}
