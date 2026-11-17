package com.intellij.bazel.python.backend;

import com.jetbrains.python.PyNames;
import com.jetbrains.python.impl.psi.impl.PyElementPresentation;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyElement;
import com.jetbrains.python.psi.PyPossibleClassMember;
import consulo.annotation.access.RequiredReadAction;
import consulo.bazel.python.localize.BazelPythonBackendLocalize;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.QualifiedName;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// TODO consulo: not registered (upstream moduleRendererFactory, order="before platform"). Consulo has no ModuleRendererFactory
//  extension point (the right part of PsiElementListCellRenderer is always PsiElementModuleRenderer) and no TextWithIcon, so
//  the class does not extend it; the location string computation is kept
public class BazelPythonModuleRendererFactory /* : ModuleRendererFactory() */ {
    @RequiredReadAction
    public boolean handles(@Nullable Object element) {
        return element instanceof PyElement pyElement && org.jetbrains.bazel.config.project.isBazelProject(pyElement.getProject());
    }

    public boolean rendersLocationString() {
        return true;
    }

    // consulo: upstream returns TextWithIcon(locationString, null)
    @RequiredReadAction
    public @Nullable String getModuleTextWithIcon(@Nullable Object element) {
        return element instanceof PyElement pyElement ? bazelPythonLocationString(pyElement) : null;
    }

    // consulo: upstream top-level extensions of BazelPythonModuleRendererFactory.kt

    @RequiredReadAction
    public static @Nullable String bazelPythonLocationString(PyElement receiver) {
        PsiFile containingFile = receiver.getContainingFile();
        if (containingFile == null) {
            return null;
        }
        String packageForFile = getBazelPackageForFile(receiver, containingFile);
        if (packageForFile == null) {
            return null;
        }
        // TODO consulo: consulo-python has no stub files support (PyiFile is missing)
        // val isPyiFile = containingFile is PyiFile
        boolean isPyiFile = false;

        PyClass containingClass = receiver instanceof PyPossibleClassMember member ? member.getContainingClass() : null;
        // consulo: the PyPsiBundle element.presentation.location.string.* messages are missing in consulo-python, they are in
        // BazelPythonBackendLocalize with the same keys
        if (containingClass != null) {
            if (isPyiFile) {
                return BazelPythonBackendLocalize.elementPresentationLocationStringInClassStub(String.valueOf(containingClass.getName()), packageForFile).get();
            }
            else {
                return BazelPythonBackendLocalize.elementPresentationLocationStringInClass(String.valueOf(containingClass.getName()), packageForFile).get();
            }
        }

        if (isPyiFile) {
            return BazelPythonBackendLocalize.elementPresentationLocationStringModuleStub(packageForFile).get();
        }
        else {
            return BazelPythonBackendLocalize.elementPresentationLocationStringModule(packageForFile).get();
        }
    }

    private static @Nullable String getBazelPackageForFile(PyElement receiver, PsiFile containingFile) {
        String nativePackage = PyElementPresentation.getPackageForFile(containingFile);
        if (nativePackage != null) {
            return nativePackage;
        }

        VirtualFile vFile = containingFile.getVirtualFile();
        if (vFile == null) {
            return null;
        }
        String indexedPackage = findIndexedPackageForFile(receiver, vFile);
        if (indexedPackage != null) {
            return indexedPackage;
        }

        return getProjectRootQualifiedName(receiver, vFile);
    }

    private static @Nullable String findIndexedPackageForFile(PyElement receiver, VirtualFile vFile) {
        Path path = Path.of(vFile.getPath());
        QualifiedName qualifiedName = receiver.getProject().getInstance(PythonResolveIndexService.class).findShortestQualifiedName(path);
        return qualifiedName != null ? qualifiedName.toString() : null;
    }

    private static @Nullable String getProjectRootQualifiedName(PyElement receiver, VirtualFile vFile) {
        VirtualFile root = org.jetbrains.bazel.config.project.getRootDir(receiver.getProject());
        String relativePath = VirtualFileUtil.getRelativePath(vFile, root, VirtualFileUtil.VFS_SEPARATOR_CHAR);
        return relativePath != null ? toQualifiedName(relativePath, vFile.isDirectory()) : null;
    }

    private static @Nullable String toQualifiedName(String receiver, boolean isDirectory) {
        List<String> components = new ArrayList<>(StringUtil.split(receiver, String.valueOf(VirtualFileUtil.VFS_SEPARATOR_CHAR)));
        if (components.isEmpty()) {
            return null;
        }

        int lastIndex = components.size() - 1;
        String nameWithoutExtension = FileUtil.getNameWithoutExtension(components.get(lastIndex));
        if (!isDirectory && nameWithoutExtension.equals(PyNames.INIT)) {
            components.remove(lastIndex);
        }
        else {
            components.set(lastIndex, nameWithoutExtension);
        }

        if (components.isEmpty() || components.stream().anyMatch(it -> it.contains(".") || !PyNames.isIdentifier(it))) {
            return null;
        }
        // TODO consulo: convertStubToRuntimePackageName (stub packages, `-stubs` suffix) is missing in consulo-python
        // return convertStubToRuntimePackageName(QualifiedName.fromComponents(components)).toString()
        return QualifiedName.fromComponents(components).toString();
    }
}
