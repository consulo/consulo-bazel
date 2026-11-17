package com.intellij.bazel.python.backend;

import com.jetbrains.python.psi.impl.PyImportResolver;
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContext;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.QualifiedName;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@ExtensionImpl
public class BazelPyImportResolver implements PyImportResolver {
    @Override
    public @Nullable PsiElement resolveImportReference(
        QualifiedName name,
        PyQualifiedNameResolveContext context,
        boolean withRoots
    ) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(context.getProject())) {
            return null;
        }

        PsiElement rootImportResolve = resolveRootImport(name, context);
        if (rootImportResolve != null) {
            return rootImportResolve;
        }

        PsiElement synonymImport = null;
        for (PythonSynonymProvider it : PythonSynonymProvider.getEp()) {
            List<String> synonym = it.getSynonym(name.getComponents());
            PsiElement resolved = synonym != null ? resolveRootImport(QualifiedName.fromComponents(synonym), context) : null;
            if (resolved != null) {
                synonymImport = resolved;
                break;
            }
        }
        if (synonymImport != null) {
            return synonymImport;
        }

        return resolveShortImport(name, context);
    }

    /**
     * Root import is an absolute import, whose qualified name aligns with the project file structure.
     * For example {@code import aaa.bbb.ccc} that references {@code aaa/bbb/ccc.py} file or {@code aaa/bbb/ccc} directory.
     */
    private @Nullable PsiElement resolveRootImport(QualifiedName name, PyQualifiedNameResolveContext context) {
        // VirtualFile.findFileByRelativePath always expects a slash ("/"), even on Windows
        String relativeToRoot = String.join("/", name.getComponents());
        return ReadAction.compute(() -> findDirectoryOrPythonFile(relativeToRoot, context));
    }

    /**
     * In rare cases, both a directory and a file match the import.
     * <p>
     * Precedence, as per <a href="https://peps.python.org/pep-0420/#specification">PEP-420</a>:
     * <p>
     * "While looking for a module or package named “foo”, for each directory in the parent path:
     * <ol>
     * <li>If {@code <directory>/foo/__init__.py} is found, a regular package is imported and returned.</li>
     * <li>If not, but {@code <directory>/foo.{py,pyc,so,pyd}} is found, a module is imported and returned (...)"</li>
     * <li>If not, but {@code <directory>/foo} is found and is a directory,
     * it is recorded and the scan continues with the next directory in the parent path.</li>
     * <li>Otherwise the scan continues with the next directory in the parent path.</li>
     * </ol>
     * If the scan completes without returning a module or package,
     * and at least one directory was recorded, then a namespace package is created."
     * <p>
     * The paragraph above mentions looking "for each directory",
     * but this function is only responsible for looking in one (the project root).
     */
    private @Nullable PsiElement findDirectoryOrPythonFile(String pathRelativeToRoot, PyQualifiedNameResolveContext context) {
        VirtualFile rootDir = org.jetbrains.bazel.config.project.getRootDir(context.getProject());
        PsiManager psiManager = context.getPsiManager();
        VirtualFile directory = directoryOrNull(rootDir.findFileByRelativePath(pathRelativeToRoot));

        // case 1
        if (directory != null && directory.findChild("__init__.py") != null) {
            return psiManager.findDirectory(directory); // case 1
        }

        // case 2
        VirtualFile file = fileOrNull(rootDir.findFileByRelativePath(pathRelativeToRoot + ".py"));
        if (file != null) {
            return psiManager.findFile(file);
        }

        // case 3
        // TODO (BAZEL-1998) - verify if simply returning the namespace package directory is a good approach
        if (directory != null) {
            return psiManager.findDirectory(directory);
        }

        // case 4
        return null;
    }

    private static @Nullable PsiElement findFileOrDirectory(PsiManager receiver, VirtualFile vf) {
        if (vf.isDirectory()) {
            return receiver.findDirectory(vf);
        }
        else {
            return receiver.findFile(vf);
        }
    }

    private @Nullable PsiElement resolveShortImport(QualifiedName name, PyQualifiedNameResolveContext context) {
        Map<QualifiedName, Path> sourcesIndex = context.getProject().getInstance(PythonResolveIndexService.class).getResolveIndex();
        Path path = sourcesIndex.get(name);
        if (path == null) {
            return null;
        }
        VirtualFile vFile = VirtualFileManager.getInstance().findFileByNioPath(path);
        if (vFile == null) {
            return null;
        }
        return findFileOrDirectory(context.getPsiManager(), vFile);
    }

    // consulo: upstream private top-level extensions of BazelPyImportResolver.kt

    private static @Nullable VirtualFile fileOrNull(@Nullable VirtualFile receiver) {
        return receiver != null && !receiver.isDirectory() ? receiver : null;
    }

    private static @Nullable VirtualFile directoryOrNull(@Nullable VirtualFile receiver) {
        return receiver != null && receiver.isDirectory() ? receiver : null;
    }
}
