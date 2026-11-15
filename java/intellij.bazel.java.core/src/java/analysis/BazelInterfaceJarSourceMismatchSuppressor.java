package org.jetbrains.bazel.java.analysis;

import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;

/**
 * The mismatch between interface jar and source jar is expected for Bazel projects, so we suppress it.
 */
// TODO consulo: LibrarySourcesMismatchSuppressor (librarySourcesMismatchSuppressor) is missing in consulo-java, the class is not registered
public class BazelInterfaceJarSourceMismatchSuppressor /* : LibrarySourcesMismatchSuppressor */ {
    @RequiredReadAction
    public boolean shouldSuppress(PsiClass sourceClass) {
        PsiFile containingFile = sourceClass.getOriginalElement().getContainingFile();
        VirtualFile file = containingFile != null ? containingFile.getVirtualFile() : null;
        if (file == null) {
            return false;
        }
        return InterfaceJars.isBazelInterfaceJar(file);
    }
}
