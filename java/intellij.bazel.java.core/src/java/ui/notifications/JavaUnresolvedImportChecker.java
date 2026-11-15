package org.jetbrains.bazel.java.ui.notifications;

import com.intellij.java.language.psi.PsiImportList;
import com.intellij.java.language.psi.PsiImportStatementBase;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiJavaFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jetbrains.bazel.ui.notifications.BuildAndResyncOnUnresolvedImportNotificationsProvider.UnresolvedImportChecker;

import java.util.Arrays;
import java.util.Objects;

@ExtensionImpl
public class JavaUnresolvedImportChecker implements UnresolvedImportChecker {
    @Override
    @RequiredReadAction
    public boolean hasUnresolvedImport(Project project, PsiFile psiFile) {
        if (!(psiFile instanceof PsiJavaFile javaPsiFile)) {
            return false;
        }
        PsiImportList importList = javaPsiFile.getImportList();
        if (importList == null) {
            return false;
        }
        return Arrays.stream(importList.getAllImportStatements())
            .map(PsiImportStatementBase::getImportReference)
            .filter(Objects::nonNull)
            .anyMatch((PsiJavaCodeReferenceElement it) -> it.multiResolve(false).length == 0);
    }
}
