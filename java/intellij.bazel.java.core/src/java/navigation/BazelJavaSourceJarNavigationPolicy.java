package org.jetbrains.bazel.java.navigation;

import com.intellij.java.language.impl.psi.impl.compiled.ClsClassImpl;
import com.intellij.java.language.impl.psi.impl.compiled.ClsCustomNavigationPolicy;
import com.intellij.java.language.impl.psi.impl.compiled.ClsFileImpl;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.language.psi.search.FilenameIndex;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bazel may produce the source jar without the strict layout - depending on the scenario, package root might not be under the jar root
 * or the package might not match the physical location at all.
 * Java plugin has a strict assumption on the source jar layout, so navigation does not work in this case.
 * Kotlin plugin is not that strict and navigation works by default.
 */
@ExtensionImpl
public class BazelJavaSourceJarNavigationPolicy implements ClsCustomNavigationPolicy {

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getNavigationElement(ClsFileImpl clsFile) {
        Project project = clsFile.getProject();
        if (DumbService.isDumb(project)) {
            return null;
        }
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }
        return findJavaSourceFile(clsFile);
    }

    private static final String JAVA_SOURCE_SUFFIX = ".java";

    @RequiredReadAction
    private static @Nullable PsiJavaFile findJavaSourceFile(ClsFileImpl receiver) {
        PsiClass[] classes = receiver.getClasses();
        if (classes.length == 0 || !(classes[0] instanceof ClsClassImpl clsClass)) {
            return null;
        }
        String sourceFileName = clsClass.getSourceFileName();
        if (!sourceFileName.endsWith(JAVA_SOURCE_SUFFIX)) {
            return null;
        }
        Project project = receiver.getProject();
        String packageName = receiver.getPackageName();
        // consulo: ProjectFileIndex.findContainingLibraries + LibraryEntity SOURCES roots (workspace model) ->
        //   library order entries of the file and their source roots
        VirtualFile[] sourceRoots = ProjectFileIndex
            .getInstance(project)
            .getOrderEntriesForFile(receiver.getVirtualFile())
            .stream()
            .filter(it -> it instanceof LibraryOrderEntry)
            .map(it -> ((LibraryOrderEntry) it).getLibrary())
            .filter(Objects::nonNull)
            .distinct()
            .flatMap((Library it) -> Arrays.stream(it.getFiles(SourcesOrderRootType.ID)))
            .distinct()
            .toArray(VirtualFile[]::new);
        if (sourceRoots.length == 0) {
            return null;
        }
        GlobalSearchScope sourceRootsScope = GlobalSearchScopesCore.directoriesScope(project, true, sourceRoots);
        for (VirtualFile it : FilenameIndex.getVirtualFilesByName(project, sourceFileName, sourceRootsScope)) {
            if (!it.isValid()) {
                continue;
            }
            PsiFile file = receiver.getManager().findFile(it);
            PsiJavaFile psi = file instanceof PsiJavaFile javaFile ? javaFile : null;
            if (psi == null || !Objects.equals(psi.getPackageName(), packageName)) {
                continue;
            }
            return psi;
        }
        return null;
    }
}
