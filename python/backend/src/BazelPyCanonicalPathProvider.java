package com.intellij.bazel.python.backend;

import com.jetbrains.python.impl.psi.resolve.QualifiedNameFinder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.QualifiedName;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

// TODO consulo: not registered (upstream Pythonid.canonicalPathProvider). The Consulo PyCanonicalPathProvider is
//  getCanonicalPath(qName, foothold) without the symbol, and it is only asked for a symbol that already has an importable
//  qualified name; the upstream computation needs the file of the symbol, so the class does not implement the interface
public class BazelPyCanonicalPathProvider /* : PyCanonicalPathProvider */ {
    public @Nullable QualifiedName getCanonicalPath(@Nullable PsiElement symbol, QualifiedName qName, @Nullable PsiElement foothold) {
        PsiElement element = foothold != null ? foothold : symbol;
        if (element == null) {
            return null;
        }
        Project project = element.getProject();
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }

        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(symbol);
        if (virtualFile == null) {
            return null;
        }
        return project.getInstance(PythonResolveIndexService.class).findShortestQualifiedName(virtualFile);
    }

    // consulo: upstream internal top-level function of BazelPyCanonicalPathProvider.kt

    /**
     * Returns {@code true} if PyCharm is capable of generating an import for symbol
     */
    public static boolean isImportableByPyCharm(PsiElement symbol, @Nullable PsiElement foothold) {
        return QualifiedNameFinder.findCanonicalImportPath(symbol, foothold) != null;
    }
}
